## <a name="tableofcontents">Table of Contents</a>
* [GitHub Webhook Lab](#github)
    * [Requirements](#githubrequirements)
    * [Prep](#githubprep)
    * [Triggering Scans with CxFlow](#githubtriggering)
* [GitLab Webhook Lab](#gitlabWebhook)
    * [Requirements](#gitlabrequirements)
    * [Prep](#gitlabprep)
    * [Triggering Scans with CxFlow](#gitlabtriggering)
* [GitLab CI/CD](#gitlabcicd)
    * [Requirements](#gitlabcicdrequirements)
    * [CI/CD Variables](#gitlabcicdvaiables)
    * [Pipeline Configuration](#gitlabpipelineconfiguration)
    * [Run Pipeline and review the results](#gitlabrunpipeline)
* [Azure DevOps Webhook Lab](#azure)
    * [Requirements](#adorequirements)
    * [Prep](#adoprep)
    * [Triggering Scans with CxFlow](#adotriggering)
* [Azure DevOps Pipeline](#adopipeline)
    * [Windows Agents](#windowsagents)
    * [Docker Container](#adopipelinedocker)
    * [Configuration](#adopipelineconfiguration)
    * [Upgrading to CxSAST v9.0 and Above](#adopipelinenine)
    * [Environment Variables](#adopipelineenvironmentvariables)
    * [Scripts](#adopipelinescripts)
    * [Building](#adopipelinebuilding)
* [Bitbucket Cloud Webhook Lab](#bitbucket)
    * [Requirements](#bitbucketrequirements)
    * [Prep](#bitbucketprep)
    * [Triggering Scans with CxFlow](#bitbuckettriggering)
* [CxFlow CLI & JIRA Lab](#clijira)
    * [Requirements](#clirequirements)
    * [Prep](#cliprep)
    * [Triggering Scans with CxFlow](#clitriggering)
* [CxFLow Batch Mode Lab](#batch)
    * [Requirements](#batchrequirements)
    * [SMTP Server Prep](#smtpserverprep)
    * [Triggering Scans with CxFlow](#batchtriggering)
    * [EmailPNEVulns.ps1](#emailpne)
* [GitHub Overrides & JIRA Lab](#githuboverrides)
    * [Requirements](#overridesrequirements)
    * [Prep](#overridesprep)
    * [Triggering Scans with CxFlow](#overridestriggering)
<br/>

## <a name="github">Github Webhook Lab</a>
* [Requirements](#githubrequirements)
* [Prep](#githubprep)
* [Triggering Scans with CxFlow](#githubtriggering)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>
This lab is designed to teach the following topics:

* How to scan on a Pull Request to a Protected Branch
* How to scan on a Push to Protected Branch
* Github Issue Creation on Push 

### <a name="githubrequirements">Requirements:</a> 
##### [Top of Lab](#github)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.

```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: GitHub
  bug-tracker-impl:
  - GitHub
  branches:
  - master
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  #   - Urgent
  #   - Confirmed
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<username>###
  password: ###<password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl


github:
  webhook-token: 12345
  token: ###<githubtoken>###
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true
```
* If following this guide for demo purposes, you can use ngrok to generate a resolvable address for your CxSAST manager. This guide includes ngrok in its examples
    * Download ngrok from https://ngrok.com/download and unzip to the CxFlow folder
    * Start ngrok on port 8982 by opening CMD and entering the following command:
```
cd C:\CxFlow
ngrok http 8982
```
### <a name="githubprep">GitHub Prep</a>
##### [Top of Lab](#github)
* Create an account at www.github.com
* Create a public repository titled CxFlowGithub
* Import code from your favorite small demo codebase on github. This guide will use <br>https://github.com/psiinon/bodgeit
* Create a token by clicking on your profile in upper right corner > settings
    * Click Developer settings > Personal Access Tokens > Generate New Token
    * Give the token a name, for example cxFlow-minimal, and both repo:status and public_repo scopes. 
    * Ensure "Issues" are enabled on the project Settings > Options > Features > Issues
[[/Images/guide1.png|Example name and scope for GitHub token]]
    * Copy this token and keep it safe. It should be posted into the token \<\> of the application.yml
* Once the .yml is completely filled out, start CxFlow in webhook mode by opening CMD prompt/shell, navigate to your CxFlow directory (created above) and entering the following, after updating the path\to\CxFlow folder:
```
cd C:\CxFlow
java -jar cx-flow-1.5.4.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```
* Create a webhook by selecting your profile and selecting the repo you just created
    * Navigate to Settings > Webhooks > Add Webhook and fill in the details
        * Payload URL: ngrok example: http://4d91e7ed.ngrok.io
        * Content type:  application/json
        * Secret: Webhook token from .yml file, in this example 12345
        * Select Events:  Pull Requests, Pushes
    * Click Add Webhook, there should be a checkmarx next to the hook name now
### <a name="githubtriggering">Triggering Scans with CxFlow</a>
##### [Top of Lab](#github)
* Open your IDE of choice. This demo will use IntelliJ
    * Check out code using Check out from Version Control, input the URL for your repo example:
<br/>[https://github.com/<username\>/CxFlowGithub](http://github.com)
    * Open README.md and add a line, example: CxFlowMasterPush-Test1
    * Commit to local git repo and push to origin with comments by clicking the following: VCS > Git > Commit File enter a message like CxFlow push to a protected branch
    * Click commit and push
    * Click Push and enter Github credentials on popup. Username is your username, password is the token you created above.
* Navigate to the Checkmarx web portal. You should see a new scan in the CxSAST queue
<br/>Notice the project name is the RepoName-Branch
<br/>Notice the team is the Github organization. This is set by the team line in the .yml file. It auto creates a team if it does not exist. This can be overridden in the config file with the multi-tenant setting. Please see the CxFlow configuration page for more information.
* When the scan finishes, you should see issues on the Issue tab of your Github repo 
<br/>[https://github.com/<username\>/CxFlowGithub/issues](http://github.com/)
* Examine the following issue CX SQL_Injection @ roost/basket.jsp [master]
* Open the Checkmarx link and examine the finding
* We will now trigger CxFlow from a Pull Request to a protected branch, from branch security-fix to master
* Open IntelliJ and create a new local branch called security-fix VCS > Git > Branches > New Branch
* Type security-fix and click ok
* Open basket.jsp under the root folder and replace lines 53-55 with the following
```Java
//Statement stmt = conn.createStatement();
//Security Fix
PreparedStatement preparedStatement = con.prepareStatement(sql);
try {
//ResultSet rs = stmt.executeQuery("SELECT * FROM Baskets WHERE basketid = " + basketId);
String sql = "SELECT * FROM Baskets WHERE basketid =?");
preparedStatement.setString(1, basketId);
ResetSet rs = preparedStatement.executeQuery();
```
* add the following to line 7 to import the correct package
```
<%@ page import="java.sql.PreparedStatement" %>
```
* Save the file, commit to the local repo and push to origin:
    * File > Save All
    * VCS > Git > Commit File and add commit message like added prepared statement on line 55
    * Click Commit and Push, then Push
* Navigate to Github
    * Navigate to Pull Requests
    * Click Compare and Pull Request > Create Pull Request
    * Alternatively you can create the pull request through the IDE. In IntelliJ click VCS > Create Pull Request
* In Github there will be some checks that have not finished yet - Checkmarx Scan
* In the Checkmarx web portal there will be a new CxSAST scan where the project name is RepoName-Branch
    * Once the scan finished you can see the post in the Github merge pull request comments with all the vulnerabilites found
    * The basket.jsp SQLi is gone
    * Click Merge Pull Request > Confirm Merge to accept the risk CxSAST has posted in the comments
* After confirming the pull request, there will be a new CxSAST scan in the Checkmarx web portal for the master branch
* In Github Issues there will be one fewer vulnerability
* In the Checkmarx web portal, the CxFlowGithub-master project will now have both solved and recurrent issues. 
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#github)

## <a name="gitlabWebhook">GitLab Webhook Lab</a>
* [Requirements](#gitlabrequirements)
* [Prep](#gitlabprep)
* [Triggering Scans with CxFlow](#gitlabtriggering)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>
This lab is designed to teach the following topics:

* How to scan on a Merge Request to a Protected Branch
* How to scan on a Push to Protected Branch
* GitLab Issue Creation on a Push to Protected Branch
        
### <a name="gitlabrequirements">Requirements:</a>
##### [Top of Lab](#gitlabWebhook)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.

```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: GitLab
  bug-tracker-impl:
  - GitLab
  branches:
  - master
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  #   - Urgent
  #   - Confirmed
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<username>###
  password: ###<password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl


gitlab:
  webhook-token: 12345
  token: ###<gitlabtoken>###
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4
  false-positive-label: false-positive
  block-merge: true
```
* If following this guide for demo purposes, you can use ngrok to generate a resolvable address for your CxSAST manager. This guide includes ngrok in its examples
    * Download ngrok from https://ngrok.com/download and unzip to the CxFlow folder
    * Start ngrok on port 8982 by opening CMD and entering the following command:
```
cd C:\CxFlow
ngrok http 8982
```
### <a name="gitlabprep">Gitlab Prep</a>
##### [Top of Lab](#gitlabWebhook)
* Create an account at http://www.gitlab.com
* Create a new private group called <yourname\>-checkmarx
* Create a new subgroup called GitLab CxFlow
* Create a new private project called cxFlowGitLab
    * Import code from your favorite small demo codebase
    * This lab will use htts://github.com/psiion/bodgeit
* Click Import Project \> Repo By URL
    * Git Repository URL htts://github.com/psiion/bodgeit
    * Project Name = CxFlowGitLab
    * Ensure the project's status is set to private and click Create Project
* Create a token by clicking your profile in upper right corner >settings
    * Click Access Tokens & add a personal access token
    * Give the token api, read_user, write_repository, read_registry scopes
    * Copy this token and keep safe - it should be pasted into the token: <> of the application.yml
* After .YML file is completely filled out and saved, start CxFlow in webhook mode by opening a CMD prompt and typing the following
<br/> **Note** change the version # for CxFlow appropriately and insert your correct path to the applicaiton.yml file, for example: 
<br/>```--spring.config.location="C:\CxFlow\application.yml"```
```
cd C:\CxFlow
java -jar cx-flow-1.5.4.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```
* Create a webhook by selecting Projects>Your Projects and select the repo you just created 
* Click Settings>Webhooks and fill in details
    * URL = ngrok location of cxflow that is running - example: http://4d91e7ed.ngrok.io
    * Secret = webhook-token: from .yml file - example: 12345
    * Trigger = Push events, Merge request events
    * Click Add Webhook 

### <a name="gitlabtriggering">Triggering Scans with CxFlow</a>
##### [Top of Lab](#gitlabWebhook)
* Open your favorite IDE of choice and clone the new repo, this lab will use Intellij & the repo created above: https://gitlab.com/<your_organization\>/cxflowgitlab
    * Open IntelliJ, click Check out from Version Control and input above URL, select next until done
* Open README.md and add the following line, then save
    * CxFlowMasterPush-Test1
* Commit to local git repo and push to origin with comments by clicking the following
    * VCS \> Git \> Commit File
    * Enter the following commit message: Cxflow push to a protected branch
* Click Commit and Push
* Click Push
* Enter GitLab credentials on popup
    * Username = username
    * Password = token created
* You show now see a scan in the CxSAST queue
<br/>Notice the project name = RepoName-Branch
<br/>Notice the team of the new project = GitLab organization
    * This is due to the team line in the .yml file - it auto-creates a team if it does not exist
    * This can be overridden and changed in the configs
* Once the scan finishes you should see issues in the issue tab on your GitLab project
    * Example: https://gitlab.com/<your_organization\>/cxflowgitlab/issues
    * Examine the following issue CX SQL_Injection @ root/basket.jsp [master]
    * Open the Checkmarx link and examine the finding
* We will now trigger CxFlow from a Pull Request to a protected branch
* Open Intellij and create a new local branch called security-fix
    * Click VCS \> Git \> Branches \> New Branch
    * Type "security-fix" and click ok
* Open basket.jsp underneath the root folder and replace lines 53-55 with the following

```java
//Statement stmt = conn.createStatement();
//Security Fix
PreparedStatement preparedStatement = con.prepareStatement(sql);
try {
//ResultSet rs = stmt.executeQuery("SELECT * FROM Baskets WHERE basketid = " + basketId);
String sql = "SELECT * FROM Baskets WHERE basketid =?");
preparedStatement.setString(1, basketId);
ResetSet rs = preparedStatement.executeQuery();
```
* Add the following on line 7 to import the correct package
```
<%@ page import="java.sql.PreparedStatement" %>
```
* Alternatively, you can pull down results using the CxViewer in Intellij and remediate based on the ID
* Save the file, commit to the local repo and push to origin
    * Click Save All
    * Click VCS \> Git \> Commit File and add a commit message, for example "added preparedstmt on line 55"
    * Click Commit and Push followed by Push
* Go to GitLab and Merge Requests on your project page
    * Click New Merge request 
    * Select cxflowgitlab security-fix as the source branch
    * Select  cxflowgitlab master as the target branch
    * Click Compare branches and continue
    * Ensure the title does not have WIP: & click submit merge request
* Notice in GitLab that some checks haven't been completed yet - Checkmarx Scan
* Notice a new scan in CxSAST with Project name = RepoName-Branch
    * Once the scan finishes, you can see the post in the GitLab merge request comments with all the vulnerabilities found
    * Notice that the basket.jsp SQLi is gone
    * Click Merge & uncheck “Delete source branch” to accept the risk CxSAST has posted about in the comments
* Notice a new scan in CxSAST with Project name = RepoName-Branch for the master branch
* Go back to the GitLab issues page and note that there are only 15 issues remaining
* Go to CxSAST and note that cxflowgitlab-master project has solved issues and recurrent issues
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#gitlabWebhook)

## <a name="gitlabcicd">GitLab CI/CD</a>
  * [Requirements](#gitlabcicdrequirements)
  * [CI/CD Variables](#gitlabcicdvaiables)
  * [Pipeline Configuration](#gitlabpipelineconfiguration)
  * [Run Pipeline and review the results](#gitlabrunpipeline)
  * [Back to Labs Table of Contents](#tableofcontents)

There are several ways of integrating Checkmarx security scans into GitLab’s ecosystem. This document specifically outlines how to integrate GitLab with Checkmarx’s Containerized CxFlow CLI.
Checkmarx integrates with GitLab, enabling the identification of new security vulnerabilities with proximity to their creation.  GitLab integration triggers Checkmarx scans as defined by the GitLab CI/CD pipeline.  Once a scan is completed, both scan summary information and a link to the Checkmarx Scan Results will be provided.  Both CxSAST and CxSCA are supported within the GitLab integration.

The following steps represent the containerized CxFlow CLI integration flow:
1. GitLab’s CI/CD pipeline is triggered (as defined in the .gitlab-ci.yml file)
2. During the test stage of GitLab’s CI/CD pipeline, Checkmarx’s containerized CxFlow CLI is invoked
3. CxFlow CLI triggers a security scan via the Checkmarx Scan Manager
4. Results can be configured to be displayed with GitLab’s ecosystem or a supported bug tracker via CxFlow YAML configuration
   * a. Results will be within Checkmarx Scan Results within the Checkmarx Manager Server
   * b. Results can be accessed within GitLab’s Merge Request Overview (if the scan was initiated during a Merge Request)
   * c. Results can be accessed within GitLab’s Issues if configured (or can be filtered into external bug tracker tools)
   * d. Results can be accessed within GitLab’s security dashboard, if you have access to it (Gold/Ultimate packages or if your project is public)

` ! Within GitLab, CxFlow CLI will zip the source directory of the repository and send it to the Checkmarx Scan Manager to perform the security scan `
 
##### [Top of Lab](#gitlabcicd)
### <a name="gitlabcicdrequirements">Requirements</a>
GitLab can access a running Checkmarx CxSAST Server with an up-to-date Checkmarx license
If performing CxSCA scans, you must have a valid CxSCA license and GitLab must be able to access the CxSCA tenant
To review scan results within GitLab’s Security Dashboard, you need the Gold/Ultimate tier or the GitLab project must be public
  * To review results in the issue management of your choice (i.e. JIRA) configuration is needed in the CxFlow YAML file, please refer to [Bug Tracker documentation](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels)

##### [Top of Lab](#gitlabcicd)
### <a name="gitlabcicdvaiables">CI/CD Variables</a>
To allow for easy configuration, it is necessary to create environment variables with GitLab to run the integration.  For more information on GitLab CI/CD variables, visit here: [GitLab: CI/CD - Environment Variables](https://gitlab.com/help/ci/variables/README#gitlab-cicd-environment-variables)
Edit the CI/CD variables under Settings → CI / CD → Variables and add the following variables for a CxSAST and/or CxSCA scan :

` ! The key CX_FLOW_CONFIG variable must be of type "File" `

[[/Images/gitlab_settings.png]]

Variable     | Value 
--------------------|-------------------
GITLAB_TOKEN | <p>API token to create Merge Request Overview entries, should have “api” privileges. <br>To create a personal token, click your Gitlab profile in the upper right corner >settings <br><br>- Click Access Tokens and add a personal access token.Click Access Tokens and add a personal access token. <br>- Give the token api, read_user, write_repository, read_registry scopes. <br><br> For additional information on creating a Personal Access Token, refer to [GitLab: Personal Access Tokens](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html) </p> 
BUG_TRACKER   (Type: Variable)| Type of bug tracking ('GitLabDashboard' or ‘GitLab’).  For vulnerabilities to be exported to GitLab’s Dashboard, use ‘GitLabDashboard’ and for vulnerabilities to be added to GitLab’s Issues, use ‘GitLab’  For more details on complete list of Bug Trackers, please refer to [CxFlow Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration) 
CHECKMARX_PASSWORD   (Type: Variable) | Password for CxSAST 
CHECKMARX_SERVER   (Type: Variable) | The base URL of CxSAST Manager Server (i.e. https://checkmarx.company.com) 
CHECKMARX_USERNAME   (Type: Variable) | User Name for the CxSAST Manager.  User must have ‘SAST Scanner’ privileges.  For more information on CxSAST roles, please refer to [CxSAST / CxOSA Roles and Permissions](https://checkmarx.atlassian.net/wiki/spaces/KC/pages/1178009601/CxSAST+CxOSA+Roles+and+Permissions+v9.0.0+and+up) 
CX_FLOW_CONFIG   (Type: File)|  [See example below](#configfile)
CHECKMARX_TEAM   (Type: Varia ble) | Checkmarx Team Name (i.e. /CxServer/teamname) 
SCA_TENANT   (Type: Variable) | The name of the CxSCA Account (i.e. SCA-CompanyName).  **Only needed if you have a valid license for CxSCA** 
SCA_USERNAME   (Type: Variable) | The username of the CxSCA Account.  **Only needed if you have a valid license for CxSCA**  
SCA_PASSWORD   (Type: Variable) | The password of the CxSCA Account.  **Only needed if you have a valid license for CxSCA** 

###### <a name="configfile">Configuration File example</a>
`! enable-vulnerability-scanners: - sca and the sca block are only needed if you have a valid CxSCA license and tenant.`
```yaml
logging:
  file:
    name: cx-flow.log

cx-flow:
  bug-tracker:  ${BUG_TRACKER}
  bug-tracker-impl:
    - GitLab
    - GitLabDashboard
  filter-severity:
    - High
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  break-build: true
  enabled-vulnerability-scanners:
    - sast
    - sca

checkmarx:
  version: 9.0
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ${CHECKMARX_SERVER}
  url: ${CHECKMARX_SERVER}/cxrestapi
  portal-url: ${CHECKMARX_SERVER}/cxwebinterface/Portal/CxWebService.asmx
  multi-tenant: true
  incremental: true
  scan-preset: Checkmarx Default
  configuration: Default Configuration
  scan-timeout: 120

sca:
  appUrl:  https://sca.scacheckmarx.com
  apiUrl:  https://api.scacheckmarx.com
  accessControlUrl:  https://platform.checkmarx.net

gitlab:
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  url: https://gitlab.com
  file-path: ./gl-sast-report.json
```

##### [Top of Lab](#gitlabcicd)
### <a name="gitlabpipelineconfiguration">Pipeline Configuration</a>
The GitLab CI/CD pipeline is controlled by a file named ‘.gitlab-ci.yml’ located in the root directory of the project.  Please refer to [GitLab: CI YAML](https://docs.gitlab.com/ee/ci/yaml/README.html) for more info.

`! It is suggested not to over-pollute your companies already existing '.gitlab-ci.yml' file.  Instead, create a new YAML file in the root directory named ‘.checkmarx.yml’ and include it in ‘.gitlab-ci.yml’`

` # Note that image is a docker container maintained by Checkmarx`
#### .checkmarx.yml
```yaml
image: docker:latest
services:
  - docker:dind

.checkmarx_sast:
  stage: test
  image:
    name: docker.io/checkmarx/cx-flow
    entrypoint: ['']
  script:
    - cat ${CX_FLOW_CONFIG} > application.yml
    - |
      if [ "$CI_PIPELINE_SOURCE" == "merge_request_event" ]; then 
        java -jar /app/cx-flow.jar --spring.config.location=./application.yml \
          --scan \
          --cx-team="${CHECKMARX_TEAM}" \
          --cx-project="${CI_PROJECT_NAME}-${CI_COMMIT_REF_NAME}" \
          --app="${CI_PROJECT_NAME}" \
          --project-id=${CI_PROJECT_ID} \
          --merge-id=${CI_MERGE_REQUEST_IID} \
          --bug-tracker=GITLABMERGE \
          --cx-flow.break-build=false \
          --f=.
      else
        java -jar /app/cx-flow.jar --spring.config.location=./application.yml \
          --scan \
          --cx-team="${CHECKMARX_TEAM}" \
          --cx-project="${CI_PROJECT_NAME}-${CI_COMMIT_REF_NAME}" \
          --app="${CI_PROJECT_NAME}-${CI_COMMIT_REF_NAME}" \
          --branch="${CI_COMMIT_REF_NAME}" \
          --repo-name="${CI_PROJECT_NAME}" \
          --namespace="${CI_PROJECT_NAMESPACE##*/}" \
          --cx-flow.break-build=false \
          --f=.
      fi
  artifacts:
    when: on_success
    reports:
      sast: gl-sast-report.json
    paths:
      - gl-sast-report.json
```

#### .gitlab-ci.yml
```yaml
include: '.checkmarx.yml'

stages:
  - test

checkmarx_sast:
  stage: test
  only:
    - master
    - merge_requests
  extends: .checkmarx_sast
```

##### [Top of Lab](#gitlabcicd)
### <a name="gitlabrunpipeline">Run Pipeline and review the results</a>
####Run pipeline
To run a Checkmarx scan, you need to trigger the pipeline.  The trigger is based on the .gitlab-ci.yml and in the provided sample above, it will be triggered on Merge Requests and on changes to the master branch
* For information on triggering a pipeline scan, please refer to [GitLab: triggering a pipeline](https://docs.gitlab.com/ee/ci/triggers/README.html#triggering-a-pipeline)
* For information on Merge Requests, please refer to [GitLab: Merge Requests](https://docs.gitlab.com/ee/user/project/merge_requests/)

#### Review results
While the scan results will always be available in the Checkmarx UI, users can also access results within the GitLab ecosystem.  Currently there are three different ways to review results from the scan:
* Merge Request Overview
* GitLab Issues
* Security Dashboard

#### Merge Request Discussion
When you have configured the .gitlab-ci.yml file to scan on merge_requests issues (please refer to [GitLab: Pipelines for Merge Requests](https://docs.gitlab.com/ee/ci/merge_request_pipelines/)), a high level report of the Checkmarx scan will be displayed within GitLab Merge Request Overview.  

An example of a Merge Request with a Checkmarx scan report can be found in the below image.
[[/Images/gitlab_merge_request.png]]

#### GitLab Issues
When you have configured the BUG_TRACKER variable to use “GitLab”, CxSAST and CxSCA issues found in Checkmarx will be opened within [GitLab Issues](https://docs.gitlab.com/ee/user/project/issues/)

For more information on GitLab issues, please refer to GitLab: Issues

An example of Issues created can be found in the below image.
[[/Images/gitlab_issues.png]]

#### Security Dashboard
With the Gold/Ultimate tier for GitLab, or if the project is public, you can review results in GitLab’s Security Dashboard.For information on GitLab’s Security Dashboard, please refer to [GitLab: Security Dashboard](https://docs.gitlab.com/ee/user/application_security/security_dashboard/index.html)

An example of vulnerabilities displayed in the Security Dashboard can be found in the below image.
##### [Top of Lab](#gitlabcicd)

## <a name="azure">Azure DevOps Webhook Lab</a>
* [Requirements](#adorequirements)
* [Prep](#adoprep)
* [Triggering Scans with CxFlow](#adotriggering)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>
This lab is designed to teach the following topics:

* How to Commit & Push code changes using Visual Studio Code
* How to  scan on a Merge Request to a Protected Branch
* How to  scan on a Push to Protected Branch
* Azure Work Item creation on a Push to Protected Branch
* Update, Open or Close tickets from Batch mode via CLI

### <a name="adorequirements">Requirements:</a>
##### [Top of Lab](#azure)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.
```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: Azure
  bug-tracker-impl:
  - Azure
  branches:
  - master
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  #   - Urgent
  #   - Confirmed
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<username>###
  password: ###<password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl


azure:
  webhook-token: cxflow:12345
  token: ###<ADOtoken>###
  url: https://dev.azure.com/
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  #block-merge: true
  closed-status: Done
  open-status: "To Do"
```
* If following this guide for demo purposes, you can use ngrok to generate a resolvable address for your CxSAST manager. This guide includes ngrok in its examples
    * Download ngrok from https://ngrok.com/download and unzip to the CxFlow folder
    * Start ngrok on port 8982 by opening CMD and entering the following command:
<br/> **Note** Ngrok also tunnels traffic on port 443 for SSL.  We will be using this for Azure DevOps webhooks since the endpoint is required to be https.
```
cd C:\CxFlow
ngrok http 8982
```

### <a name="adoprep">Azure DevOps Prep</a>
##### [Top of Lab](#azure)
* Create an account at https://azure.microsoft.com/en-us/services/devops/
* Create a new organization if one does not already exist
* Create a new private project called CxFlowADO
    * Make sure repo type is Git under Advanced
* Click Repos & Import code from your favorite small demo codebase on GitHub
    * This lab will use - https://github.com/jerryhoff/WebGoat.NET 
* Create a token by clicking your profile in upper right corner > Personal Access Tokens
    * Give the token a name and change Expiration to Custom defined and set to a year
    * Give the token full access to Work Items, Code, Build, Release
    * Copy this token and keep safe - it should be pasted into the token: <> of the application-azure.yml
* After .YML file is completely filled out and saved, start CxFlow in webhook mode by opening a CMD prompt and typing the following
<br/> **Note** change the version # for CxFlow appropriately and insert your correct path to the applicaiton.yml file, for example: 
<br/>--spring.config.location="C:\CxFlow\application.yml"
```
cd C:\CxFlow
java -jar cx-flow-1.5.4.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```
* Create a webhook by selecting in the upper left corner Azure DevOps &  select the new repo you just created 
* Create a Webhook for Merge Requests
    * Click Project Settings \> Service hooks \> Create subscription and fill in details
    * Click Web Hooks then Next
        * Change drop down to Pull request created
        * Repository = CxFlowADO
        * Branch = master
        * URL = https://<cxflow\>/ado/pull 
        * **Note** <cxflow\> is https ngrok location of cxflow that is running
        * Example: https://4d91e7ed.ngrok.io/ado/pull
        * Basic authentication username = webhook-token: left side of : from .yml file - example: cxflow
        * Basic authentication password = webhook-token: right side of : from .yml file - example: 12345
    * Click Test and a green check should appear, then click Finish
* Create a Webhook for Push to Master
    * Click Project Settings > Service hooks >  Create subscription and fill in details
    * Click Web Hooks then Next
        * Change drop down to Code pushed
        * Repository = CxFlowADO
        * Branch = master
        * URL = https://<cxflow\>/ado/push 
        * **Note** <cxflow\> is https ngrok location of cxflow that is running
        * Example: https://4d91e7ed.ngrok.io/ado/push
        * Basic authentication username = webhook-token: left side of : from .yml file - example: cxflow
        * Basic authentication password = webhook-token: right side of : from .yml file - example: 12345
    * Click Add Webhook 

### <a name="adotriggering">Triggering Scans with CxFlow</a>
##### [Top of Lab](#azure)
* Open your favorite IDE of choice and clone the new repo
    * This lab will use VS Code 
    * VS Code download - https://code.visualstudio.com/download
    * Select View \> Command Pallete - remember this shortcut
    * Type git:clone and enter the your ADO URL
    * Use the token created earlier for your password & click Open after the clone
* Open README.md and add the following line & save
    * CxFlowMasterPush-Test1
* Commit to local git repo & push to origin with comments by entering the following in the command palette
    * git:commit - always stage changes
    * message = Cxflow push to a protected branch
    * git:push
* You show now see a scan in the CxSAST queue
    * Notice the project name = RepoName-Branch
    * Notice the team of the new project = ADO team name
        * This is due to the team line in the .yml file - it auto-creates a team if it does not exist
        * This can be overridden and changed in the configs
* Once the scan finishes you should see work items in the Boards>Work items tab in the project
* To view all tickets you can create a new query view
    * Click Open in Queries>Editor & edit the following for each entry
    * Clause 1
        * Field = Area Path
        * Operator = In
        * Value = CxFlowADO
    * Clause 2 - And
        * Field = Title
        * Operator = Contains Words
        * Value = SQL_Injection
* Run to the query to be sure it works and then Save Query
* You can now see all the SQLi work items by selecting Boards>Queries & the query you just created
* Examine the following issue CX SQL_Injection @ WebGoat/WebGoatCoins/ProductDetails.aspx.cs [master]
* Open the Checkmarx link and examine the finding
[[/Images/adolab1.png|Example of Azure DevOps Work Item]]
<br/>**Note** CxFlow only shows the source file of the vulnerability - using the viewer is key to remediation
*Open VSCode and create a new local branch called fix/issue-## where ## is the ticket created in Azure DevOps
    * git:create branch
        * Branch name = fix/issue-##
    * Fix all of the following SQL Injection vulnerabilities
        * **Note** The CxQL query Find_SQL Sanitize must be overridden with the following query for these fixes to work properly until the bug 211957 is closed.
```
result = base.Find_SQL_Sanitize();

CxList createCommand = All.FindByMemberAccess("connection.CreateCommand");
CxList sqlCommand = createCommand.GetAssignee();
sqlCommand.Add(All.FindAllReferences(sqlCommand));
sqlCommand.Add(All.FindByType("SqliteCommand"));

CxList SqlCeCommandParameters = sqlCommand.GetMembersOfTarget().FindByShortName("Parameters"); 
result.Add(SqlCeCommandParameters.GetMembersOfTarget().FindByShortNames(new List<string> {"AddWithValue", "Add", "AddRange"}));
```
       
* In the file WebGoat/App_Code/DB/MySqlDbProvider.cs
* Replace lines 203-205 with the following to remediate data flow from the “Value” source object on line 89 before command.ExecuteScalar()

```
                    string sql = "select email from CustomerLogin where customerNumber = @customerNumber";
                    MySqlCommand command = new MySqlCommand(sql, connection);
                    command.Parameters.AddWithValue("@customerNumber", customerNumber);
```

* Replace lines 273 with the following to remediate data flows from the “Value” & “Text” source objects on line 41

```
            string sql = "insert into Comments(productCode, email, comment) values (@productCode=productCode,@email=email,@comment=comment)";
```

* Insert the following at line 283 before command.ExecuteNonQuery();

```
                    command.Parameters.AddWithValue("@productCode", productCode);
                    command.Parameters.AddWithValue("@email", email);
                    command.Parameters.AddWithValue("@comment", comment);
```

* Add the following on line 9 to import the correct package

```
using System.Data.SqlClient; 
```

* WebGoat/App_Code/DB/SqliteDbProvider.cs
* Replace lines 190-192 with the following to remediate data flow from the “Value” source object on line 89 before command.ExecuteScalar()

```
                    string sql = "select email from CustomerLogin where customerNumber = @customerNumber";
                    SqliteCommand command = new SqliteCommand(sql, connection);
                    command.Parameters.AddWithValue("@customerNumber", customerNumber);
```

* Replace lines 265 with the following to remediate data flows from the “Value” & “Text” source objects on line 41

```
            string sql = "insert into Comments(productCode, email, comment) values (@productCode=productCode,@email=email,@comment=comment)";
```

* Insert the following at line 275 before command.ExecuteNonQuery();

```
                    command.Parameters.AddWithValue("@productCode", productCode);
                    command.Parameters.AddWithValue("@email", email);
                    command.Parameters.AddWithValue("@comment", comment);
```

* Replace lines 419-425 with the following to flows from the “Request” source object on line 55

```
                SqliteCommand command = new SqliteCommand(sql, connection);
                da = new SqliteDataAdapter(sql, connection);
                command.Parameters.AddWithValue("@productCode", productCode);

                sql = "select * from Products where productCode = @productCode";
                da.Fill(ds, "products");

                sql = "select * from Comments where productCode = @productCode";
                da.Fill(ds, "comments");
```

* Add the following on line 9 to import the correct package

```
using System.Data.SqlClient; 
```

* Save the files then commit to the local repo and push to origin using the command palette
    * git:commit
    * Message = added parameterization in DB providers
    * git:push - click ok
* Go to Azure DevOps and Pull Requests on your project page
    * Click Create a Pull Request where it says “You updated fix/issue-## just now”
    * Add a relevant title
    * Select the Work Item # that you just fixed from the drop down.
    * Click Create
* Notice in ADO that some checks haven't been completed yet - Checkmarx Scan
* Notice a new scan in CxSAST with Project name = RepoName-Branch
    * Once the scan finishes, you can see the post in the ADO merge request comments with all the vulnerabilities found
    * Notice that the Product Details vulnerabilities are now gone   
    * Click Complete & uncheck “Delete security_fix after merging” followed by Complete Merge to accept the risk CxSAST has posted about in the comments
* Notice a new scan in CxSAST with Project name = RepoName-Branch for the master branch
* Go back to the ADO Work Items Query page 
* Examine the following issue CX SQL_Injection @ WebGoat/WebGoatCoins/ProductDetails.aspx.cs [master]
* Go to CxSAST and note that CxFlowADO-master project has solved issues and recurrent issues
### <a name="adobatch">Running Batch Mode CxFlow from the CLI to update Work Items </a>
* Open a CMD prompt and enter the following
```
cd C:\CxFlow
java -jar cx-flow-1.6.5.jar --spring.config.location="C:\CxFlow\application.yml" --project --cx-team="CxServer\SP\Company\CxFlowADO" --cx-project="CxFlowADO-master" --namespace="CxFlowADO" --repo-name="CxFlowADO" --branch="master"
```
* **Note** The --cx-teamand --namespace arguments match the organization/namespace in Azure DevOps, like https://dev.azure.com/<organization_or_namespace\>/CxFlowADO
* **Note** If a MachinaException is raised with a “Unable to determine target ADO project” message when executing this command, add the --alt-project parameter with the project’s name at the end, like --alt-project="CxFlowADO"  
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#azure)


## <a name="adopipeline">Azure DevOps Pipeline</a>
* [Windows Agents](#windowsagents)
* [Docker Container](#adopipelinedocker)
* [Configuration](#adopipelineconfiguration)
* [Upgrading to CxSAST v9.0 and Above](#adopipelinenine)
* [Environment Variables](#adopipelineenvironmentvariables)
* [Scripts](#adopipelinescripts)
* [Building](#adopipelinebuilding)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>
This documentation is to help organisations create and run CxFlow in Azure DevOps (ADO) Pipelines.
<br/>The key features of doing this are:

* Utilize CxFlow as a Stage/Task in ADO Pipelines
* Automatically determine matching variables between the Azure Pipeline and Checkmarx
     * Variables can optionally be statically set by the developer team
* Automatically generating work items from the pipeline if required
* Cross platform Azure DevOps Agent support
    * Docker image for cross organisation updating
    * Updating the image will update all projects configurations
* Ability to create custom workflows for pipelines to run via the endpoint script
    * Run multi-stage CxFlow jobs
<br/>Below are examples of Azure DevOps Pipeline YAML files that use CxFlow to scan the code and create [Work Items](https://docs.microsoft.com/en-us/azure/devops/boards/work-items/about-work-items?view=azure-devops&tabs=agile-process) with vulnerabilities. CxFlow is invoked with custom workflow(s) that an organization might require.

### <a name="windowsagents">Windows Agents</a>
##### [Top of Lab](#adopipeline)
This Windows based script is called entrypoint.ps1 which is the Powershell script that allows developers to run a wrapper around CxFlow. This can be distributed to all (security focused) Agents in the environment along with the application.yml and the Java archive of CxFlow.
<br/> **Auto-downloader**
<br/> The Powershell script has the ability to download automatically the current release of CxFlow as a Jar off the GitHub Releases. This feature can be disabled in environments that do not allow out-bound connections to the internet or downloading of binaries.

```
trigger:
- master

pool:
  name: Agents
  vmImage: 'CxAgent'

stages:
- stage: Security
  jobs:
  - job: CxFlow
    steps:
    # This will have to be present on the agent
    - task: PowerShell@2
      inputs:
        # Full or Relative path to Powershell script
        filePath: '.\entrypoint.ps1'
```
### <a name="adopipelinedocker">Docker Containers</a>
##### [Top of Lab](#adopipeline)
The docker container version of CxFlow runs the exact same code as the Linux based Agents do. The only primary difference is that you can create a Docker image (container all the code and configuration) in a single binary which is immutable and can be distributed by using Docker Registries.
```
pool:
  vmImage: 'ubuntu-latest'

stages:
- stage: Security
  jobs:
  - job: CxFlow
    steps:
    # This step might not be needed if Docker is pre installed
    - task: DockerInstaller@0
      inputs:
        dockerVersion: '17.09.0-ce'
    # Run CxFlow
    - bash: docker run -v `pwd`:/code --rm --env-file <(env) organisation/cxflow-azure:latest
      env:
        # Required Settings
        AZURE_ACCESS_TOKEN: $(System.AccessToken)
        CHECKMARX_PASSWORD: $(CHECKMARX_PASSWORD)
```
### <a name="adopipelineconfiguration">Configuration</a>
##### [Top of Lab](#adopipeline)
The application.yml is where most of the static settings are stored that do not change. These can be configured per organisation and nothing sensitive should be stored in this file unless encrypted (encrypt them using [Jasypt](http://www.jasypt.org/)).
```
# ...
checkmarx:
  username: ${CHECKMARX_USER}
  password: ${CHECKMARX_PASSWORD}
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ${CHECKMARX_URI}
  multi-tenant: false
  scan-preset: ${CHECKMARX_PRESET:Checkmarx Default}
  configuration: Default Configuration
  team: ${CHECKMARX_TEAM:\CxServer\SP\Company}
  preserve-xml: true
  incremental: false

azure:
  token: ${AZURE_ACCESS_TOKEN}
  url: ${AZURE_URL}
  api-version: 5.0
  issue-type: issue
  closed-status: Closed
  open-status: Active
  false-positive-label: false-positive
  block-merge: true
```
### <a name="adopipelinenine">When Upgrading to CxSAST v9.0 and above</a>
##### [Top of Lab](#adopipeline)
When updating to CxSAST version 9.0 or above, the REST API changes so CxFlow needs to swap to version 9.0 support and some configuration changes need to be done. This requires the following changes:
<br/>More information can be found on the CxSAST Version 9.0 page
```
# ...
checkmarx:
  version: 9.0  
  client-id: resource_owner_client
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  scope: access_control_api sast_rest_api
  # ...
  team: /CxServer/Checkmarx/CxFlow
```
The Team syntax changes from version 8.9 to 9.0. Originally back-slashes are now forward-slashes.

### <a name="adopipelineenvironmentvariables">Environment Variables</a>
##### [Top of Lab](#adopipeline)
Here is a list the different variables that can be passed into the Docker environment or the endpoint.sh script.

Name | Required? | Description
--------------------|-------------------|----------------------
AZURE_ACCESS_TOKEN | Yes | This is the token that is used to clone the repository and open/edit/close Work Items. You can use the Azure System.AccessToken
AZURE_URL| No | This is the URL to the organisation in Azure. Default is System.TeamFoundationCollectionUri
CHECKMARX_URI | Yes | The URL/URI of where Checkmarx is hosted. This can be built in by default by editing the application.yml.
CHECKMARX_USERNAME | Yes | Username of the Checkmarx user (typically a service scanner account)
CHECKMARX_PASSWORD | Yes | Password for the Checkmarx user. This should be a Azure Pipeline Secrets or encrypted using Jasypt (see CXFLOW_KEY section).
CHECKMARX_PROJECT | No | Project name in Checkmarx. The Default is the Azure Project name (Build.Repository.Name). This can work along side the project-script feature of CxFlow.
CHECKMARX_TEAM | No | Project Team that the project should be under. Default \CxServer\SP\Company. This can be built in by default by editing the application.yml. This can work along side the team-script feature of CxFlow.
CHECKMARX_PRESET | No | Project Preset to use. Default is Checkmarx Default
CXFLOW_KEY | No | This key is used for decryption of the tokens or sensitive data using Jasypt. By Default, the application will not decrpt anything.
CXFLOW_KEY_ALGORITHM | No | Custom algorithm you want to use with Jasypt. The default value is PBEWITHHMACSHA512ANDAES_256.

### <a name="adopipelinescripts">Scripts</a>
##### [Top of Lab](#adopipeline)
These scripts are used on an Azure DevOps Agent as part of a Pipeline. They provide a wrapper around CxFlow to automatically pull out various [built-in Azure Pipeline variables](https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml) to provide a seamless experience for organisations. Many of the variables are dictated based on environment variables passed into the Docker container at run time or the application.yml.
<br/>These can be updated to your requirements and can be different from organisation-to-organisation.
<br/>The entrypoint.sh script is to support both Linux based agents and it’s the entry point for the Docker image.

### <a name="adopipelinebuilding">Building</a>
##### [Top of Lab](#adopipeline)
**Docker Image**
We recommend that organisations create a git repository of these files to track changes and easily deploy the images for all pipelines in the organisation in a private registry. 
<br/>Note: This Docker image can be used for any pipelines as long as the ADO variables being supplied are updated to corresponding build systems/bug tracking systems.
<br/>**Command Line Interface**
<br/> In the working directory of the source code, run the following commands:
```
# Building the Docker image
docker build -t organisation/cxflow .

# Pushing image to registry
docker push private-registry:5000/organisation/cxflow
```
Feel free to change the name of the image to anything but make sure that the pipelines match the container name.

<br/>**Build CxFlow using an Azure Pipeline**
<br/>If you have created a separate repository in Azure DevOps and use this simple pipeline to build and push the Docker image into an internal registry. This allows for organisations to automatically make updates to CxFlow, commit the changes, build the Docker container and push them to a globally accessible directory. 
```
# This Azure Pipeline is for building Docker images using Azure
pool:
  vmImage: 'ubuntu-latest'

variables:
  imageName: 'organisation/cxflow-azure'

steps:
- task: Docker@2
  displayName: Login
  inputs:
    command: login
    containerRegistry: dockerRegistryServiceConnection1
- task: Docker@2
  displayName: Build and Push Image
  inputs:
    repository: $(imageName)
    command: 'buildAndPush'
    Dockerfile: '**/Dockerfile'
```
<br/>Congratulations, you have completed this lab!
<br/>[Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#adopipeline)


## <a name="bitbucket">Bitbucket Cloud Webhook Lab</a>
* [Requirements](#bitbucketrequirements)
* [Prep](#bitbucketprep)
* [Triggering Scans with CxFlow](#bitbuckettriggering)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>

This lab is designed to teach the following topics:
* How to scan on a Merge Request to a Protected Branch
* How to scan on a Push to Protected Branch which opens tickets in JIRA
* How to work with version control & remediate code in Eclipse IDE

### <a name="bitbucketrequirements">Requirements:</a>
##### [Top of Lab](#bitbucket)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.
<br/>**Note** Please note the unique token configuration for bitbucket, ###\<bitbucketuser\>###:###\<bitbuckettoken\>###
```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: Azure
  bug-tracker-impl:
  - Azure
  branches:
  - master
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  #   - Urgent
  #   - Confirmed
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<username>###
  password: ###<password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl

bitbucket:
  webhook-token: 12345
  token: ###<bitbucketuser>###:###<bitbuckettoken>###
  url: https://api.bitbucket.org
  api-path: /2.0
```
* If following this guide for demo purposes, you can use ngrok to generate a resolvable address for your CxSAST manager. This guide includes ngrok in its examples
    * Download ngrok from https://ngrok.com/download and unzip to the CxFlow folder
    * Start ngrok on port 8982 by opening CMD and entering the following command:
<br/> **Note** Ngrok also tunnels traffic on port 443 for SSL.  We will be using this for Azure DevOps webhooks since the endpoint is required to be https.
```
cd C:\CxFlow
ngrok http 8982
```
### <a name="bitbucketprep">Bitbucket Prep</a>
##### [Top of Lab](#bitbucket)
* Login or sign up for an account at https://www.bitbucket.org
* **Note** Use the same email address you used or will use to setup JIRA
* [Connect JIRA and Bitbucket](https://support.atlassian.com/bitbucket-cloud/docs/connect-bitbucket-cloud-to-jira-software-cloud/)
* Create a new private repository named CxFlowBitbucket by clicking the + button on the sidebar
* Click Import repository to import code from your favorite small demo codebase on GitHub
    * This lab will use https://github.com/psiinon/bodgeit 
* Create a token by clicking your profile in lower-left corner & Personal settings
    * Click App Passwords & Create app password
    * Create a Label (i.e. CxFlow)
    * Give the token all Read/Write access to Pull requests & Webhooks
    * Copy this token and keep safe - it should be pasted into the token: <> of the application.yml
    * **Note** The token in the YML file should follow the format <UserName>:<Token>
* Once the .YML file is completely filled out and saved, start CxFlow in webhook mode by opening a CMD prompt and typing the following
```
cd C:\CxFlow
java -jar cx-flow-1.5.4.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```
* In Bitbucket, create a webhook by selecting Repositories & select the new repo you just created 
* Click Repository settings>Webhooks>Add Webhook and fill in details
    * Title = CxFlow
    * URL = ngrok location of cxflow that is running + ?token=webtoken from yml file - example: http://4d91e7ed.ngrok.io?token=12345
* Choose from a full list of triggers = Push, Pull Request Created
* Click Save 

### <a name="bitbuckettriggering">Triggering Scans with CxFLow</a>
##### [Top of Lab](#bitbucket)
* Open your favorite IDE of choice and clone the new repo
* This lab will use Eclipse
* Open Eclipse, click File > Import > Git > Projects from Git (Next) >Clone URI (Next)
    * URI = your repo
    * Authentication User = Bitbucket username 
    * Password = token you created earlier 
    * Select Store in Secure Store
    * Select next until Import as a general project, then click Next followed by Finish
* Right click README.md and open with Generic Text Editor, add the following line & save
```
CxFlowMasterPush-Test1
```
* Stage the Readme.md file, commit to local git repo & push to origin with comments by clicking the following
    * Window \> Show View \> Other… \> Git \> Git Staging
    * Select README.md & click the + button
    * Enter the following into the Commit Message window
    * Cxflow push to a protected branch
    * Click Commit & Push
* You show now see a scan in the CxSAST queue
    * Notice the project name = RepoName-Branch
    * Notice the team of the new project = Bitbucket organization
        * This is due to the team line in the .yml file - it auto-creates a team if it does not exist
        * This can be overridden and changed in the configs
* Open Eclipse and create a new local branch called security-fix
    * Click Window \> Show View \> Other… \> Git Repositories
    * Right click Branches \> Switch To \> New Branch
    * Type "security-fix" and click Finish
* Open basket.jsp underneath the root folder and replace lines 53-55 with the following
```
//Statement stmt = conn.createStatement();
//Security Fix
PreparedStatement preparedStatement = con.prepareStatement(sql);
try {
//ResultSet rs = stmt.executeQuery("SELECT * FROM Baskets WHERE basketid = " + basketId);
String sql = "SELECT * FROM Baskets WHERE basketid =?");
preparedStatement.setString(1, basketId);
ResetSet rs = preparedStatement.executeQuery();
```
* Add the following on line 7 to import the correct package
```
<%@ page import="java.sql.PreparedStatement" %>
```
* Alternatively, you can pull down results using the CxViewer in Eclipse and remediate based on the ID
* Save the file, stage the file, commit to the local repo and push to origin
    * Click Save All
    * Click Git Staging>basket.jsp and then + button then add commit message "added preparedstmt on line 55"
    * Click Commit and Push followed by Next and then Finish
* Go to Bitbucket and Branches on your repository page
    * Click Create in the Pull Request column and on the security-fix row 
    * Click Create Pull Request
* Notice in Bitbucket that a comment has been made - Scan submitted to Checkmarx
* Notice a new scan in CxSAST with Project name = RepoName-Branch
    * Once the scan finishes, you can see the post in the Bitbucket merge request comments with all the vulnerabilities found
    * Notice that the basket.jsp SQLi is gone   
    * Click Merge & uncheck “Close source branch” to accept the risk CxSAST has posted about in the comments
* Notice a new scan in CxSAST with Project name = RepoName-Branch for the master branch
* Go to CxSAST and note that cxflowbitbucket-master project has solved issues and recurrent issues
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#bitbucket)


## <a name="clijira">CxFlow CLI & JIRA Lab</a>
* [Requirements](#clirequirements)
* [Prep](#cliprep)
* [Triggering Scans with CxFlow](#clitriggering)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>

This lab is designed to teach the following topics:
* How to configure a Jira project for CxFlow
* Automated ticket creation using CxFlow CLI
* Scanning via CxFlow CLI

### <a name="clirequirements">Requirements:</a>
##### [Top of Lab](#clijira)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.
```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: JIRA
  # bug-tracker-impl:
  branches:
  - master
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  #   - Urgent
  #   - Confirmed
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<username>###
  password: ###<password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl

jira:
  url: ###<jira url>###
  username: ###<jira user email>###
  token: ###<jira api token>###
  project: APPSEC
  issue-type: Bug
  priorities:
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: In Progress
  close-transition: Done
  open-status:
    - Backlog
    - Selected for Development
    - In Progress
  closed-status:
    - Done
  fields:
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: loc
      jira-field-name: LOC
      jira-field-type: label
      jira-default-value: XXXXX
```

### <a name="cliprep">Jira Prep</a>
##### [Top of Lab](#clijira)
* Sign up for free Atlassian Cloud account at https://www.atlassian.com/try/cloud/signup?bundle=jira-software&edition=free
* **Note** If your company email is already associated with an Atlassian account, to follow this guide:
    * Open an incognito browser window
    * Navigate to https://www.atlassian.com/try/cloud/signup?bundle=jira-software&edition=free
    * Use a personal or other email account to create an account
* During the auto-setup  choose the following options
    * I am experienced with Jira
    * My team is experienced with agile methodologies
    * We spend our time working on fixing bugs
    * We have a flexible schedule to finish our work
* Create a new project & choose a Kanban project
    * Project Name = APPSEC
    * Project Key = APPSEC
    * **Note** The 'Jira Project' field in the .yml file corresponds to this 'Project Key' and is case sensitive
* Create an API token from your Atlassian account: 
    * Log in to https://id.atlassian.com/manage-profile/security/api-tokens
    * Click Create API token.
    * From the dialog that appears, enter ‘CxFlow’ and click Create.
    * Click Copy to clipboard, then paste the token to your script, or elsewhere to save: it should be pasted into the token: <\> of the application.yml
* Create a custom field for this project and issue type screen by clicking the settings wheel in the top right  corner
    * Click Issues \> Custom Fields \> Create Custom Field
    * Click Labels and give it a name “Application”
    * Description = CxSAST Project
    * Select the checkboxes next to APPSEC: Kanban Bug Screen & APPSEC: Kanban Default Issue Screen
    * Click Update
* Create another custom field for Category
    * Name = Category
    * Description = CxSAST Vulnerability Type
    * Select the checkboxes next to APPSEC: Kanban Bug Screen & APPSEC: Kanban Default Issue Screen
    * Click Update

### <a name="clitriggering">Triggering Scans with CxFLow</a>
##### [Top of Lab](#clijira)
* After the .YML file is completely filled out and saved
* The following command clones a github repo, creates a CxSAST scan for the cloned repo, and creates tickets according to the .yml file
```
cd C:\CxFlow
git clone https://github.com/ethicalhack3r/DVWA.git 
cd C:\CxFlow
java -jar cx-flow-1.6.5.jar --spring.config.location="C:\CxFlow\application.yml" --scan --f="./DVWA" --cx-team="CxServer\SP\Company" --cx-project="DVWA" --app="DVWA"
```
* After the .YML file is completely filled out and saved
* **Note** The url for the jira section of the .yml file should be the one assigned to you when you first start your Jira account, for example 
```
url: https://<username>.atlassian.net/
```
* The following command opens tickets for a CxSAST project’s last finished scan according to the .yml file

```
cd C:\CxFlow
java -jar cx-flow-1.6.5.jar --spring.config.location="C:\CxFlow\application-jira.yml" --project --cx-team="CxServer\SP\Company" --cx-project="DVWA" --app="DVWA"
```
* Open the APPSEC project in Jira and note the vulnerabilities that have been opened

### Bonus
* You can kick off batch mode ticket creation in any Linux pipeline by supplying the application.yml file and using the following code to download CxFlow and run
* **Note** Replace cx-project and app flags with environment variables relevant to the pipeline
```
apk add --update curl
curl -O -k https://github.com/checkmarx-ltd/cx-flow/releases/download/1.6.5/cx-flow-1.6.5.jar
java -jar cx-flow-1.6.5.jar --spring.config.location="./application.yml" --scan --f=. --cx-team="CxServer" --cx-project="Bodgeit" --app="Bodgeit"
```
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)

## <a name="batch">CxFlow Batch Mode Lab</a>
* [Requirements](#batchrequirements)
* [SMTP Server Prep](#smtpserverprep)
* [Triggering Scans with CxFlow](#batchtriggering)
* [EmailPNEVulns.ps1](#emailpne)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>

This lab is designed to teach the following topics:
* Run CxFlow in batch mode with XML results output
* Automate email notifications on Proposed Not Exploitable Vulnerabilities using the [EmailPNEVulns.ps1](#emailpne) script below

### <a name="batchrequirements">Requirements:</a>
##### [Top of Lab](#batch)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* Create a new file called EmailPNEVulns.ps1 in C:\Flow with the text at the bottom of the page and replace any values surrounded in ###<\>### with your appropriate values, see [SMTP Server Prep](#smtpserverprep) steps below
* In the same folder create a file titled application-email.yml
* Add the text below to the application-email.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.
```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: CxXml
  bug-tracker-impl:
  - CxXml
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
     - Proposed Not Exploitable

checkmarx:
  username: ###<cxsast username>###
  password: ###<cxsast password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl
  preserve-xml: true
  
cx-xml:
  file-name-format: "xmlresults.xml"
  data-folder: "./"
```

### <a name="smtpserverprep">SMTP Server Prep</a>
##### [Top of Lab](#batch)
* Open a google chrome browser window & signup for SendGrid using a personal or fake email
    * https://signup.sendgrid.com/
    * **Note** After April 6, 2020, Single Sender Verification may be required, see:
    *  https://sendgrid.com/docs/ui/sending-email/sender-verification#adding-a-sender
* Confirm your email address 
* Click Email API \> Integration Guide on the left sidebar
    * Choose SMTP Relay
    * My First API Key Name - CxSAST
    * Click Create Key and add to the application-email.yml file
* Scan the following GitHub project using source control scan in CxSAST under the following team
    * URL = https://github.com/ethicalhack3r/DVWA.git  
    * Team = CxServer\SP\Company
    * Project Name = DVWA
* After the scan completes, open the CxViewer & mark all SQL_Injection as Proposed Not Exploitable

### <a name="batchtriggering">Triggering Scans with CxFLow</a>
##### [Top of Lab](#batch)
* After the .YML file and .PS1 file are completely filled out and saved
* Run CxFlow in batch mode & the email PowerShell script by opening a Powershell prompt and typing the following
```
cd C:\CxFlow
java -jar cx-flow-1.6.5.jar --spring.config.location="C:\CxFlow\application-email.yml" --project --cx-team="CxServer\SP\Company" --cx-project="DVWA" --app="DVWA"
.\EmailPNEVulns.ps1 -results_xml .\xmlresults.xml -email <youremail>
```
* Open your email & verify that the Proposed Not Exploitable results have been emailed.
* The email might be in your junk folder.

### Bonus
You can use Windows Task Scheduler to call the above commands/scripts & run this every evening.

### <a name="emailpne">EmailPNEVulns.ps1</a>
##### [Top of Lab](#batch)
```

# Usage: EmailPNEVulns.ps1 -results_xml <path-to-xml-results> -email_to <comma-separated-email-addresses>
param (
    [Parameter(Mandatory = $true)][string]$results_xml,
    [Parameter(Mandatory = $true)][string]$email_to
)

#=======================================
# **************************************
#
# Modify SMTP Settings below before use
#
# **************************************
#=======================================

[string] $smtpServer = "smtp.sendgrid.net"
[int] $smtpPort = 587
[string] $smtpUser = "apikey"
[string] $smtpPass = <###your send grid API key###>
[string] $smtpFrom = "cxflow@cx.com"

#=======================================



[string[]] $recipients = $email_to -split ","

#Parse XML file into an object
try {
    [xml]$results = get-content $results_xml
}
catch {
    Write-Output "Error parsing " + $results_xml
    Write-Output "Exception: $($_.Exception.Message)"
}

[string]$project_name = $results.CxXMLResults.ProjectName
[string]$message = "<font size='4'>Checkmarx CxSAST found New Vulnerabilities in Project : <b>" + $project_name + "</b>"
$message += "<br>Detailed results are available at : <a href='" + $results.CxXMLResults.DeepLink + "'>" + $results.CxXMLResults.DeepLink + "</a>"
$message += "<br><b>New vulnerabilities found : " + $results.CxXMLResults.ScanStart + "</b></font><br>"
Write-Output $message

#Find all NEW vulnerabilities
[Object []]$Vulns = $results.CxXMLResults.Query.Result | Where-Object {$_.state -eq "4"}

$nv = "Found " + $Vulns.Count + " vulnerabilities"
Write-Output $nv

# NOP if no vulnerabilities match above filter
if ($Vulns.Count -eq 0) {
	exit
}

[int]$issue_count = 0 

#For each vulnerability, update the message
Foreach ($Vuln in $Vulns) {
    $message += "<br>" + "<b>New [" + $Vuln.Severity + "] Severity [" + $Vuln.ParentNode.name + "] vulnerability</b>"

    #For data flows with more than one node, provide filename, line number, and code snippet for source and sink 
    #Otherwise, provide the filename, line number, and code snippet of the single node
    [Object]$origin = $Vuln.Path.FirstChild
    if ($Vuln.Path.ChildNodes.Count -gt 1) {
        $originCode = $origin.Snippet.Line.Code -replace '\t', ' '
        $destinationCode = $destination.Snippet.Line.Code -replace '\t', ' '
        $message += "<br>Source: [" + $origin.FileName + " : " + $origin.Line + "]. Code: <i>" + $originCode + "</i>"
        [Object]$destination = $Vuln.Path.LastChild
        $message += "<br>Sink: [" + $destination.FileName + " : " + $destination.Line + "]. Code: <i>" + $destinationCode + "</i>"
    }
    elseif ($Vuln.Path.ChildNodes.Count -eq 1) {
        $originCode = $origin.Snippet.Line.Code -replace '\t', '  '
        $message += "<br>File [" + $origin.FileName + " : " + $origin.Line + "]. Code: <i>" + $originCode + "</i>"
    }

    #Deep link to the vulnerability information in the portal
    $message += "<br>View result on CxSAST : <a href='" + $Vuln.DeepLink + "'>" + $Vuln.DeepLink + "</a><br>"

    #Escape characters
    #$message = $message.Replace("\", "\\").Replace("`"", "\`"")
    Write-Debug $message

    $issue_count++

} 

$message += "<br><b>Total New Vulnerabilities : " + $issue_count + "</b>"

$subject = "Checkmarx found $issue_count NEW vulnerabilities in [$project_name]"

Write-Output "Sending notifications to $recipients"

# Notify email list
try {
    $secP = ConvertTo-SecureString $smtpPass -AsPlainText -Force
	$smtpCreds = New-Object System.Management.Automation.PSCredential $smtpUser,$secP
	
	$smtpMessage = @{
		SmtpServer = $smtpServer
		Port = $smtpPort
		UseSsl = $true
		Credential  = $smtpCreds
		From = $smtpFrom
		To = $recipients
		Subject = $subject
		Body = $message
	}

	Send-MailMessage @smtpMessage -BodyAsHtml
	
    $message = "Found and notified [$email_to] about " + $issue_count + " NEW vulnerabilities."
    Remove-Item -path .\xmlresults.xml
    Write-Output $message
}
catch {
    $exception = $_.Exception

    Write-Output "Failed to send out notification emails."
    Write-Output "Reason: $($exception.Message)"
}

```
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#batch)


## <a name="githuboverrides">GitHub Overrides & JIRA  Lab</a>
### **Note** Both the [GitHub Webhook Lab](#github) and the [CxFlow CLI & JIRA Lab](#clijira) should be completed before this lab.
* [Requirements](#overridesrequirements)
* [Prep](#overridesprep)
* [Triggering Scans with CxFlow](#overridestriggering)
* [Back to Labs Table of Contents](#tableofcontents)
<br/>

This lab is designed to teach the following topics:
* Automated ticket creation for JIRA using CxFlow in Webhook mode
* Organization level webhooks
* Webhook overrides
* Branching & scanning best practices

### <a name="overridesrequirements">Requirements</a>
##### [Top of Lab](#githuboverrides)
**Note** Prerequisites include completing the following two labs:
    * [GitHub WebHook Lab](#github)
    * [CxFlow CLI & JIRA Lab](#clijira)
* The following steps will have been completed in the above labs; however, if any do not exist:
    * Create a folder on the C:\ drive called CxFlow
    * Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
    * In the folder create a file titled application.yml
    * **Note** If a previously created application.yml file can be used, ensure that the bugtracker under the cxflow heading is set to JIRA, for example:
```
  bug-tracker: JIRA
```
* Alternatively, add the text below to a newly created application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.
```
server:
  port: 8982
logging:
  file: flow.log

cxflow:
  bug-tracker: JIRA
  #bug-tracker-impl:
  branches:
  - master
  filter-severity:
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  #   - Urgent
  #   - Confirmed
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<username>###
  password: ###<password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: ###<CxSAST url or http://localhost>###
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl


github:
  webhook-token: 12345
  token: ###<githubtoken>###
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true

jira:
  url: ###<jira url>###
  username: ###<jira user email>###
  token: ###<jira api token>###
  project: APPSEC
  issue-type: Bug
  priorities:
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: In Progress
  close-transition: Done
  open-status:
    - Backlog
    - Selected for Development
    - In Progress
  closed-status:
    - Done
  fields:
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: loc
      jira-field-name: LOC
      jira-field-type: label
      jira-default-value: XXXXX
```
* If following this guide for demo purposes, you can use ngrok to generate a resolvable address for your CxSAST manager. This guide includes ngrok in its examples
    * Download ngrok from https://ngrok.com/download and unzip to the CxFlow folder
    * Start ngrok on port 8982 by opening CMD and entering the following command:
```
cd C:\CxFlow
ngrok http 8982
```

### <a name="overridesprep">GitHub Override Prep</a>
##### [Top of Lab](#githuboverrides)
* Login to [GitHub](https://github.com/)
* Create a new organization called <yourname\>-cxflowgithub
* Click create New repository \> Import and import your favorite small repository
    * This lab will use https://github.com/CSPF-Founder/JavaVulnerableLab
    * Owner = <yourname\>-cxflowgithub
    * Name = github2jira
    * Privacy = Private
    * **Note** Privacy settings need to match between repo and WebHook. If you want to resuse your existing WebHook, set Privacy = Public
* Create an organization level webhook by selecting your organization -  <yourname\>-cxflowgithub
    * Click Settings \> Webhooks \> Add Webhook and fill in details
    * Payload URL = ngrok location of cxflow that is running - example: http://4d91e7ed.ngrok.io
    * Content type = application/json
    * Secret = webhook-token: from .yml file - example: 12345
    * Let me select events -   Push
    * Click Add Webhook - there should be a checkmark next to the hook name now
* Add a second webhook with an override for pull requests
    * Payload URL = ngrok location of cxflow that is running + override
    * Example: http://4d91e7ed.ngrok.io?project=repo-pr
    * Content type = application/json
    * Secret = webhook-token: from .yml file - example: 12345
    * Let me select events -   Pull requests
    * Optionally, select Branch or tag deletion events to delete the project in Checkmarx after merging the pull request 
    * Click Add Webhook - there should be a checkmark next to the hook name now

### <a name="overridestriggering">Triggering Scans with CxFLow</a>
##### [Top of Lab](#githuboverrides)
* After the .yml file is completely filled out and saved
* Start CxFlow in webhook mode by opening a CMD prompt and typing the following
```
cd C:\CxFlow
java -jar cx-flow-1.6.4.jar --spring.config.location="C:\CxFlow\application.yml" --web
```
* Open your favorite IDE of choice and clone the new repo
    * This lab will use Intellij
* Open IntelliJ, click Check out from Version Control and input your repo's URL, select next until done
* Create a new local branch called develop
    * Click VCS \> Git \> Branches \> New Branch
    * Type "develop" and click ok
    * Open README.md and add the following line & save
    * develop-push-Test1
* Commit to local git repo & push to origin with comments by clicking the following
    * VCS \> Git \> Commit File 
    * Enter the following commit message
    * Cxflow push to a protected branch
    * Click Commit & Push
    * Click Push and enter Github credentials on popup
        * Username = username
        * Password = token created
* You should now see a scan in the CxSAST queue
* Notice the project name = RepoName-Branch
* Notice the team of the new project = GitHub organization
    * This is due to the team line in the .yml file - it auto-creates a team if it does not exist
    * This can be overridden and changed in the configs
* Once the scan finishes, open the APPSEC project in Jira and use the search ability to look for the SQL Injection in the LoginValidator.java page of the [develop] branch
* We will now triggering CxFlow from a Pull Request to a protected branch (master)
* Create a new local branch called fix/APPSEC-## where ## is the ticket created in JIRA
    * Click VCS \> Git \> Branches \> New Branch
    * Type "fix/APPSEC-##" and click ok
* Replace with the following code block starting at line 50-52 of JavaVulnerableLab\src\main\java\org\cysecurity\cspf\jvl\controller\LoginValidator.java
```java
//Statement stmt = con.createStatement();
//Sanitized User and Password
String sql = "select * from users where username=? and password=?"; 
PreparedStatement preparedStatement = con.prepareStatement(sql);
// rs=stmt.executeQuery("select * from users where username='"user"' and password='"pass"'"
preparedStatement.setString(1,user);
preparedStatement.setString(2,pass);
ResultSet rs = preparedStatement.executeQuery(); 
```
* Add the following line on line 21 of the same file
```
import java.sql.PreparedStatement;
```
* Save the file, commit to the local repo and push to origin
    * Click File \> Save All
    * Click VCS \> Git \> Commit File and add commit message "added preparedstmt on line 50"
    * Click Commit and Push followed by Push
    * Ignore any warnings
* Notice that this branch did not meet the criteria for scanning In the cxflow logs
* Open Github and click Compare & Pull Request for the fix/APPSEC-## branch
    * Select develop for the branch to merge into 
    * Click Create Pull Request
* Notice that this branch will scan into a common project ending in -pr.
* Now, let's merge to develop and follow our defined branching strategy by creating a pull request  to master
* Notice that the merge performed a scan because of the push to develop and that APPSEC-## is DONE in JIRA
* Note how all the scans are under two projects in CxSAST
    * The LoginValidtor.java SQLi is gone from the comments 
    * Click Merge Pull Request \> Confirm Merge to accept the risk CxSAST has posted about in the comments
    * **Optional** Click Delete Branch button to delete the fix/APPSEC-## branch.  If you selected the Branch or tag deletion event when creating the webhook, the -pr project will be deleted in CxSAST.
* Congratulations, you have completed this lab!
* [Back to Labs Table of Contents](#tableofcontents)
##### [Top of Lab](#githuboverrides)