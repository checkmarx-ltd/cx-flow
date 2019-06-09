package com.checkmarx.flow.service;

import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.utils.Constants;
import com.checkmarx.flow.utils.ScanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HelperService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HelperService.class);
    private final FlowProperties properties;
    private final CxProperties cxProperties;
    private final ExternalScriptService scriptService;

    public HelperService(FlowProperties properties, CxProperties cxProperties, ExternalScriptService scriptService) {
        this.properties = properties;
        this.cxProperties = cxProperties;
        this.scriptService = scriptService;
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
        return new String(Files.readAllBytes(Paths.get(path)));
    }

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

