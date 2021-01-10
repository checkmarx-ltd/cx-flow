package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.CxProfile;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxPropertiesBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HelperService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HelperService.class);
    private final FlowProperties properties;
    private final CxPropertiesBase cxProperties;
    private final JiraProperties jiraProperties;
    private final ExternalScriptService scriptService;
    private List<CxProfile> profiles;

    public HelperService(FlowProperties properties, CxScannerService cxScannerService,
                         JiraProperties jiraProperties,
                         ExternalScriptService scriptService) {
        this.properties = properties;
        this.cxProperties = cxScannerService.getProperties();
        this.jiraProperties = jiraProperties;
        this.scriptService = scriptService;
    }

    @PostConstruct
    public void loadCxProfiles() {
        if(properties.isAutoProfile() && !ScanUtils.empty(properties.getProfileConfig())) {
            log.debug("Loading CxProfile: {}", properties.getProfileConfig());
            File profileConfig = new File(properties.getProfileConfig());
            ObjectMapper mapper = new ObjectMapper();
            //if override is provided, check if chars are more than 20 in length, implying base64 encoded json
            try {
                CxProfile[] cxProfiles = mapper.readValue(profileConfig, CxProfile[].class);
                this.profiles = Arrays.asList(cxProfiles);
            }catch (IOException e){
                log.warn("No CxProfile found - {}", e.getMessage());
            }
        }
    }

    public boolean isBranch2Scan(ScanRequest request, List<String> branches) {
        String branchToCheck = getBranchToCheck(request);

        // If script is provided, it is highest priority
        String scriptFile = properties.getBranchScript();
        if (!ScanUtils.empty(scriptFile)) {
            Object branchShouldBeScanned = scriptService.executeBranchScript(scriptFile, request, branches);
            if (branchShouldBeScanned instanceof Boolean) {
                return ((boolean) branchShouldBeScanned);
            }
        }

        // Override branches if provided in the request
        if (CollectionUtils.isNotEmpty(request.getActiveBranches())) {
            branches = request.getActiveBranches();
        }

        // If the script fails above, default to base property check functionality (regex list)
        if (isBranchProtected(branchToCheck, branches, request)) {
            return true;
        }

        log.info("Branch {} did not meet the scanning criteria [{}]", branchToCheck, branches);
        return false;
    }

    public boolean isBranchProtected(String branchToCheck, List<String> protectedBranchPatterns, ScanRequest request) {
        boolean result;
        if (protectedBranchPatterns.isEmpty() && branchToCheck.equalsIgnoreCase(request.getDefaultBranch())) {
            result = true;
            log.info("Scanning default branch - {}", request.getDefaultBranch());
        } else {
            result = protectedBranchPatterns.stream().anyMatch(aBranch -> strMatches(aBranch, branchToCheck));
        }
        return result;
    }

    private static String getBranchToCheck(ScanRequest request) {
        String result = request.getBranch();
        String targetBranch = request.getMergeTargetBranch();
        if (StringUtils.isNotEmpty(targetBranch)) { //if targetBranch is set, it is a merge request
            result = targetBranch;
        }
        return result;
    }

    public String getCxTeam(ScanRequest request) {
        String scriptFile = cxProperties.getTeamScript();
        String team = request.getTeam();
        return getEffectiveEntityName(request, scriptFile, team, "team");
    }

    public String getCxProject(ScanRequest request) {
        String scriptFile = cxProperties.getProjectScript();
        String project = request.getProject();
        return getEffectiveEntityName(request, scriptFile, project, "project");
    }

    public String getJiraProjectKey(ScanRequest request) {
        String scriptFile = jiraProperties.getProjectKeyScript();
        String jiraProject = request.getBugTracker().getProjectKey();
        return getEffectiveEntityName(request, scriptFile, jiraProject, "jira project");
    }

    public String getCxComment(ScanRequest request, String defaultValue){
        String scriptFile = properties.getCommentScript();
        return getEffectiveEntityName(request, scriptFile, defaultValue,"comment");
    }

    private String getEffectiveEntityName(ScanRequest request, String scriptFile, String defaultName, String entity) {
        String result = null;
        //note:  if script is provided, it is highest priority
        if (!ScanUtils.empty(scriptFile)) {
            result = Optional.ofNullable(scriptService.getScriptExecutionResult(request, scriptFile, entity)).orElse(defaultName) ;
        } else if (!ScanUtils.empty(defaultName)) {
            result = defaultName;
        }
        return result;  //null will indicate no override will take place
    }

    public String getShortUid(ScanRequest request){
        String uid = RandomStringUtils.random(Constants.SHORT_ID_LENGTH, true, true) ;
        request.setId(uid);
        return uid;
    }

    public String getShortUid(){
        return RandomStringUtils.random(Constants.SHORT_ID_LENGTH, true, true) ;
    }

    /**
     * Determine what preset to use based on Sources and Profile mappings
     */
    public String getPresetFromSources(Sources sources){
        if(sources == null || profiles == null || sources.getSources() == null){
            return cxProperties.getScanPreset();
        }

        for(CxProfile p: profiles){
            log.debug(p.toString());
            if(p.getName().equalsIgnoreCase("default")){ //This should be the last profile
                log.info("Using default preset {}", p.getPreset());
                return p.getPreset();
            }
            if(checkProfile(p, sources)){
                log.info("Using preset {} based on profile {}", p.getPreset(), p.getName());
                return p.getPreset();
            }
        }
        return null;
    }

    private boolean checkProfile(CxProfile profile, Sources sources){
        log.debug("Evaluating profile {}", profile.getName());
        //check for matching files
        if(!checkFileRegex(sources.getSources(), profile.getFiles())){
            return false;
        }
        if(profile.getWeight() == null || profile.getWeight().isEmpty()){
            return true;
        }
        if(sources.getLanguageStats() != null && !sources.getLanguageStats().isEmpty()) {
            for(CxProfile.Weight p: profile.getWeight()){
                if(!checkSourceWeight(sources.getLanguageStats(), p)){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkSourceWeight(Map<String, Integer> languages, CxProfile.Weight weight){
        if(weight == null){
            return true;
        }
        else if(languages == null || languages.isEmpty() ){
            return false;
        }
        boolean match = false;
        for (Map.Entry<String, Integer> langs : languages.entrySet()) {
            if(langs.getKey().equalsIgnoreCase(weight.getType())){
                if(langs.getValue() < weight.getWeight()){
                    return false;
                }
                else{
                    match = true;
                }
            }
        }
        return match;
    }

    /**
     * Go through each possible pattern and determine if a match exists within the Sources list
     */
    private boolean checkFileRegex(List<Sources.Source> sources, List<String> regex){
        if(sources == null || sources.isEmpty() || regex == null || regex.isEmpty()){
            return true;
        }
        for(String r: regex){
            if(!strListMatches(sources, r)){
                return false;
            }
        }
        return true;
    }

    /**
     * Go through list of Sources (file names/paths) and determine if a match exists with a pattern
     */
    private boolean strListMatches(List<Sources.Source> sources, String patternStr){
        for(Sources.Source s: sources) {
            if (strMatches(patternStr, s.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Regex String match
     */
    private boolean strMatches(String patternStr, String str){
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(str);
        if(matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            return start == 0 && end == str.length();
        }
        return false;
    }

    public List<CxProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<CxProfile> profiles) {
        this.profiles = profiles;
    }

}

