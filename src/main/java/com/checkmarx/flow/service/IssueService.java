package com.checkmarx.flow.service;

import com.checkmarx.flow.custom.IssueTracker;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.ScanResults;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import java.util.*;

@Service
public class IssueService implements ApplicationContextAware {

    private ApplicationContext context;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IssueService.class);


    public ApplicationContext getContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext context){
        this.context = context;
    }


    /**
     * Create a map of custom issues
     *
     * @param tracker
     * @param issues
     * @param request
     * @return
     */
    private Map<String, Issue> getIssueMap(IssueTracker tracker, List<Issue> issues, ScanRequest request) {
        Map<String, Issue> issueMap = new HashMap<>();
        for (Issue issue : issues) {
            String key = tracker.getIssueKey(issue, request);
            issueMap.put(key, issue);
        }
        return issueMap;
    }

    /**
     * Create a map of Checkmarx Issues
     *
     * @param tracker
     * @param issues
     * @param request
     * @return
     */
    private Map<String, ScanResults.XIssue> getXIssueMap(IssueTracker tracker, List<ScanResults.XIssue> issues, ScanRequest request) {
        Map<String, ScanResults.XIssue> xMap = new HashMap<>();
        for (ScanResults.XIssue issue : issues) {
            String key = tracker.getXIssueKey(issue, request);
            xMap.put(key, issue);
        }
        return xMap;
    }

    Map<String, List<String>> process(ScanResults results, ScanRequest request) throws MachinaException {
        Map<String, ScanResults.XIssue> xMap;
        Map<String, Issue> iMap;
        List<String> newIssues = new ArrayList<>();
        List<String> updatedIssues = new ArrayList<>();
        List<String> closedIssues = new ArrayList<>();
        if (!request.getBugTracker().getType().equals(BugTracker.Type.CUSTOM) && !ScanUtils.empty(request.getBugTracker().getCustomBean())) {
            throw new MachinaException("A valid custom bean must be used here.");
        }

        try {
            IssueTracker tracker = (IssueTracker) context.getBean(request.getBugTracker().getCustomBean());
            tracker.init(request, results);
            String fpLabel = tracker.getFalsePositiveLabel();

            log.info("Processing Issues with custom bean {}", request.getBugTracker().getCustomBean());

            List<Issue> issues = tracker.getIssues(request);
            if(issues == null){
                issues = Collections.emptyList();
            }
            xMap = this.getXIssueMap(tracker, results.getXIssues(), request);
            iMap = this.getIssueMap(tracker, issues, request);
            if(iMap == null){
                iMap = Collections.emptyMap();
            }
            if(xMap == null){
                xMap = Collections.emptyMap();
            }
            for (Map.Entry<String, ScanResults.XIssue> xIssue : xMap.entrySet()) {
                try {
                    String fileUrl;
                    ScanResults.XIssue currentIssue = xIssue.getValue();

                    /*Issue already exists -> update and comment*/
                    if (iMap.containsKey(xIssue.getKey())) {
                        Issue i = iMap.get(xIssue.getKey());

                        /*Ignore any with label indicating false positive*/
                        if (!i.getLabels().contains(fpLabel)) {
                            log.info("Issue still exists.  Updating issue with key {}", xIssue.getKey());
                            fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                            currentIssue.setGitUrl(fileUrl);
                            Issue updatedIssue = tracker.updateIssue(i, currentIssue, request);
                            if (updatedIssue != null) {
                                updatedIssues.add(updatedIssue.getId());
                                log.debug("Update completed for issue #{}", updatedIssue.getId());
                            }
                        } else {
                            log.info("Skipping issue marked as false positive with key {}", xIssue.getKey());
                        }
                    } else {
                        /*Create the new issue*/
                        fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                        xIssue.getValue().setGitUrl(fileUrl);
                        log.info("Creating new issue with key {}", xIssue.getKey());
                        Issue newIssue = tracker.createIssue(xIssue.getValue(), request);
                        if(newIssue != null) {
                            newIssues.add(newIssue.getId());
                            log.info("New issue created. #{}", newIssue.getId());
                        }
                    }
                } catch (HttpClientErrorException e) {
                    log.error("Error occurred while processing issue with key {} {}", xIssue.getKey(), e);
                }
            }

            /*Check if an issue exists in GitLab but not within results and close if not*/
            for (Map.Entry<String, Issue> issue : iMap.entrySet()) {
                try {
                    if (!xMap.containsKey(issue.getKey())) {
                        if (tracker.isIssueOpened(issue.getValue())) {
                            /*Close the issue*/
                            tracker.closeIssue(issue.getValue(), request);
                            closedIssues.add(issue.getValue().getId());
                            log.info("Closing issue #{} with key {}", issue.getValue().getId(), issue.getKey());
                        }
                    }
                } catch (HttpClientErrorException e) {
                    log.error("Error occurred while processing issue with key {} {}", issue.getKey(), e);
                }
            }

            Map<String, List<String>> issuesMap = new HashMap<>();
            issuesMap.put("new", newIssues);
            issuesMap.put("updated", updatedIssues);
            issuesMap.put("closed", closedIssues);

            tracker.complete(request, results);

            return issuesMap;
        } catch (BeansException e){
            log.error("Specified bug tracker bean was not found or properly loaded.");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        } catch (ClassCastException e){
            log.error("Bean must implement the IssueTracker Interface");
            log.error(ExceptionUtils.getStackTrace(e));
            throw new MachinaRuntimeException();
        }
    }

}
