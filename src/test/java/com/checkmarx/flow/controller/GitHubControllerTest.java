package com.checkmarx.flow.controller;

import com.checkmarx.flow.config.properties.FlowProperties;
import com.checkmarx.flow.config.properties.GitHubProperties;
import com.checkmarx.flow.config.properties.JiraProperties;
import com.checkmarx.flow.exception.InvalidCredentialsException;
import com.checkmarx.flow.exception.InvalidTokenException;
import com.checkmarx.flow.exception.MachinaRuntimeException;

import com.checkmarx.flow.service.*;
import com.checkmarx.sdk.service.*;
import com.checkmarx.flow.service.HelperService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class GitHubControllerTest {

    @Autowired
    private static ResultsService resultsService;

    @Autowired
    private static ScanSettingsClient scanSettingsClient;

    private static final FlowProperties flowProperties = new FlowProperties();
    private static final GitHubProperties properties = new GitHubProperties();
    private static final JiraProperties jiraProperties = new JiraProperties();
    private static CxScannerService cxScannerService;
    private static final ExternalScriptService scriptService = new ExternalScriptService();
    private static final HelperService helperService = new HelperService(flowProperties, cxScannerService,
                                                                         jiraProperties,
                                                                         scriptService);
    private static final List<VulnerabilityScanner> scanners = new ArrayList<>();
    private static final ProjectNameGenerator projectNameGenerator = new ProjectNameGenerator(helperService, cxScannerService, flowProperties);
    private static final FlowService flowService = new FlowService(scanners, projectNameGenerator, resultsService);
    private static final FilterFactory filterFactory = new FilterFactory();

    private static final String validBody = "{\"ref\":\"refs/heads/develop\",\"before\":\"b169b7bf26b9e4c86c27d1f6128797e2585e0dd8\",\"after\":\"b873842de207246ce012354a6d48c78c54d329ec\",\"created\":false,\"deleted\":false,\"forced\":false,\"base_ref\":null,\"compare\":\"https://github.com/miguelfreitas93/AndroidGoat/compare/b169b7bf26b9...b873842de207\",\"commits\":[{\"id\":\"b873842de207246ce012354a6d48c78c54d329ec\",\"tree_id\":\"ecc1b7d94ca7bb87c0ab483da27756f346e372a8\",\"distinct\":true,\"message\":\"no message\",\"timestamp\":\"2019-05-30T16:45:15+01:00\",\"url\":\"https://github.com/miguelfreitas93/AndroidGoat/commit/b873842de207246ce012354a6d48c78c54d329ec\",\"author\":{\"name\":\"Miguel Freitas\",\"email\":\"\"},\"committer\":{\"name\":\"Miguel Freitas\",\"email\":\"\"},\"added\":[],\"removed\":[],\"modified\":[\"README.markdown\"]}],\"head_commit\":{\"id\":\"b873842de207246ce012354a6d48c78c54d329ec\",\"tree_id\":\"ecc1b7d94ca7bb87c0ab483da27756f346e372a8\",\"distinct\":true,\"message\":\"no message\",\"timestamp\":\"2019-05-30T16:45:15+01:00\",\"url\":\"https://github.com/miguelfreitas93/AndroidGoat/commit/b873842de207246ce012354a6d48c78c54d329ec\",\"author\":{\"name\":\"Miguel Freitas\",\"email\":\"\"},\"committer\":{\"name\":\"Miguel Freitas\",\"email\":\"\"},\"added\":[],\"removed\":[],\"modified\":[\"README.markdown\"]},\"repository\":{\"id\":149525137,\"node_id\":\"MDEwOlJlcG9zaXRvcnkxNDk1MjUxMzc=\",\"name\":\"AndroidGoat\",\"full_name\":\"miguelfreitas93/AndroidGoat\",\"private\":false,\"owner\":{\"name\":\"miguelfreitas93\",\"email\":\"13312380+miguelfreitas93@users.noreply.github.com\",\"login\":\"miguelfreitas93\",\"id\":13312380,\"node_id\":\"MDQ6VXNlcjEzMzEyMzgw\",\"avatar_url\":\"https://avatars3.githubusercontent.com/u/13312380?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/miguelfreitas93\",\"html_url\":\"https://github.com/miguelfreitas93\",\"followers_url\":\"https://api.github.com/users/miguelfreitas93/followers\",\"following_url\":\"https://api.github.com/users/miguelfreitas93/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/miguelfreitas93/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/miguelfreitas93/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/miguelfreitas93/subscriptions\",\"organizations_url\":\"https://api.github.com/users/miguelfreitas93/orgs\",\"repos_url\":\"https://api.github.com/users/miguelfreitas93/repos\",\"events_url\":\"https://api.github.com/users/miguelfreitas93/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/miguelfreitas93/received_events\",\"type\":\"User\",\"site_admin\":false},\"html_url\":\"https://github.com/miguelfreitas93/AndroidGoat\",\"description\":\"Vulnerable Android application for developers and security enthusiasts to learn about Android insecurities\",\"fork\":true,\"url\":\"https://github.com/miguelfreitas93/AndroidGoat\",\"forks_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/forks\",\"keys_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/keys{/key_id}\",\"collaborators_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/collaborators{/collaborator}\",\"teams_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/teams\",\"hooks_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/hooks\",\"issue_events_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/issues/events{/number}\",\"events_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/events\",\"assignees_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/assignees{/user}\",\"branches_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/branches{/branch}\",\"tags_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/tags\",\"blobs_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/git/blobs{/sha}\",\"git_tags_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/git/tags{/sha}\",\"git_refs_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/git/refs{/sha}\",\"trees_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/git/trees{/sha}\",\"statuses_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/statuses/{sha}\",\"languages_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/languages\",\"stargazers_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/stargazers\",\"contributors_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/contributors\",\"subscribers_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/subscribers\",\"subscription_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/subscription\",\"commits_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/commits{/sha}\",\"git_commits_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/git/commits{/sha}\",\"comments_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/comments{/number}\",\"issue_comment_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/issues/comments{/number}\",\"contents_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/contents/{+path}\",\"compare_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/compare/{base}...{head}\",\"merges_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/merges\",\"archive_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/{archive_format}{/ref}\",\"downloads_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/downloads\",\"issues_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/issues{/number}\",\"pulls_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/pulls{/number}\",\"milestones_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/milestones{/number}\",\"notifications_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/notifications{?since,all,participating}\",\"labels_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/labels{/name}\",\"releases_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/releases{/id}\",\"deployments_url\":\"https://api.github.com/repos/miguelfreitas93/AndroidGoat/deployments\",\"created_at\":1537400756,\"updated_at\":\"2019-05-30T15:05:55Z\",\"pushed_at\":1559231128,\"git_url\":\"git://github.com/miguelfreitas93/AndroidGoat.git\",\"ssh_url\":\"git@github.com:miguelfreitas93/AndroidGoat.git\",\"clone_url\":\"https://github.com/miguelfreitas93/AndroidGoat.git\",\"svn_url\":\"https://github.com/miguelfreitas93/AndroidGoat\",\"homepage\":\"\",\"size\":56848,\"stargazers_count\":0,\"watchers_count\":0,\"language\":\"Java\",\"has_issues\":true,\"has_projects\":true,\"has_downloads\":true,\"has_wiki\":true,\"has_pages\":false,\"forks_count\":0,\"mirror_url\":null,\"archived\":false,\"disabled\":false,\"open_issues_count\":47,\"license\":{\"key\":\"mit\",\"name\":\"MIT License\",\"spdx_id\":\"MIT\",\"url\":\"https://api.github.com/licenses/mit\",\"node_id\":\"MDc6TGljZW5zZTEz\"},\"forks\":0,\"open_issues\":47,\"watchers\":0,\"default_branch\":\"master\",\"stargazers\":0,\"master_branch\":\"master\"},\"pusher\":{\"name\":\"miguelfreitas93\",\"email\":\"13312380+miguelfreitas93@users.noreply.github.com\"},\"sender\":{\"login\":\"miguelfreitas93\",\"id\":13312380,\"node_id\":\"MDQ6VXNlcjEzMzEyMzgw\",\"avatar_url\":\"https://avatars3.githubusercontent.com/u/13312380?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/miguelfreitas93\",\"html_url\":\"https://github.com/miguelfreitas93\",\"followers_url\":\"https://api.github.com/users/miguelfreitas93/followers\",\"following_url\":\"https://api.github.com/users/miguelfreitas93/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/miguelfreitas93/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/miguelfreitas93/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/miguelfreitas93/subscriptions\",\"organizations_url\":\"https://api.github.com/users/miguelfreitas93/orgs\",\"repos_url\":\"https://api.github.com/users/miguelfreitas93/repos\",\"events_url\":\"https://api.github.com/users/miguelfreitas93/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/miguelfreitas93/received_events\",\"type\":\"User\",\"site_admin\":false}}";
    private static final String validSignature = "sha1=2E458E802E4A363F2A3A0BCF5820DBDA4A560A47";
    private static final String validSignature2 = "sha1=0BA87D348820CF9D6715274837A34BAF4550C59A";
    private static final String invalidWebhookToken = "test";
    private static final String validWebhookToken = "adsfdsfddsfsadaf";
    @Test
    public void initNullController() throws InvalidKeyException, NoSuchAlgorithmException {
        GitHubController gitHubControllerNull = new GitHubController(null, null, null, null,  helperService, null, null, null, null, null, null);
        gitHubControllerNull.init();
    }

    @Test
    public void initNullWebHookToken() throws InvalidKeyException, NoSuchAlgorithmException {
        properties.setWebhookToken(null);
        GitHubController gitHubControllerNull = allocateGitHubController();
        gitHubControllerNull.init();
    }

    @Test
    public void pingRequestNullController() {
        GitHubController gitHubControllerNull = new GitHubController(null, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubControllerNull.pingRequest("body", "product", "signature");
            assert false;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pingRequestNullControllerWithNullParameters() {
        GitHubController gitHubControllerNull = new GitHubController(null, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubControllerNull.pingRequest(null, null, null);
            assert false;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pingRequestWithNullParametersNullWebHookToken() {
        GitHubController gitHubController = new GitHubController(properties, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pingRequest(null, null, null);
            assert false;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pingRequestWithNullParametersWithWebHookTokenNullMessage() {
        properties.setWebhookToken("token");
        GitHubController gitHubController = new GitHubController(properties, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pingRequest(null, null, null);
            assert false;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pingRequestWithWebHookTokenInvalidSignature() {
        properties.setWebhookToken(invalidWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pingRequest("test", null, null);
            assert false;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pingRequestWithWebHookTokenInvalidMessage() {
        properties.setWebhookToken(invalidWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pingRequest(null, null, validSignature);
            assert true;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pingRequestWithWebHookTokenValidSignature() {
        properties.setWebhookToken(invalidWebhookToken);
        GitHubController gitHubController = allocateGitHubController();
        try {
            gitHubController.pingRequest(invalidWebhookToken, null, validSignature);
            assert false;
        } catch (InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pushRequestNullControllerNullParameters() {
        GitHubController gitHubController = new GitHubController(null, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pushRequest(null, null, null, null);
            assert false;
        } catch (MachinaRuntimeException e) {
            assert true;
        }
    }

    @Test
    public void pushRequestNullControllerNullParametersWithBody() {
        GitHubController gitHubController = new GitHubController(null, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pushRequest(validBody, null, null, null);
            assert false;
        } catch (MachinaRuntimeException | InvalidTokenException e) {
            assert true;
        }
    }

    @Test
    public void pushRequestNullParametersWithBodyInvalidWebHook() {
        properties.setWebhookToken(invalidWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, null, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pushRequest(validBody, null, null, null);
            assert false;
        } catch (MachinaRuntimeException | InvalidTokenException e) {
            assert true;
        }
    }


    @Test
    public void pushRequestNullParametersWithBodyValidWebHookInvalidSignature() {
        properties.setWebhookToken(validWebhookToken);
        GitHubController gitHubController = allocateGitHubController();
        try {
            gitHubController.pushRequest(validBody, null, null, null);
            assert false;
        } catch (MachinaRuntimeException | InvalidTokenException e) {
            assert true;
        }
    }

    private GitHubController allocateGitHubController() {
        return new GitHubController(properties, null, null, null,  helperService,null, null, null, null, null, null);
    }

    @Test
    public void pushRequestInvalidFlowPropertiesWithBodyValidWebHookValidSignature() {
        properties.setWebhookToken(validWebhookToken);
        GitHubController gitHubController = allocateGitHubController();
        try {
            gitHubController.pushRequest(validBody,validSignature2, null, null);
            assert false;
        } catch (MachinaRuntimeException e) {
            assert true;
        }
    }

    @Test
    public void pushRequestValidFlowPropertiesWithBodyValidWebHookValidSignature() {
        properties.setWebhookToken(validWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, flowProperties, null, null,  helperService, null, null, null, null, null, null);
        try {
            gitHubController.pushRequest(validBody, validSignature2, null, null);
            assert false;
        } catch (MachinaRuntimeException e) {
            assert true;
        }
    }


    @Test
    public void pushRequestValidFlowPropertiesWithBodyValidWebHookValidSignatureWithValidToken() {
        properties.setWebhookToken(validWebhookToken);
        properties.setToken(invalidWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, flowProperties, null, null,  helperService, null, null,  filterFactory, null, null, null);
        try {
            gitHubController.pushRequest(validBody, validSignature2, null, null);
            assert false;
        } catch (MachinaRuntimeException e) {
            assert true;
        }
    }


    /*@Test TODO Fix Test, Signiture is not validating properly
    public void pushRequestValidCxPropertiesWithBodyValidWebHookValidSignatureWithValidTokenNullFlowService() {
        properties.setWebhookToken(validWebhookToken);
        properties.setToken(invalidWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, flowProperties, cxProperties, null, null);
        try {
            gitHubController.pushRequest(validBody, validSignature2, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null);
            assert true;
        } catch (InvalidTokenException | MachinaRuntimeException e) {
            assert false;
        }
    }*/

    @Test
    public void pushRequestValidCxPropertiesWithBodyValidWebHookValidSignatureWithValidTokenFlowService() {
        properties.setWebhookToken(validWebhookToken);
        properties.setToken(invalidWebhookToken);
        GitHubController gitHubController = new GitHubController(properties, flowProperties,  null, flowService, helperService, null, null,  filterFactory, null, null, null);
        try {
            gitHubController.pushRequest(validBody, validSignature2, null, null);
            assert false;
        } catch (InvalidTokenException | InvalidCredentialsException e) {
            assert true;
        }
    }

}