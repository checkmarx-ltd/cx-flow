package com.checkmarx.flow.dto.github;

import com.checkmarx.flow.config.*;
import com.checkmarx.flow.constants.FlowConstants;
import com.checkmarx.flow.controller.GitHubController;
import com.checkmarx.flow.controller.WebhookController;
import com.checkmarx.flow.dto.BugTracker;
import com.checkmarx.flow.dto.ControllerRequest;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.service.*;
import com.checkmarx.flow.utils.HTMLHelper;
import com.checkmarx.flow.utils.ScanUtils;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.filtering.FilterConfiguration;
import com.checkmarx.sdk.dto.sast.CxConfig;
import com.checkmarx.sdk.service.CxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class CxFlowCommandHandler extends WebhookController {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitHubController.class);
    @Autowired
    private FlowProperties flowProperties;
    @Autowired
    private JiraProperties jiraProperties;
    @Autowired
    private HelperService helperService;
    @Autowired
    private FlowService flowService;

    @Autowired
    private FilterFactory filterFactory;

    @Autowired
    private GitHubAppAuthService gitHubAppAuthService;

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private ScmConfigOverrider scmConfigOverrider;
    @Autowired
    private GitHubProperties properties;

    @Autowired
    private GitAuthUrlGenerator gitAuthUrlGenerator;

    @Autowired
    private ConfigurationOverrider configOverrider;

    private Mac hmac;

    @Autowired
    private CxService objCxservice;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeyException {
        // initialize HMAC with SHA1 algorithm and secret
        setHmacToken(properties.getWebhookToken());
    }

    private void setHmacToken(String webhookToken) throws NoSuchAlgorithmException, InvalidKeyException {
        if (properties != null && !ScanUtils.empty(webhookToken)) {
            SecretKeySpec secret = new SecretKeySpec(webhookToken.getBytes(CHARSET), HMAC_ALGORITHM);
            hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(secret);
        }
    }

    public String handleCxFlowCommand(GitHubProperties properties, String commentBody, int issueNumber, String repoFullName, String userName, CommentEvent event, String signature, String product, ControllerRequest controllerRequest, String body) throws Exception {
        if(commentBody.startsWith(">")) return "";
        String command = CxFlowCommandParser.parseCommand(commentBody);
        int scanID = 0;
        if (command.equalsIgnoreCase("cancel") || command.equalsIgnoreCase("status")) {
            Pattern pattern;
            if(command.equalsIgnoreCase("cancel")){
                pattern = Pattern.compile("@cxflow cancel (\\d+)");
            }else{
                pattern = Pattern.compile("@cxflow status (\\d+)");
            }
            Matcher matcher = pattern.matcher(commentBody);
            if (matcher.find()) {
                scanID = Integer.parseInt(matcher.group(1));
            }else{
                    postComment(repoFullName, issueNumber, "> "+commentBody+"\n\nPlease provide Scan ID.", properties);
                    return "NA";

            }
        }
        switch (command) {
            case "status":
                postComment(repoFullName, issueNumber, "> "+commentBody+"\n\n - Scan is in  : "+objCxservice.getScanStatusName(scanID)+" state.", properties);
                return "Scan status done.";
            case "hi":
                // Call internal rescan process

                postComment(repoFullName, issueNumber, "> @cxflow hi\n\n" + "\n Hi " + userName + "," + " \n How can CX-Flow help you? \n - Get the status of the current scan by posting the command: @CXFlow status scanID" +
                        "\n - Perform a new scan by posting the command: @CXFlow rescan \n - Cancel a running scan by posting the command: @CXFlow cancel scanID \n ", properties);
                return "OK";
            case "rescan":
                // Call internal rescan process

                postComment(repoFullName, issueNumber, "> "+commentBody+"\n\n"+"- Rescan initiated.", properties);
                return triggerRescanProcess(event, signature, product, controllerRequest, getPullRequest(repoFullName, issueNumber, properties), body);

            case "cancel":
                // Call internal rescan process

                objCxservice.cancelScan(scanID);
                postComment(repoFullName, issueNumber, "> "+commentBody+"\n\nScan cancelled with Scan ID : "+scanID, properties);
                return "Scan Deleted with Scan ID : "+scanID;
            default:
                return unsupportedCommandResponse(issueNumber, userName, properties, repoFullName);
        }
    }



//    private String triggerRescanProcess(int issueNumber, String repoFullName,GitHubProperties properties) {
//        // Logic for handling rescan
//
//        return "Rescan initiated for issue #" + issueNumber + " in repository " + repoFullName;
//    }

    private String unsupportedCommandResponse(int issueNumber, String userName, GitHubProperties properties, String repoFullName) {

        postComment(repoFullName, issueNumber, "I'm afraid I can't do that, " + userName + " .", properties);
        return "I'm afraid I can't do that, " + userName + " .";
    }

    public void postComment(String repoFullName, int issueNumber, String comment, GitHubProperties properties) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.valueOf(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.getToken());

        Map<String, String> body = new HashMap<>();
        body.put("body", comment);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        String url = "https://api.github.com/repos/" + repoFullName + "/issues/" + issueNumber + "/comments";
        restTemplate.postForEntity(url, request, String.class);
    }

    public static PullRequest getPullRequest(String repo, int pullNumber, GitHubProperties properties) throws Exception {
        String urlString = GITHUB_API_URL + repo + "/pulls/" + pullNumber;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "token " + properties.getToken());
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        StringBuilder response = new StringBuilder();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } else {
            System.out.println("GET request failed, response code: " + responseCode);
        }

        PullRequest pullRequest;
        ObjectMapper mapper = new ObjectMapper();
        try {
            pullRequest = mapper.readValue(response.toString(), PullRequest.class);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            return null;
        }

        return pullRequest;
    }


    public String triggerRescanProcess(
            CommentEvent event,
            String signature,
            String product,
            ControllerRequest controllerRequest, PullRequest pullRequest, String body
    ) {
        String uid = helperService.getShortUid();
        MDC.put(FlowConstants.MAIN_MDC_ENTRY, uid);
        log.info("Processing GitHub PULL request");

        Integer installationId = null;
        controllerRequest = ensureNotNull(controllerRequest);


        //gitHubService.initConfigProviderOnCommandEvent(uid, event);

        //verify message signature
        verifyHmacSignature(body, signature, controllerRequest);

        try {
            String action = event.getAction();
            // synchronize - happens when user pushes code into a branch for which a pull request exists
            if (!action.equalsIgnoreCase("created") &&
                    !action.equalsIgnoreCase("opened") &&
                    !action.equalsIgnoreCase("reopened") &&
                    !action.equalsIgnoreCase("synchronize")) {
                log.info("Pull requested not processed.  Status was not opened ({})", action);
                if (!flowProperties.isDeleteForkedProject()) {
                    return null;
                }

            }
            Repository repository = event.getRepository();
            String app = repository.getName();
            if (!ScanUtils.empty(controllerRequest.getApplication())) {
                app = controllerRequest.getApplication();
            }

            // By default, when a pull request is opened, use the current source control provider as a bug tracker
            // (GitHub in this case). Bug tracker from the config is not used, because we only want to notify the user
            // that their code has some issues. I.e. we don't want to open real issues in the "official" bug tracker yet.
            BugTracker.Type bugType = BugTracker.Type.GITHUBPULL;

            // However, if the bug tracker is overridden in the query string, use the override value.
            if (!ScanUtils.empty(controllerRequest.getBug())) {
                bugType = ScanUtils.getBugTypeEnum(controllerRequest.getBug(), flowProperties.getBugTrackerImpl());
            }

            if (controllerRequest.getAppOnly() != null) {
                flowProperties.setTrackApplicationOnly(controllerRequest.getAppOnly());
            }

            if (ScanUtils.empty(product)) {
                product = ScanRequest.Product.CX.getProduct();
            }
            ScanRequest.Product p = ScanRequest.Product.valueOf(product.toUpperCase(Locale.ROOT));

            String currentBranch = pullRequest.getHead().getRef();
            String targetBranch = pullRequest.getBase().getRef();
            List<String> branches = getBranches(controllerRequest, flowProperties);
            BugTracker bt = ScanUtils.getBugTracker(controllerRequest.getAssignee(), bugType, jiraProperties, controllerRequest.getBug());
            FilterConfiguration filter = filterFactory.getFilter(controllerRequest, flowProperties);
            Map<FindingSeverity, Integer> thresholdMap = getThresholds(controllerRequest);

            //build request object
            String gitUrl = Optional.ofNullable(pullRequest.getHead().getRepo())
                    .map(Repo::getCloneUrl)
                    .orElse(repository.getCloneUrl());

            String token;
            String gitAuthUrl;
            log.info("Using url: {}", gitUrl);

            if (event.getInstallation() != null && event.getInstallation().getId() != null) {
                installationId = event.getInstallation().getId();
                token = gitHubAppAuthService.getInstallationToken(installationId);
                token = FlowConstants.GITHUB_APP_CLONE_USER.concat(":").concat(token);
            } else {
                token = scmConfigOverrider.determineConfigToken(properties, controllerRequest.getScmInstance());
            }
            gitAuthUrl = gitAuthUrlGenerator.addCredToUrl(ScanRequest.Repository.GITHUB, gitUrl, token);

            ScanRequest request = ScanRequest.builder()
                    .application(app)
                    .product(p)
                    .project(controllerRequest.getProject())
                    .team(controllerRequest.getTeam())
                    .namespace(pullRequest.getHead().getRepo().getOwner().getLogin().replace(" ", "_"))
                    .repoName(repository.getName())
                    .repoUrl(repository.getCloneUrl())
                    .repoUrlWithAuth(gitAuthUrl)
                    .repoType(ScanRequest.Repository.GITHUB)
                    .branch(currentBranch)
                    .defaultBranch(repository.getDefaultBranch())
                    .refs(Constants.CX_BRANCH_PREFIX.concat(currentBranch))
                    .mergeNoteUri(pullRequest.getIssueUrl().concat("/comments"))
                    .mergeTargetBranch(targetBranch)
                    .email(null)
                    .isDeleteForkedProject(flowProperties.isDeleteForkedProject())
                    .scanPreset(controllerRequest.getPreset())
                    .incremental(controllerRequest.getIncremental())
                    .excludeFolders(controllerRequest.getExcludeFolders())
                    .excludeFiles(controllerRequest.getExcludeFiles())
                    .bugTracker(bt)
                    .isPRCloseEvent(action.equalsIgnoreCase("closed"))
                    .isForked(pullRequest.getHead().getRepo().getFork() != null ? pullRequest.getHead().getRepo().getFork() : false)
                    .filter(filter)
                    .thresholds(thresholdMap)
                    .organizationId(getOrganizationid(repository))
                    .gitUrl(gitUrl)
                    .hash(pullRequest.getHead().getSha())
                    .build();

            setScmInstance(controllerRequest, request);

            //Check if an installation Id is provided and store it for later use
            if (installationId != null) {
                request.putAdditionalMetadata(
                        FlowConstants.GITHUB_APP_INSTALLATION_ID, installationId.toString()
                );
            }
            /*Check for Config as code (cx.config) and override*/
            log.debug(repository.getId() + " :: Calling  getCxConfigOverride function : " + System.currentTimeMillis());
            CxConfig cxConfig = gitHubService.getCxConfigOverride(request);
            log.debug(repository.getId() + " :: Calling  overrideScanRequestProperties function : " + System.currentTimeMillis());
            request = configOverrider.overrideScanRequestProperties(cxConfig, request);
            log.debug(repository.getId() + " :: Calling  putAdditionalMetadata function : " + System.currentTimeMillis());

            request.putAdditionalMetadata(HTMLHelper.WEB_HOOK_PAYLOAD, body);
            request.putAdditionalMetadata("statuses_url", pullRequest.getStatusesUrl());
            request.setId(uid);
            //only initiate scan/automation if target branch is applicable
            if (helperService.isBranch2Scan(request, branches)) {
                log.debug(repository.getId() + " :: Calling  isBranch2Scan function End : " + System.currentTimeMillis());
                log.debug(repository.getId() + " :: Free Memory : " + Runtime.getRuntime().freeMemory());
                log.debug(repository.getId() + " :: Total Numbers of processors : " + Runtime.getRuntime().availableProcessors());
                long startTime = System.currentTimeMillis();
                log.debug(repository.getId() + " :: Start Time : " + startTime);
                flowService.initiateAutomation(request);
                long endTime = System.currentTimeMillis();
                log.debug(repository.getId() + " :: End Time  : " + endTime);
                log.debug(repository.getId() + " :: Total Time Taken  : " + (endTime - startTime));
            }
        } catch (IllegalArgumentException e) {
            return null;
        }

        return "OK";
    }

    protected ControllerRequest ensureNotNull(ControllerRequest requestToCheck) {
        return Optional.ofNullable(requestToCheck)
                .orElseGet(() -> ControllerRequest.builder().build());
    }


    protected List<String> getBranches(ControllerRequest request, FlowProperties flowProperties) {
        List<String> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getBranch())) {
            result.addAll(request.getBranch());
        } else if (CollectionUtils.isNotEmpty(flowProperties.getBranches())) {
            result.addAll(flowProperties.getBranches());
        }
        return result;
    }

    public void verifyHmacSignature(String message, String signature, ControllerRequest controllerRequest) {
        if (hmac == null) {
            log.error("Hmac was not initialized. Trying to initialize...");
            try {
                init();
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                log.error(e.getMessage(), e);
            }
        }
        try {
            setHmacToken(scmConfigOverrider.determineConfigWebhookToken(properties, controllerRequest));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error(e.getMessage());
        }

        if (hmac != null) {
            if (message != null) {
                byte[] sig = hmac.doFinal(message.getBytes(CHARSET));
                String computedSignature = "sha1=" + DatatypeConverter.printHexBinary(sig);
                if (!computedSignature.equalsIgnoreCase(signature)) {
                    log.error("Message was not signed with signature provided.");
                    throw new InvalidTokenException("Invalid Credentials: Make sure webhook token is correct");
                }
                log.info("Signature verified");
            } else {
                log.error("Signature cannot be verified because message is null.");
                throw new InvalidTokenException();
            }
        } else {
            log.error("Unable to initialize Hmac. Signature cannot be verified.");
            throw new InvalidTokenException();
        }
    }

    private String getOrganizationid(Repository repository) {
        // E.g. "cxflowtestuser/VB_3845" ==> "cxflowtestuser"
        return StringUtils.substringBefore(repository.getFullName(), "/");
    }

}
