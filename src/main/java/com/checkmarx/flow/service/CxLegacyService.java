package com.checkmarx.flow.service;

import checkmarx.wsdl.portal.*;
import com.checkmarx.flow.config.CxProperties;
import com.checkmarx.flow.exception.CheckmarxLegacyException;
import com.checkmarx.flow.exception.MachinaException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import java.beans.ConstructorProperties;

/**
 * Checkmarx SOAP WebService Client
 */
@Component
public class CxLegacyService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CxLegacyService.class);
    private final CxProperties properties;
    private final WebServiceTemplate ws;

    private static final String CX_WS_PREFIX= "http://Checkmarx.com/";
    private static final String CX_WS_LOGIN_URI = CX_WS_PREFIX + "LoginV2";
    private static final String CX_WS_DESCRIPTION_URI = CX_WS_PREFIX + "GetResultDescription";
    private static final String CX_WS_TEAM_URI = CX_WS_PREFIX + "CreateNewTeam";

    @ConstructorProperties({"properties", "ws"})
    public CxLegacyService(CxProperties properties, WebServiceTemplate ws) {
        this.properties = properties;
        this.ws = ws;
    }

    /**
     * Login to Cx using legacy SOAP WS
     * @param username
     * @param password
     * @return
     * @throws CheckmarxLegacyException
     */
    public String login(String username, String password) throws CheckmarxLegacyException {
        LoginV2 request = new LoginV2();
        request.setApplicationCredentials(new Credentials(username, password));
        LoginV2Response response = (LoginV2Response) ws.marshalSendAndReceive(ws.getDefaultUri(), request, new SoapActionCallback(CX_WS_LOGIN_URI));
        try {
            if(!response.getLoginV2Result().isIsSuccesfull())
                throw new CheckmarxLegacyException("Authentication Error");
            return response.getLoginV2Result().getSessionId();
        }
        catch(NullPointerException e){
            log.error("Authentication Error while logging into CX using SOAP WS");
            throw new CheckmarxLegacyException("Authentication Error");
        }
    }

    void createTeam(String sessionId, String parentId, String teamName) throws MachinaException {
        CreateNewTeam request = new CreateNewTeam(sessionId);
        request.setNewTeamName(teamName);
        request.setParentTeamID(parentId);
        log.info("Creating team {} ({})", teamName, parentId);

        try {
            CreateNewTeamResponse response = (CreateNewTeamResponse) ws.marshalSendAndReceive(ws.getDefaultUri(), request, new SoapActionCallback(CX_WS_TEAM_URI));
            if(!response.getCreateNewTeamResult().isIsSuccesfull()){
                log.error("Error occurred while creating Team {} with parentId {}", teamName, parentId);
                throw new MachinaException("Error occurred during team creation");
            }
        }catch(NullPointerException e){
            log.error("Error occurred while creating Team {} with parentId {}", teamName, parentId);
            throw new MachinaException("Error occurred during team creation");
        }
    }

    String getDescription(String session, Long scanId, Long pathId){
        GetResultDescription request = new GetResultDescription(session);
        request.setPathID(pathId);
        request.setScanID(scanId);

        log.debug("Retrieving description for {} / {} ", scanId, pathId);

        GetResultDescriptionResponse response = (GetResultDescriptionResponse)
                ws.marshalSendAndReceive(ws.getDefaultUri(), request, new SoapActionCallback(CX_WS_DESCRIPTION_URI));
        try{
            if(!response.getGetResultDescriptionResult().isIsSuccesfull()){
                log.error(response.getGetResultDescriptionResult().getErrorMessage());
                return "";
            }
            else {
                String description = response.getGetResultDescriptionResult().getResultDescription();
                description = description.replace(properties.getHtmlStrip(), "");
                description = description.replaceAll("\\<.*?>", ""); /*Strip tag elements*/
                return description;
            }
        }catch (NullPointerException e){
            log.warn("Error occurred getting description for {} / {}", scanId, pathId);
            return "";
        }
    }
}
