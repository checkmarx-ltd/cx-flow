package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.CxProfile;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HelperService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HelperService.class);
    private final FlowProperties properties;
    private final CxProperties cxProperties;
    private final ExternalScriptService scriptService;
    private List<CxProfile> profiles;

    public HelperService(FlowProperties properties, CxProperties cxProperties, ExternalScriptService scriptService) {
        this.properties = properties;
        this.cxProperties = cxProperties;
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
                log.warn("No CxProfile found - {}", properties.getProfileConfig());
            }
        }
    }

    public boolean isBranch2Scan(ScanRequest request, List<String> branches){
        String scriptFile = properties.getBranchScript();
        String branch = request.getBranch();
        String targetBranch = request.getMergeTargetBranch();
        if(!ScanUtils.empty(targetBranch)){ //if targetBranch is set, it is a merge request
            branch = targetBranch;
        }
        //note:  if script is provided, it is highest priority
        if(!ScanUtils.empty(scriptFile)){
            log.info("executing external script to determine if branch should be scanned ({})", scriptFile);
            try {
                String script = getStringFromFile(scriptFile);
                HashMap<String, Object> bindings = new HashMap<>();
                bindings.put("request", request);
                bindings.put("branches", branches);
                Object result = scriptService.runScript(script, bindings);
                if (result instanceof Boolean) {
                    return ((boolean) result);
                }
            }catch (IOException e){
                log.error("Error reading script file {}", scriptFile);
                log.error(ExceptionUtils.getMessage(e));
            }
        }
        /*Override branches if provided in the request*/
        if(request.getActiveBranches() != null && !request.getActiveBranches().isEmpty()){
            branches = request.getActiveBranches();
        }
        //If the script fails above, default to base property check functionality (regex list)
        for( String b: branches){
            if(strMatches(b, branch)) return true;
        }
        log.info("Branch {} did not meet the scanning criteria [{}]", branch, branches);
        return false;
    }

    public String getCxTeam(ScanRequest request){
        String scriptFile = cxProperties.getTeamScript();
        String team = request.getTeam();
        //note:  if script is provided, it is highest priority
        if(!ScanUtils.empty(scriptFile)){
            log.info("executing external script to determine the Team in Checkmarx to be used ({})", scriptFile);
            try {
                String script = getStringFromFile(scriptFile);
                HashMap<String, Object> bindings = new HashMap<>();
                bindings.put("request", request);
                Object result = scriptService.runScript(script, bindings);
                if (result instanceof String) {
                    return ((String) result);
                }
            }catch (IOException e){
                log.error("Error reading script file for checkmarx team {}", scriptFile);
                log.error(ExceptionUtils.getMessage(e));
            }
        }
        else if(!ScanUtils.empty(team)){
            return team;
        }
        return null;  //null will indicate no override of team will take place
    }

    public String getCxProject(ScanRequest request){
        String scriptFile = cxProperties.getProjectScript();
        String project = request.getProject();
        //note:  if script is provided, it is highest priority
        if(!ScanUtils.empty(scriptFile)){
            log.info("executing external script to determine the Project in Checkmarx to be used ({})", scriptFile);
            try {
                String script = getStringFromFile(scriptFile);
                HashMap<String, Object> bindings = new HashMap<>();
                bindings.put("request", request);
                Object result = scriptService.runScript(script, bindings);
                if (result instanceof String) {
                    return ((String) result);
                }
            }catch (IOException e){
                log.error("Error reading script file for checkmarx project {}", scriptFile);
                log.error(ExceptionUtils.getMessage(e));
            }
        }
        else if(!ScanUtils.empty(project)){
            return project;
        }
        return null;  //null will indicate no override of team will take place
    }

    public String getShortUid(ScanRequest request){
        String uid = RandomStringUtils.random(Constants.SHORT_ID_LENGTH, true, true) ;
        request.setId(uid);
        return uid;
    }

    public String getShortUid(){
        return RandomStringUtils.random(Constants.SHORT_ID_LENGTH, true, true) ;
    }

    private String getStringFromFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path.intern())));
    }

    /**
     * Determine what preset to use based on Sources and Profile mappings
     * @param sources
     * @return
     */
    public String getPresetFromSources(Sources sources){
        if(sources == null || profiles == null || sources.getLanguageStats() == null || sources.getSources() == null){
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
     * @param sources
     * @param regex
     * @return
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
     * @param sources
     * @param patternStr
     * @return
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
     * @param patternStr
     * @param str
     * @return
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
}

