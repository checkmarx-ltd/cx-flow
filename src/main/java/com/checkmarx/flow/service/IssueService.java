package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.custom.IssueTracker;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.Issue;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaException;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.dto.ScanResults;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

/**
 *  Issue manipulation logic for issue trackers of type {@link BugTracker.Type#CUSTOM}.
 */
@Service
public class IssueService implements ApplicationContextAware {

    private ApplicationContext context;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IssueService.class);
    private final FlowProperties properties;
    private final CodeBashingService codeBashingService;

    public ApplicationContext getContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext context){
        this.context = context;
    }

    public IssueService(FlowProperties properties, CodeBashingService codeBashingService) {
        this.properties = properties;
        this.codeBashingService = codeBashingService;
    }

    /**
     * Create a map of custom issues
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
     */
    private Map<String, ScanResults.XIssue> getXIssueMap(IssueTracker tracker, ScanResults results, ScanRequest request) {
        List<ScanResults.XIssue> issues = new ArrayList<>();

        Optional.ofNullable(results.getScaResults()).ifPresent( s -> {
            List<ScanResults.XIssue> scaIssues = ScanUtils.scaToXIssues(s);
            issues.addAll(scaIssues);
        });

        Optional.ofNullable(results.getAstResults()).ifPresent( s -> {
            List<ScanResults.XIssue> astIssues = ScanUtils.setASTXIssuesInScanResults(results);
            issues.addAll(astIssues);
        });

        Optional.ofNullable(results.getXIssues()).ifPresent(i ->
                issues.addAll(results.getXIssues())
        );

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
        BugTracker bugTracker = request.getBugTracker();
        String customBean = bugTracker.getCustomBean();
        if (!bugTracker.getType().equals(BugTracker.Type.CUSTOM) && !ScanUtils.empty(customBean)) {
            throw new MachinaException("A valid custom bean must be used here.");
        }

        try {
            IssueTracker tracker = (IssueTracker) context.getBean(customBean);
            tracker.init(request, results);
            String fpLabel = tracker.getFalsePositiveLabel();

            codeBashingService.createLessonsMap();

            log.info("Processing Issues with custom bean {}", customBean);

            List<Issue> issues = tracker.getIssues(request);
            if(issues == null){
                issues = Collections.emptyList();
            }
            xMap = this.getXIssueMap(tracker, results, request);
            iMap = this.getIssueMap(tracker, issues, request);

            for (Map.Entry<String, ScanResults.XIssue> xIssue : xMap.entrySet()) {
                try {
                    String fileUrl;
                    ScanResults.XIssue currentIssue = xIssue.getValue();

                    codeBashingService.addCodebashingUrlToIssue(currentIssue);

                    /*Issue already exists -> update and comment*/
                    if (iMap.containsKey(xIssue.getKey())) {
                        Issue i = iMap.get(xIssue.getKey());
                        if(xIssue.getValue().isAllFalsePositive()) {
                            //All issues are false positive, so issue should be closed
                            Issue fpIssue;
                            log.debug("All issues are false positives");

                            if(properties.isListFalsePositives()) { //Update the ticket if flag is set
                                log.debug("Issue is being updated to reflect false positive references.  Updating issue with key {}", xIssue.getKey());
                                tracker.updateIssue(i, currentIssue, request);
                            }
                            if (tracker.isIssueOpened(i, request)) {
                                /*Close the issue if in an open state*/
                                log.info("Closing issue with key {}", i.getId());
                                tracker.closeIssue(i, request);
                                closedIssues.add(i.getId());
                            }

                        }
                        else if (!i.getLabels().contains(fpLabel)) { /*Ignore any with label indicating false positive*/
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
                        if(!xIssue.getValue().isAllFalsePositive()) {
                            fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                            xIssue.getValue().setGitUrl(fileUrl);
                            log.info("Creating new issue with key {}", xIssue.getKey());
                            Issue newIssue = tracker.createIssue(xIssue.getValue(), request);
                            if (newIssue != null) {
                                newIssues.add(newIssue.getId());
                                log.info("New issue created. #{}", newIssue.getId());
                            }
                        }
                    }
                } catch (HttpClientErrorException e) {
                    log.error("Error occurred while processing issue with key {}", xIssue.getKey(), e);
                }
            }

            /*Check if an issue exists in GitLab but not within results and close if not*/
            for (Map.Entry<String, Issue> issueMap : iMap.entrySet()) {
                String key = issueMap.getKey();
                Issue issue = issueMap.getValue();
                try {
                    if (!xMap.containsKey(key) && tracker.isIssueOpened(issue, request)) {
                        /*Close the issue*/
                        tracker.closeIssue(issue, request);
                        closedIssues.add(issue.getId());
                        log.info("Closing issue #{} with key {}", issue.getId(), key);
                    }
                } catch (HttpClientErrorException e) {
                    log.error("Error occurred while processing issue with key {}", key, e);
                }
            }

            Map<String, List<String>> issuesMap = new HashMap<>();
            issuesMap.put("new", newIssues);
            issuesMap.put("updated", updatedIssues);
            issuesMap.put("closed", closedIssues);

            tracker.complete(request, results);

            return issuesMap;
        } catch (BeansException e){
            log.error("Specified bug tracker bean was not found or properly loaded.", e);
            throw new MachinaRuntimeException();
        } catch (ClassCastException e){
            log.error("Bean must implement the IssueTracker Interface", e);
            throw new MachinaRuntimeException();
        }
    }
}
