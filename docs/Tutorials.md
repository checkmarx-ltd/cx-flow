## <a name="tableofcontents">Table of Contents</a>
* [Requirements For Tutorials](#requirementsfortutorials)
* [Quick Start](#quickstart)
* [Triggering Webhook Scans with CxFlow](#webhooktriggering)
* [GitHub Webhook Tutorial](#github)
    * [Prep](#githubprep)
* [GitLab Webhook Tutorial](#gitlabWebhook)
    * [Prep](#gitlabprep)
* [GitLab CI/CD](#gitlabcicd)
    * [Requirements](#gitlabcicdrequirements)
    * [CI/CD Variables](#gitlabcicdvaiables)
    * [Pipeline Configuration](#gitlabpipelineconfiguration)
    * [Run Pipeline and review the results](#gitlabrunpipeline)
* [Azure DevOps Webhook Tutorial](#azure)
    * [Prep](#adoprep)
* [Azure DevOps Pipeline](#adopipeline)
    * [Windows Agents](#windowsagents)
    * [Docker Container](#adopipelinedocker)
    * [Configuration](#adopipelineconfiguration)
    * [Upgrading to CxSAST v9.0 and Above](#adopipelinenine)
    * [Environment Variables](#adopipelineenvironmentvariables)
    * [Scripts](#adopipelinescripts)
    * [Building](#adopipelinebuilding)
    * [Wget Script](#adopipelinewget)
* [Bitbucket Cloud Webhook Tutorial](#bitbucket)
    * [Prep](#bitbucketprep)
* [CxFlow CLI & JIRA Tutorial](#clijira)
    * [Prep](#cliprep)
    * [Triggering CLI Scans with CxFlow](#clitriggering)
* [CxFlow Batch Mode Tutorial](#batch)
    * [Requirements](#batchrequirements)
    * [SMTP Server Prep](#smtpserverprep)
    * [Triggering Scans with CxFlow](#batchtriggering)
    * [EmailPNEVulns.ps1](#emailpne)
* [GitHub Actions with JIRA Integration](#actions)
* [CxFlow IAST Integration Tutorial](#IASTintegrations)
    * [Prerequisites](#IASTprerequisites)
    * [General Procedure](#IASTgeneralprocedures)
    * [Sample Jenkins Pipeline](#IASTJenkinsPipeline)
    * [Yaml - application.yml file](#IASTYaml)
<br/>


## <a name="requirementsfortutorials">Requirements For Tutorials:</a> 
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.6.12, if you download another version, input your version in the command below
* Download the appropriate version of the example application.yml file for your CxSAST version from https://github.com/checkmarx-ts/cicd-examples/tree/master/cxflow/YML-templates

<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Source Control heading please enter your token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.  **Note** Each lesson will walk through creating a personal access token in the source control.

* Once the .yml is completely filled out including the personal access token, start CxFlow in webhook mode by opening CMD prompt/shell, navigate to your CxFlow directory (created above) and entering the following, after updating the path\to\CxFlow folder:
```
cd C:\CxFlow
java -jar cx-flow-1.6.19.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```

<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.

* If following this guide for demo purposes, you can use ngrok to generate a resolvable address for your CxSAST manager. This guide includes ngrok in its examples
    * Download ngrok from https://ngrok.com/download and unzip to the CxFlow folder
    * Start ngrok on port 8982 by opening CMD and entering the following command:
```
cd C:\CxFlow
ngrok http 8982
```
## <a name="quickstart">Quick Start</a>
This quick start guide describes how to trigger a CxSAST scan on a Pull Request and a Push to a protected GitHub branch. Pushes to a protected branch will create GitHub Issues from the scan results.

Requirements:
* Create a folder called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br>https://github.com/checkmarx-ltd/cx-flow/releases
<br>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the GitHub heading please enter your GitHub token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.

```
server:
  port: 8982
logging:
  file:
    name: flow.log

cxflow:
  bug-tracker: GitHub
  bug-tracker-impl:
    - GitHub
  branches:
    - main
  filter-severity:
  filter-category:
    - SQL_Injection
    - Stored_XSS
    - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
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
ngrok http 8982
```
* Create an account at www.github.com
* Create a public repository titled CxFlowGitHub
* Import code from your favorite small demo codebase on github. This guide will use <br>https://github.com/psiinon/bodgeit
* Create a token by clicking on your profile in upper right corner > settings
    * Click Developer settings > Personal Access Tokens > Generate New Token
    * Give the token a name, for example cxFlow-minimal, and both repo:status and public_repo scopes. 
    * Ensure "Issues" are enabled on the project Settings > Options > Features > Issues
[[/Images/guide1.png|Example name and scope for GitHub token]]
    * Copy this token and keep it safe. It should be posted into the token \<\> of the application.yml
* Once the .yml is completely filled out, start CxFlow in webhook mode by opening CMD prompt/shell, navigate to your CxFlow directory (created above) and entering the following, after updating the path\to\CxFlow folder:
```
java -jar cx-flow-1.6.19.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```
* Create a webhook by selecting your profile and selecting the repo you just created
    * Navigate to Settings > Webhooks > Add Webhook and fill in the details
        * Payload URL: ngrok example: http://xxxxx.ngrok.io
        * Content type:  application/json
        * Secret: Webhook token from .yml file, in this example 12345
        * Select Events:  Pull Requests, Pushes
    * Click Add Webhook, there should be a checkmarx next to the hook name now
* Open your IDE of choice. This demo will use IntelliJ
    * Check out code using Check out from Version Control, input the URL for your repo example:
<br>[https://github.com/<username\>/CxFlowGitHub](http://github.com)
    * Open README.md and add a line, example: CxFlowMasterPush-Test1
    * Commit to local git repo and push to origin with comments by clicking the following: VCS > Git > Commit File enter a message like CxFlow push to a protected branch
    * Click commit and push
    * Click Push and enter GitHub credentials on popup. Username is your username, password is the token you created above.
* Navigate to the Checkmarx web portal. You should see a new scan in the CxSAST queue
<br>Notice the project name is the RepoName-Branch
<br>Notice the team is the GitHub organization. This is set by the team line in the .yml file. It auto creates a team if it does not exist. This can be overridden in the config file with the multi-tenant setting. Please see the CxFlow configuration page for more information.
* When the scan finishes, you should see issues on the Issue tab of your GitHub repo 
<br>[https://github.com/<username\>/CxFlowGitHub/issues](http://github.com/)
* Examine the following issue CX SQL_Injection @ roost/basket.jsp [main]
* Open the Checkmarx link and examine the finding
* We will now trigger CxFlow from a Pull Request to a protected branch, from branch security-fix to main
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
* Navigate to GitHub
    * Navigate to Pull Requests
    * Click Compare and Pull Request > Create Pull Request
    * Alternatively you can create the pull request through the IDE. In IntelliJ click VCS > Create Pull Request
* In GitHub there will be some checks that have not finished yet - Checkmarx Scan
* In the Checkmarx web portal there will be a new CxSAST scan where the project name is RepoName-Branch
    * Once the scan finished you can see the post in the GitHub merge pull request comments with all the vulnerabilities found
    * The basket.jsp SQLi is gone
    * Click Merge Pull Request > Confirm Merge to accept the risk CxSAST has posted in the comments
* After confirming the pull request, there will be a new CxSAST scan in the Checkmarx web portal for the main branch
* In GitHub Issues there will be one fewer vulnerability
* In the Checkmarx web portal, the CxFlowGitHub-main project will now have both solved and recurrent issues.

## <a name="webhooktriggering">Triggering Webhook Scans with CxFlow</a>
[Back to Table of Contents](#tableofcontents)
* Open your IDE of choice. This tutorial will use IntelliJ
    * Check out code using Check out from Version Control, input the URL for your repo example:
<br/>https://github.com/<username\>/CxFlowBodgeit
    * Open README.md and add a line, example: CxFlowMasterPush-Test1
    * Commit to local git repo and push to origin with comments by clicking the following: VCS > Git > Commit File enter a message like CxFlow push to a protected branch
    * Click commit and push
    * Click Push and enter the source control credentials on popup. Username is your username, password is the personal access token you created.
* Navigate to the Checkmarx web portal. You should see a new scan in the CxSAST queue
<br/>**Notice** the project name is the RepoName-Branch
<br/>**Notice** the team is the organization. This is set by the team line in the .yml file. It auto creates a team if it does not exist. This can be overridden in the config file with the multi-tenant setting. Please see the CxFlow configuration page for more information.
* When the scan finishes, you should see issues on the Issue tab of your repo 
<br/>[https://github.com/<username\>/CxFlowBodgeit/issues](http://github.com/)
* Examine the following issue CX SQL_Injection @ root/basket.jsp [main]
* Open the Checkmarx link and examine the finding
* We will now trigger CxFlow from a Pull Request to a protected branch, from the branch security-fix to main
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
* Navigate to the source control in your browser
    * Navigate to Pull Requests
    * Click Compare and Pull Request > Create Pull Request
    * Alternatively you can create the pull request through the IDE. In IntelliJ click VCS > Create Pull Request
* In the pull request there will be some checks that have not finished yet - Checkmarx Scan
* In the Checkmarx web portal there will be a new CxSAST scan where the project name is RepoName-Branch
    * Once the scan is finished you can see the post in the pull request comments with all the vulnerabilities found
    * The basket.jsp SQLi is gone
    * Click Merge Pull Request > Confirm Merge to accept the risk CxSAST has posted in the comments
* After confirming the pull request, there will be a new CxSAST scan in the Checkmarx web portal for the master branch
* In Issues section of the source control there will be one fewer vulnerability
* In the Checkmarx web portal, the CxFlowBodgeit-main project will now have both solved and recurrent issues. 

## <a name="github">GitHub Webhook Tutorial</a>
* [Prep](#githubprep)
* [Triggering Webhook Scans with CxFlow](#webhooktriggering)

[Back to Table of Contents](#tableofcontents)
<br/>

This tutorial is designed to teach the following topics:

* How to scan on a Pull Request to a Protected Branch
* How to scan on a Push to Protected Branch
* GitHub Issue Creation on Push to Protected Branch

### <a name="githubprep">GitHub Prep</a>
* Update the bugtracker section of the application.yml file with the following
```yaml
bug-tracker: GitHub
  bug-tracker-impl:
  - GitHub
```
* Create an account at www.github.com
* Create a public repository titled CxFlowBodgeit
* Import code from your favorite small demo codebase on github. This guide will use <br>https://github.com/psiinon/bodgeit
* Create a token by clicking on your profile in upper right corner > settings
    * Click Developer settings > Personal Access Tokens > Generate New Token
    * Give the token a name, for example cxFlow-minimal, and both repo:status and public_repo scopes. 
    * Ensure "Issues" are enabled on the project Settings > Options > Features > Issues
[[/Images/guide1.png|Example name and scope for GitHub token]]
    * Copy this token and keep it safe. It should be posted into the token \<\> of the application.yml
    * After .YML file is completely filled out and saved, start CxFlow in webhook mode by opening a CMD prompt

* Create a webhook by selecting your profile and selecting the repo you just created
    * Navigate to Settings > Webhooks > Add Webhook and fill in the details
        * Payload URL: ngrok example: https://xxxx.ngrok.io
        * Content type:  application/json
        * Secret: Webhook token from .yml file, in this example 12345
        * Select Events:  Pull Requests, Pushes, Branch or tag deletion
    * Click Add Webhook, there should be a checkmarx next to the hook name now
* Continue to [Triggering Webhook Scans with CxFlow](#webhooktriggering)

## <a name="gitlabWebhook">GitLab Webhook Tutorial</a>
* [Prep](#gitlabprep)
* [Triggering Webhook Scans with CxFlow](#webhooktriggering)

[Back to Table of Contents](#tableofcontents)
<br/>

This tutorial is designed to teach the following topics:

* How to scan on a Merge Request to a Protected Branch
* How to scan on a Push to Protected Branch
* GitLab Issue Creation on a Push to Protected Branch
        
### <a name="gitlabprep">GitLab Prep</a>
##### [Top of Tutorial](#gitlabWebhook)
* Update the bugtracker section of the application.yml file with the following
```yaml
bug-tracker: GitLab
  bug-tracker-impl:
  - GitLab
```
* Create an account at http://www.gitlab.com
* Create a new private group called <yourname\>-checkmarx
* Create a new subgroup called GitLab CxFlow
* Create a new private project called CxFlowGitLab
    * Import code from your favorite small demo codebase
    * This tutorial will use https://github.com/psiion/bodgeit
* Click Import Project \> Repo By URL
    * Git Repository URL https://github.com/psiion/bodgeit
    * Project Name = CxFlowGitLab
    * Ensure the project's status is set to private and click Create Project
* Create a token by clicking your profile in upper right corner >settings
    * Click Access Tokens & add a personal access token
    * Give the token api, read_user, write_repository, read_registry scopes
    * Copy this token and keep safe - it should be pasted into the token: <> of the application.yml
* After .YML file is completely filled out and saved, start CxFlow in webhook mode by opening a CMD prompt and typing the following

* Create a webhook by selecting Projects>Your Projects and select the repo you just created 
* Click Settings>Webhooks and fill in details
    * URL = ngrok location of CxFlow that is running - example: https://xxxx.ngrok.io
    * Secret = webhook-token: from .yml file - example: 12345
    * Trigger = Push events, Merge request events
    * Click Add Webhook
* Continue to [Triggering Webhook Scans with CxFlow](#webhooktriggering) 

## <a name="gitlabcicd">GitLab CI/CD</a>
  * [Requirements](#gitlabcicdrequirements)
  * [CI/CD Variables](#gitlabcicdvaiables)
  * [Pipeline Configuration](#gitlabpipelineconfiguration)
  * [Run Pipeline and review the results](#gitlabrunpipeline)

[Back to Tutorials Table of Contents](#tableofcontents)

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
 
##### [Top of Tutorial](#gitlabcicd)
### <a name="gitlabcicdrequirements">Requirements</a>
GitLab can access a running Checkmarx CxSAST Server with an up-to-date Checkmarx license
If performing CxSCA scans, you must have a valid CxSCA license and GitLab must be able to access the CxSCA tenant
To review scan results within GitLab’s Security Dashboard, you need the Gold/Ultimate tier or the GitLab project must be public
  * To review results in the issue management of your choice (i.e. JIRA) configuration is needed in the CxFlow YAML file, please refer to [Bug Tracker documentation](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels)

##### [Top of Tutorial](#gitlabcicd)
### <a name="gitlabcicdvaiables">CI/CD Variables</a>
To allow for easy configuration, it is necessary to create environment variables with GitLab to run the integration.  For more information on GitLab CI/CD variables, visit here: [GitLab: CI/CD - Environment Variables](https://gitlab.com/help/ci/variables/README#gitlab-cicd-environment-variables)
Edit the CI/CD variables under Settings → CI / CD → Variables and add the following variables for a CxSAST and/or CxSCA scan:

[[/Images/gitlab_settings.png]]

Variable/ Inputs     | Value 
--------------------|-------------------
GITLAB_TOKEN | <p>API token to create Merge Request Overview entries, should have “api” privileges. <br>To create a personal token, click your GitLab profile in the upper right corner >settings <br><br>- Click Access Tokens and add a personal access token.Click Access Tokens and add a personal access token. <br>- Give the token api, read_user, write_repository, read_registry scopes. <br><br> For additional information on creating a Personal Access Token, refer to [GitLab: Personal Access Tokens](https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html) </p> 
CX_FLOW_BUG_TRACKER   (Type: Variable)| Type of bug tracking ('GitLabDashboard' or ‘GitLab’).  For vulnerabilities to be exported to GitLab’s Dashboard, use ‘GitLabDashboard’ and for vulnerabilities to be added to GitLab’s Issues, use ‘GitLab’  For more details on complete list of Bug Trackers, please refer to [CxFlow Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration) 
CX_FLOW_ENABLED_VULNERABILITY_SCANNERS | Vulnerability Scanners (sast, sca, ast, cxgo). Multiple comma separated values allowed.
CHECKMARX_PASSWORD   (Type: Variable) | Password for CxSAST 
CHECKMARX_SERVER   (Type: Variable) | The base URL of CxSAST Manager Server (i.e. https://checkmarx.company.com) 
CHECKMARX_USERNAME   (Type: Variable) | User Name for the CxSAST Manager.  User must have ‘SAST Scanner’ privileges.  For more information on CxSAST roles, please refer to [CxSAST / CxOSA Roles and Permissions](https://checkmarx.atlassian.net/wiki/spaces/KC/pages/1178009601/CxSAST+CxOSA+Roles+and+Permissions+v9.0.0+and+up) 
CHECKMARX_TEAM   (Type: Variable) | Checkmarx Team Name (i.e. /CxServer/teamname) 
CHECKMARX_CLIENT_SECRET | Checkmarx OIDC Client Secret
SCA_TENANT   (Type: Variable) | The name of the CxSCA Account (i.e. SCA-CompanyName).  **Only needed if you have a valid license for CxSCA** 
SCA_USERNAME   (Type: Variable) | The username of the CxSCA Account.  **Only needed if you have a valid license for CxSCA**  
SCA_PASSWORD   (Type: Variable) | The password of the CxSCA Account.  **Only needed if you have a valid license for CxSCA** 
CXGO_CLIENT_SECRET | Client-Secret needed for AST Cloud (CxGo).
AST_API_URL | API URL for AST scan
AST_WEBAPPURL | WebApp URL for AST scan
AST_CLIENT_ID | Client-ID configured within AST. 
AST_CLIENT_SECRET | Client-Secret within AST.
PARAMS | Any additional parameters for CxFlow. For a full list of all the parameters, check [here](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration)

###### <a name="configfile">GitLab Configuration File example</a>

The gitlab configuration file is stored at a remote location within the cxflow repo. It can be directly used as a template using the following syntax.

```yaml
include: 'https://raw.githubusercontent.com/checkmarx-ltd/cx-flow/master/templates/gitlab/v1/Checkmarx.gitlab-ci.yml'

```

##### [Top of Tutorial](#gitlabcicd)
### <a name="gitlabpipelineconfiguration">Pipeline Configuration</a>
The GitLab CI/CD pipeline is controlled by a file named ‘.gitlab-ci.yml’ located in the root directory of the project.  Please refer to [GitLab: CI YAML](https://docs.gitlab.com/ee/ci/yaml/README.html) for more info.

`! It is suggested not to over-pollute your companies already existing '.gitlab-ci.yml' file.  Instead, create a new YAML file in the root directory named ‘.checkmarx.yml’ and include it in ‘.gitlab-ci.yml’`

` # Note that image is a docker container maintained by Checkmarx`
#### .checkmarx.yml (For SAST Scan)
```yaml
include: 'https://raw.githubusercontent.com/checkmarx-ltd/cx-flow/master/templates/gitlab/v1/Checkmarx.gitlab-ci.yml'

variables:
    CX_FLOW_ENABLED_VULNERABILITY_SCANNERS: sast
    CX_TEAM: "/CxServer/MP"
    CHECKMARX_USERNAME: $CX_USERNAME
    CHECKMARX_PASSWORD: $CX_PASSWORD
    CHECKMARX_BASE_URL: $CHECKMARX_SERVER
    CHECKMARX_CLIENT_SECRET: $CHECKMARX_CLIENT_SECRET
  
stages:
  - scan

```

#### .gitlab-ci.yml
```yaml
include: '.checkmarx.yml'

stages:
  - scan

```

##### [Top of Tutorial](#gitlabcicd)
### <a name="gitlabrunpipeline">Run Pipeline and review the results</a>
####Run pipeline
To run a Checkmarx scan, you need to trigger the pipeline.  The trigger is based on the .gitlab-ci.yml and in the provided sample above, it will be triggered on Merge Requests and on changes to the main branch
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
##### [Top of Tutorial](#gitlabcicd)

## Sample GitLab config files for different scanners

 * [GitLab config for AST](https://github.com/checkmarx-ltd/cx-flow/blob/master/src/main/resources/samples/gitlab/gitlab-ast-sample.yml)
 * [GitLab config for SAST and SCA combined](https://github.com/checkmarx-ltd/cx-flow/blob/master/src/main/resources/samples/gitlab/gitlab-sast-sca-sample.yml)
 * [GitLab config for AST Cloud](https://github.com/checkmarx-ltd/cx-flow/blob/master/src/main/resources/samples/gitlab/gitlab-astcloud-sample.yml)
 * [GitLab config for SCA](https://github.com/checkmarx-ltd/cx-flow/blob/master/src/main/resources/samples/gitlab/gitlab-sca-sample.yml)

## <a name="azure">Azure DevOps Webhook Lab</a>
* [Prep](#adoprep)
* [Triggering Webhook Scans with CxFlow](#webhooktriggering)

[Back to Tutorials Table of Contents](#tableofcontents)
<br/>
This tutorial is designed to teach the following topics:

* How to  scan on a Merge Request to a Protected Branch
* How to  scan on a Push to Protected Branch
* Azure Work Item creation on a Push to Protected Branch

### <a name="adoprep">Azure DevOps Prep</a>
* Update the bugtracker section of the application.yml file with the following
```yaml
bug-tracker: Azure
  bug-tracker-impl:
  - Azure
```
* Create an account at https://azure.microsoft.com/en-us/services/devops/
* Create a new organization if one does not already exist
* Create a new private project called CxFlowBodgeit
    * Make sure repo type is Git under Advanced
* Click Repos & Import code from your favorite small demo codebase on GitHub
    * This tutorial will use https://github.com/psiinon/bodgeit 
* Create a token by clicking your profile in upper right corner > Personal Access Tokens
    * Give the token a name and change Expiration to Custom defined and set to a year
    * Give the token full access to Work Items, Code, Build, Release
    * Copy this token and keep safe - it should be pasted into the token: <> of the application-azure.yml
* After .YML file is completely filled out and saved, start CxFlow in webhook mode 
* Create a webhook by selecting in the upper left corner Azure DevOps &  select the new repo you just created 
* Create a Webhook for Merge Requests
    * Click Project Settings \> Service hooks \> Create subscription and fill in details
    * Click Web Hooks then Next
        * Change drop down to Pull request created
        * Repository = CxFlowADO
        * Branch = main
        * URL = https://<cxflow\>/ado/pull 
        * **Note** <cxflow\> is https ngrok location of CxFlow that is running
        * Example: https://xxxxx.ngrok.io/ado/pull
        * Basic authentication username = webhook-token: left side of : from .yml file - example: cxflow
        * Basic authentication password = webhook-token: right side of : from .yml file - example: 12345
    * Click Test and a green check should appear, then click Finish
* Create a Webhook for Push to Master
    * Click Project Settings > Service hooks >  Create subscription and fill in details
    * Click Web Hooks then Next
        * Change drop down to Code pushed
        * Repository = CxFlowADO
        * Branch = main
        * URL = https://<cxflow\>/ado/push 
        * **Note** <cxflow\> is https ngrok location of cxflow that is running
        * Example: https://xxxxx.ngrok.io/ado/push
        * Basic authentication username = webhook-token: left side of : from .yml file - example: cxflow
        * Basic authentication password = webhook-token: right side of : from .yml file - example: 12345
    * Click Add Webhook 
* Continue to [Triggering Webhook Scans with CxFlow](#webhooktriggering)

##### [Top of Tutorial](#azure)

## <a name="adopipeline">Azure DevOps Pipeline</a>
* [Windows Agents](#windowsagents)
* [Docker Container](#adopipelinedocker)
* [Configuration](#adopipelineconfiguration)
* [Upgrading to CxSAST v9.0 and Above](#adopipelinenine)
* [Environment Variables](#adopipelineenvironmentvariables)
* [Scripts](#adopipelinescripts)
* [Building](#adopipelinebuilding)
* [Wget Script](#adopipelinewget)

[Back to Tutorials Table of Contents](#tableofcontents)
<br/>
This documentation is to help organizations create and run CxFlow in Azure DevOps (ADO) Pipelines.
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
##### [Top of Tutorial](#adopipeline)
This Windows based script is called entrypoint.ps1 which is the Powershell script that allows developers to run a wrapper around CxFlow. This can be distributed to all (security focused) Agents in the environment along with the application.yml and the Java archive of CxFlow.
<br/> **Auto-downloader**
<br/> The Powershell script has the ability to download automatically the current release of CxFlow as a Jar off the GitHub Releases. This feature can be disabled in environments that do not allow out-bound connections to the internet or downloading of binaries.

```yaml
trigger:
- main

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
##### [Top of Tutorial](#adopipeline)
The docker container version of CxFlow runs the exact same code as the Linux based Agents do. The only primary difference is that you can create a Docker image (container all the code and configuration) in a single binary which is immutable and can be distributed by using Docker Registries.

```yaml
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
##### [Top of Tutorial](#adopipeline)
The application.yml is where most of the static settings are stored that do not change. These can be configured per organisation and nothing sensitive should be stored in this file unless encrypted (encrypt them using [Jasypt](http://www.jasypt.org/)).

```yaml
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
##### [Top of Tutorial](#adopipeline)
When updating to CxSAST version 9.0 or above, the REST API changes so CxFlow needs to swap to version 9.0 support and some configuration changes need to be done. This requires the following changes:
<br/>More information can be found on the CxSAST Version 9.0 page

```yaml
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
##### [Top of Tutorial](#adopipeline)

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
##### [Top of Tutorial](#adopipeline)
These scripts are used on an Azure DevOps Agent as part of a Pipeline. They provide a wrapper around CxFlow to automatically pull out various [built-in Azure Pipeline variables](https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml) to provide a seamless experience for organizations. Many of the variables are dictated based on environment variables passed into the Docker container at run time or the application.yml.
<br/>These can be updated to your requirements and can be different from organization-to-organization.
<br/>The entrypoint.sh script is to support both Linux based agents and it’s the entry point for the Docker image.

### <a name="adopipelinebuilding">Building</a>
##### [Top of Tutorial](#adopipeline)
**Docker Image**<br/>
We recommend that organizations create a git repository of these files to track changes and easily deploy the images for all pipelines in the organisation in a private registry. 
<br/>Note: This Docker image can be used for any pipelines as long as the ADO variables being supplied are updated to corresponding build systems/bug tracking systems.
<br/>**Command Line Interface**
<br/> In the working directory of the source code, run the following commands:

```shell
# Building the Docker image
docker build -t organisation/cxflow .

# Pushing image to registry
docker push private-registry:5000/organisation/cxflow
```

Feel free to change the name of the image to anything but make sure that the pipelines match the container name.

<br/>**Build CxFlow using an Azure Pipeline**
<br/>If you have created a separate repository in Azure DevOps and use this simple pipeline to build and push the Docker image into an internal registry. This allows for organisations to automatically make updates to CxFlow, commit the changes, build the Docker container and push them to a globally accessible directory.

```yaml
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

### <a name="adopipelinewget">Wget Script</a>
##### [Top of Tutorial](#adopipeline)
Alternatively, you can download the CxFlow jar directly from the GitHub release page and run using the shell command below to scan the workspace and open work items.

```yaml
pool:
  vmImage: 'ubuntu-latest'

stages:
- stage: Security
  jobs:
  - job: CxFlow
    steps:
    - task: CmdLine@2
      inputs:
        script: |
      wget -O cxflow.jar https://github.com/checkmarx-ltd/cx-flow/releases/download/1.6.19/cx-flow-1.6.19.jar
      java -version
      whoami
      pwd
      java -jar cxflow.jar --spring.config.location="./application.yml" --project  --cx-project="InsertCheckmarxProjectNameHere" --alt-project="InsertADOProjectNameHere" --namespace="$(System.TeamProject)" --repo-name="$(Build.Repository.Name)" --branch="$(Build.SourceBranchName)"

```

## <a name="bitbucket">Bitbucket Cloud Webhook Tutorial</a>
* [Prep](#bitbucketprep)
* [Triggering Webhook Scans with CxFlow](#webhooktriggering)
  
[Back to Table of Contents](#tableofcontents)
<br/>

This tutorial is designed to teach the following topics:
* How to scan on a Merge Request to a Protected Branch
* How to scan on a Push to Protected Branch which opens tickets in JIRA

### <a name="bitbucketprep">Bitbucket Prep</a>
##### [Top of Tutorial](#bitbucket)
* Login or sign up for an account at https://www.bitbucket.org
* **Note** Use the same email address you used or will use to setup JIRA
* Ensure JIRA & the application.yml are setup according to [CxFlow CLI & JIRA](#clijira)
* [Connect JIRA and Bitbucket](https://support.atlassian.com/bitbucket-cloud/docs/connect-bitbucket-cloud-to-jira-software-cloud/)
* Create a new private repository named CxFlowBodgeit by clicking the + button on the sidebar
* Click Import repository to import code from your favorite small demo codebase on GitHub
    * This tutorial will use https://github.com/psiinon/bodgeit 
* Create a token by clicking your profile in lower-left corner & Personal settings
    * Click App Passwords & Create app password
    * Create a Label (i.e. CxFlow)
    * Give the token all Read/Write access to Pull requests & Webhooks
    * Copy this token and keep safe - it should be pasted into the token: <> of the application.yml
    * **Note** The token in the YML file should follow the format <UserName>:<Token>
* Once the .YML file is completely filled out and saved, start CxFlow in webhook mode 
* In Bitbucket, create a webhook by selecting Repositories & select the new repo you just created 
* Click Repository settings>Webhooks>Add Webhook and fill in details
    * Title = CxFlow
    * URL = ngrok location of cxflow that is running + ?token=webtoken from yml file - example: https://xxxxx.ngrok.io?token=12345
* Choose from a full list of triggers = Push, Pull Request Created
* Click Save
* Continue to [Triggering Webhook Scans with CxFlow](#webhooktriggering)

##### [Top of Tutorial](#bitbucket)

## <a name="clijira">CxFlow CLI & JIRA Tutorial</a>
* [Prep](#cliprep)
* [Triggering CLI Scans with CxFlow](#clitriggering)
* [Back to Tutorials Table of Contents](#tableofcontents)
<br/>

This tutorial is designed to teach the following topics:
* How to configure a Jira project for CxFlow
* Automated ticket creation using CxFlow CLI
* Scanning via CxFlow CLI

### <a name="cliprep">Jira Prep</a>
##### [Top of Tutorial](#clijira)
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
    * Click Tutorialels and give it a name “Application”
    * Description = CxSAST Project
    * Select the checkboxes next to APPSEC: Kanban Bug Screen & APPSEC: Kanban Default Issue Screen
    * Click Update
* Create another custom field for Category
    * Name = Category
    * Description = CxSAST Vulnerability Type
    * Select the checkboxes next to APPSEC: Kanban Bug Screen & APPSEC: Kanban Default Issue Screen
    * Click Update

### <a name="clitriggering">Triggering Scans with CxFlow</a>
##### [Top of Tutorial](#clijira)

* Update the bugtracker section of the application.yml file with the following
```yaml
bug-tracker: JIRA
  #bug-tracker-impl:
```
* After the .YML file is completely filled out and saved
* The following command clones a github repo, creates a CxSAST scan for the cloned repo, and creates tickets according to the .yml file
```
cd C:\CxFlow
git clone https://github.com/ethicalhack3r/DVWA.git 
cd C:\CxFlow
java -jar cx-flow-1.6.19.jar --spring.config.location="C:\CxFlow\application.yml" --scan --f="./DVWA" --cx-team="CxServer\SP\Company" --cx-project="DVWA" --app="DVWA"
```
* **Note** The url for the jira section of the .yml file should be the one assigned to you when you first start your Jira account, for example 
```
url: https://<username>.atlassian.net/
```
* The following command opens tickets for a CxSAST project’s last finished scan according to the .yml file

```
cd C:\CxFlow
java -jar cx-flow-1.6.19.jar --spring.config.location="C:\CxFlow\application.yml" --project --cx-team="CxServer\SP\Company" --cx-project="DVWA" --app="DVWA"
```
* Open the APPSEC project in Jira and note the vulnerabilities that have been opened

### Bonus
* You can kick off batch mode ticket creation in any Linux pipeline by supplying the application.yml file and using the following code to download CxFlow and run
* **Note** Replace cx-project and app flags with environment variables relevant to the pipeline.  wget can also be used instead of curl
```
apk add --update curl
curl -O -k https://github.com/checkmarx-ltd/cx-flow/releases/download/1.6.19/cx-flow-1.6.19.jar
java -jar cx-flow-1.6.19.jar --spring.config.location="./application.yml" --scan --f=. --cx-team="CxServer" --cx-project="Bodgeit" --app="Bodgeit"
```
* [Back to Tutorials Table of Contents](#tableofcontents)

## <a name="batch">CxFlow Batch Mode Tutorial</a>
* [Requirements](#batchrequirements)
* [SMTP Server Prep](#smtpserverprep)
* [Triggering Scans with CxFlow](#batchtriggering)
* [EmailPNEVulns.ps1](#emailpne)

[Back to Tutorials Table of Contents](#tableofcontents)
<br/>

This tutorial is designed to teach the following topics:
* Run CxFlow CLI with XML results output
* Automate email notifications on Proposed Not Exploitable Vulnerabilities using the [EmailPNEVulns.ps1](#emailpne) script below

### <a name="batchrequirements">Requirements:</a>
##### [Top of Tutorial](#batch)
* Create a folder on the C:\ drive called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br/>https://github.com/checkmarx-ltd/cx-flow/releases
<br/>The Java 11 version will have -java11 at the end of the file name 
<br/>**Note** This guide is using CxFlow version 1.6.12, if you download another version, input your version in the command below
* Create a new file called EmailPNEVulns.ps1 in C:\Flow with the text at the bottom of the page and replace any values surrounded in ###<\>### with your appropriate values, see [SMTP Server Prep](#smtpserverprep) steps below
* In the same folder create a file titled application-email.yml
* Add the text below to the application-email.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br/> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the GitHub heading please enter your GitHub token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br/>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br/>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.
```yaml
server:
  port: 8982
logging:
  file:
    name: flow.log

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
##### [Top of Tutorial](#batch)
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

### <a name="batchtriggering">Triggering Scans with CxFlow</a>
##### [Top of Tutorial](#batch)
* After the .YML file and .PS1 file are completely filled out and saved
* Run CxFlow in batch mode & the email PowerShell script by opening a Powershell prompt and typing the following
```
cd C:\CxFlow
java -jar cx-flow-1.6.19.jar --spring.config.location="C:\CxFlow\application-email.yml" --project --cx-team="CxServer\SP\Company" --cx-project="DVWA" --app="DVWA"
.\EmailPNEVulns.ps1 -results_xml .\xmlresults.xml -email <youremail>
```
* Open your email & verify that the Proposed Not Exploitable results have been emailed.
* The email might be in your junk folder.

### Bonus
You can use Windows Task Scheduler to call the above commands/scripts & run this every evening.

### <a name="emailpne">EmailPNEVulns.ps1</a>
##### [Top of Tutorial](#batch)
```powershell

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
* [Back to Tutorials Table of Contents](#tableofcontents)
##### [Top of Tutorial](#batch)

Pre-requisites:

Have a JIRA project ready with the Application and Category custom fields (see previous tutorials)

Have a private GitHub repo (no webhook required)

Be familiar with config-as-code overrides (see previous tutorials)

Goal:

The goal of this workshop is to set up a GitHub Action workflow to leverage the CxFlow GitHub Action in a private GitHub repository to launch a CxSAST scan on code pushes and pull requests on the master branch and create/update issues in JIRA.

GitHub Actions:

“GitHub Actions help you automate your software development workflows in the same place you store code and collaborate on pull requests and issues. You can write individual tasks, called actions, and combine them to create a custom workflow.”

How does the CxFlow GitHub Action work ?

The action is available on the GitHub marketplace. The source is available here:  

GitHub actions rely on a .yml workflow definition file stored under /.github/workflows.

The action runs CxFlow in a container based on the standard checkmarx/cx-flow available from Docker Hub and uses a basic application.yml file. There are different tags of the action available depending on your version of CxSAST.

By default, the output of the action is a Sarif file for integration into GitHub’s CodeQL (See https://github.com/marketplace/actions/checkmarx-cxflow-action).

It is possible to override the default application.yml file from the workflow yml file using the params: field and via the usual cx.config file in the repository.

Step 1: Create a config-as-code override file in your GitHub repo

Since the application.yml provided by the CxFlow GitHub Action doesn’t contain a complete jira section, we have to use a configuration override.

Create a new file named cx.config at the root of your repository (main branch) containing the following (adapt the values with your specific environment details).

{
  "application": "DSVW",
  "branches": ["develop", "main"],
  "bugTracker": "JIRA",
  "jira": {
	"project": "DSVW",
	"issue_type": "Bug",
	"opened_status": ["Open","Reopen"],
	"closed_status": ["Closed","Done"],
	"open_transition": "Reopen Issue",
	"close_transition": "Close Issue",
	"close_transition_field": "resolution",
	"close_transition_value": "Done",
	"priorities": {
	  "High": "High",
	  "Medium": "Medium",
	  "Low": "Low"
	},
	"fields": [
	  {
		"type": "result",
		"name": "application",
		"jira_field_name": "Application",
		"jira_field_type": "label"
	  },
	  {
		"type": "result",
		"name": "category",
		"jira_field_name": "Category",
		"jira_field_type": "label"
	  }
	]
  }
}

Step 2: Create a workflow

In your GitHub repository, click on “Actions”, then “set up a workflow yourself”

In the Marketplace panel, search for “cxflow”

Click on “Checkmarx CxFlow Action”

Select your version (only version v1.0-9.x supports CxSAST 9.x at the moment) and click the icon on the right to copy the action code to your clipboard.

Then on the left panel, replace this section with it:

Correct the indentation. Then enter your details or use GitHub Secrets (setup in your repository’s settings). You must provide more details (including the JIRA connection) via the params: field.

params: --bug-tracker=jira --config=cx.config --repo-name=DSVW --namespace=jbruinaud --branch=main --jira.url=${{secrets.JIRA_URL}} --jira.username=${{secrets.JIRA_USER}} --jira.token=${{secrets.JIRA_TOKEN}

Here is a complete main.yml working example with GitHub Secrets. Notice the top section with the name of the workflow and the triggers configuration and also the bottom parameters.

## <a name="actions">GitHub Actions with JIRA Integration</a>
### Pre-requisites
* Have a JIRA project ready with the Application and Category custom fields (see previous tutorials)
* Have a private GitHub repo (no webhook required)
* Be familiar with config-as-code overrides

### Goal
The goal of this workshop is to set up a GitHub Action workflow to leverage the CxFlow GitHub Action in a private GitHub repository to launch a CxSAST scan on code pushes and pull requests on the master branch and create/update issues in JIRA.

### GitHub Actions
“GitHub Actions help you automate your software development workflows in the same place you store code and collaborate on pull requests and issues. You can write individual tasks, called actions, and combine them to create a custom workflow.”

### How does the Checkmarx GitHub Action work?
The action is available on the GitHub marketplace. The source is available [here](https://github.com/checkmarx-ts/checkmarx-cxflow-github-action).
<br>GitHub actions rely on a .yml workflow definition file stored under /.github/workflows.<br>
<br>The action runs CxFlow in a container based on the standard checkmarx/cx-flow available from Docker Hub and uses a basic application.yml file. There are different tags of the action available depending on your version of CxSAST.<br>
<br>By default, the output of the action is a Sarif file for integration into [GitHub’s CodeQL](https://github.com/marketplace/actions/checkmarx-cxflow-action).<br>
<br>It is possible to override the default application.yml file from the workflow yml file using the params: field and via the usual cx.config file in the repository.<br>

### Step 1: Create a config-as-code override file in your GitHub repo
Since the application.yml provided by the CxFlow GitHub Action doesn’t contain a complete jira section, we have to use a configuration override.
<br>Create a new file named cx.config at the root of your repository (main branch) containing the following (adapt the values with your specific environment details).
```yaml
{
  "application": "DSVW",
  "branches": ["develop", "main"],
  "bugTracker": "JIRA",
  "jira": {
    "project": "DSVW",
    "issue_type": "Bug",
    "opened_status": ["Open","Reopen"],
    "closed_status": ["Closed","Done"],
    "open_transition": "Reopen Issue",
    "close_transition": "Close Issue",
    "close_transition_field": "resolution",
    "close_transition_value": "Done",
    "priorities": {
      "High": "High",
      "Medium": "Medium",
      "Low": "Low"
    },
    "fields": [
      {
      "type": "result",
      "name": "application",
      "jira_field_name": "Application",
      "jira_field_type": "label"
      },
      {
      "type": "result",
      "name": "category",
      "jira_field_name": "Category",
      "jira_field_type": "label"
      }
    ]
  }
}
```

### Step 2: Create a workflow
<br>In your GitHub repository, click on “Actions”, then “set up a workflow yourself”
[[/Images/gh1.png|Getting Started]]
<br>In the Marketplace panel, search for “cxflow”
[[/Images/gh2.png|Marketplace]]
<br>Click on “Checkmarx CxFlow Action”
[[/Images/gh3.png|Action]]
<br>Select your version (only version v1.0-9.x supports CxSAST 9.x at the moment) and click the icon on the right to copy the action code to your clipboard.
<br>Then on the left panel, replace this section with it:
[[/Images/gh4.png|Action]]
<br>Correct the indentation. Then enter your details or use GitHub Secrets (setup in your repository’s settings). You must provide more details (including the JIRA connection) via the params: field.
```
params: --bug-tracker=jira --config=cx.config --repo-name=DSVW --namespace=jbruinaud --branch=main --jira.url=${{secrets.JIRA_URL}} --jira.username=${{secrets.JIRA_USER}} --jira.token=${{secrets.JIRA_TOKEN}
```
<br>Here is a complete main.yml working example with GitHub Secrets. Notice the top section with the name of the workflow and the triggers configuration and also the bottom parameters.
```yaml
# This is a basic workflow to help you get started with Actions
name: CxFlow

# Controls when the action will run. Triggers the workflow on push or pull request
# events but only for the main branch

on:
  push:
    branches: [ master, main ]
  pull_request:
    branches: [ master, main ]

# A workflow run is made up of one or more jobs that can run sequentially or in parallel
jobs:
  # This workflow contains a single job called "build"
  build:
    # The type of runner that the job will run on
    runs-on: ubuntu-latest

    # Steps represent a sequence of tasks that will be executed as part of the job
    steps:
      # Checks-out your repository under $GITHUB_WORKSPACE, so your job can access it
      - uses: actions/checkout@v2

      # Cxflow Action
      - name: Checkmarx CxFlow Action
        # You may pin to the exact commit or the version.
        uses: checkmarx-ts/checkmarx-cxflow-github-action@v1.0-9.x
        with:
          # Provide Checkmarx URL
          checkmarx_url: ${{secrets.CHECKMARX_URL}}
          # Provide team
          team: /CxServer/SP/EMEA/UK
          # Provide Checkmarx Username
          checkmarx_username: ${{secrets.CHECKMARX_USERNAME}}
          # Provide Checkmarx Password
          checkmarx_password: ${{secrets.CHECKMARX_PASSWORD}}
          # Provide Checkmarx Client Secret
          checkmarx_client_secret: ${{secrets.CHECKMARX_CLIENT_SECRET}}
          # Select a Checkmarx Project
          project: DSVW-GitHub-action
          # Select an Application Name used by downstream bug tracker systems
          app: DSVW
          # Select a Checkmarx Preset
          #preset: # optional, default is Checkmarx Default
          # Break build based on Checkmarx findings?
          #break_build: # optional
          # Incremental Scans?
          incremental: true
          # GitHub API Token (note: you don't have to create secrets.GITHUB_TOKEN, it is created automatically and will not appear in your repo's custom secrets)
          github_token: ${{secrets.GITHUB_TOKEN}}
          # extra parameters
          params: --bug-tracker=jira --config=cx.config --repo-name=DSVW --namespace=jbruinaud --branch=main --jira.url=${{secrets.JIRA_URL}} --jira.username=${{secrets.JIRA_USER}} --jira.token=${{secrets.JIRA_TOKEN}}
```
<br>Click “Start commit” then “Commit new file” to complete the process. This will trigger the workflow automatically since we are committing a new file to the main branch.<br>

[[/Images/gh5.png|Commit]]
### Step 3: Monitor the workflow execution
<br>Click on “Actions”. You will see your workflow execution details, in yellow (in execution), green (succeeded) or red (failed). Click on it.<br>
[[/Images/gh6.png|Monitor]]
<br> Then click on “build”<br>
[[/Images/gh7.png|Build]]
<br> You now can see the execution logs.<br>
[[/Images/gh8.png|Execution]]
### Extra configuration:
<br>Additional parameters (passed via the params: field) can be found in the [Configuration Definitions section](https://github.com/checkmarx-ltd/cx-flow/wiki#configuration-details).<br>

<br>For example, only process Urgent and Confirmed results by adding this parameter:<br> 
```
--cx-flow.filter-state=Confirmed,Urgent
```

## <a name="IASTintegrations">CxFlow IAST Integration Tutorial</a>
[Back to Table of Contents](#tableofcontents)
### <a name="IASTprerequisites">Prerequisites</a>
<br>The following must be set up:<br>

* CxIAST Management Server (refer to CxIAST Setup Guide and Installing the CxIAST Management Server)
* The application under test (AUT) (refer to Configuring the AUT Environment)  
* Jenkins server (refer to Installing Jenkins) 
* Bug Tracker (refer to Bug Tracker)

### <a name="IASTgeneralprocedures">General Procedure</a>

<br> Create a pipeline in Jenkins that will perform the following:<br>
1. Start CxFlow.
2. Create a scan tag so that CxFLow can identify the scan.
3. Start the AUT with a CxIAST agent.
4. Start an E2E test suite on the AUT.
5. Stop the CxIAST scan and open tickets

### <a name="IASTJenkinsPipeline">Sample Jenkins Pipeline</a>
Following is a description of a sample Jenkins declarative pipeline written in Groovy.
#### Stage 1. Start CxFlow
```
        // 1. Get CxFlow - download CxFlow .jar and save it in a "cx-flow" folder
        stage('Build CxFlow Jar'){
            steps {
                script{
                    dir("cx-flow"){
                        sh "curl https://github.com/checkmarx-ltd/cx-flow/releases/download/${CX_FLOW_VERSION}/cx-flow-${CX_FLOW_VERSION}.jar --output cx-flow.jar"
                    }
                }
            }
        }

        // CxFlow can be used in server mode, or in CLI mode.
        // If used in CLI mode - skip this step.
        // If used in server mode, run it (needs to happen before running the AUT):
        stage('Run CxFlow in server mode'){
            steps{
                script{
                    dir('cx-flow'){
                        sh """
                            JENKINS_NODE_COOKIE=dontKillMe nohup java -jar cx-flow*.jar --spring.config.location=/var/jenkins/CxFlow/application.yml --web&>/tmp/start_app.log &
                        """
                        sleep 30
                    }
                }
            }
        }
```
#### Stage 2. Create a Scan ID Tag
```
        // 2. Create a scan tag so that CxFlow can identify the scan.
        // CxIAST scan is started with the generated scan tag so CxFlow can stop it after tests are finished.

        stage('Generate Scan Tag'){
            steps{
                script{
                    def tag = ""

                    // CxFlow can be used in server mode, or in CLI mode

                    // If CxFlow is in server mode, get a generated scan tag from CxFlow:
                    def response = sh(script: "curl --header \"Content-Type:application/json\" \
                                                    --header \"X-Atlassian-Token:xxxx\" \
                                                    --request POST \
                                                     --data '{\"success\":\"true\",\"message\":\"ok\"}' \
                                                     http://CXFLOW-URL/iast/generate-tag",returnStdout: true)
                    def jsonSlurper = new JsonSlurper(type: JsonParserType.INDEX_OVERLAY)
                    def object = jsonSlurper.parseText(response)
                    tag = object.message

                    // If CxFlow is in CLI mode, generate a scan tag (make sure it's unique)
                    def now = new Date()
                    tag =  now.format("yyMMddHHmmSS", TimeZone.getTimeZone('UTC'))

                    // Used in following steps
                    APP_TAG = tag
                }
            }
        }
```
#### Stage 3. Start the Application Under Test (AUT)
```
        // 3. Start the AUT with a CxIAST agent.
        // This example downloads a Java agent from CxIAST manager, extracts it, and runs WebGoat8 with an agent attached

        stage('Run The App'){
            steps {
                script {
                    sh "curl http://CXIAST-URL/iast/compilation/download/JAVA --output javaagent.zip"
                    sh "unzip javaagent.zip -d javaagent"
                    sh "JENKINS_NODE_COOKIE=dontKillMe nohup java -javaagent:javaagent/cx-launcher.jar -Dcx.appName=WebGoat -DcxScanTag=${APP_TAG} -jar /var/jenkins/webgoat-server-8.0.0.M21.jar --server.address=0.0.0.0 &>/tmp/run_webgoat.log &"
                }
            }
        }
```
#### Stage 4. Start E2E test suite on the AUT
```
        // 4. Start an E2E test suite on the AUT.
        // This example uses jmeter to run tests against WebGoat8

        stage('Run Jmeter'){
            steps{
                script{
                    sh "/var/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t WebGoat8.jmx"
                }
            }
        }
```
#### Stage 5. Stop the CxIAST scan and open tickets
```
        // 5. Stop the CxIAST scan and open tickets.
        // CxFlow can be used in server mode, or in CLI mode

        stage('Stop Scan - Server mode'){
            steps{
                script{

                    // If bug tracker is Jira
                    sh "curl --header \"Content-Type:application/json\" \
                            --header \"X-Atlassian-Token:xxxx\" \
                            --request POST \
                             --data '{\"success\":\"true\",\"message\":\"ok\"}' \
                             http://CXFLOW-URL/iast/stop-scan-and-create-jira-issue/${appTag}"
                    }

                    // ------------- OR ------------

                    // If bug tracker is GitHub
                    sh "curl --header \"Content-Type:application/json\" \
                            --header \"X-Atlassian-Token:xxxx\" \
                            --request POST \
                            --data '{\"success\":\"true\",\"message\":\"ok\", \"assignee\":\"talilabok\", \"repoName\":\"cxflow-github\", \"namespace\":\"CxIAST\"}' \
                             http://CXFLOW-URL/iast/stop-scan-and-create-github-issue/${appTag}"
                }
            }

        // ---------OR-----------

        stage("Stop Scan - Cli Mode"){
            steps{
                script{

                    // If bug tracker is Jira
                    dir("cx-flow"){
                       echo "tag is ${APP_TAG}"
                       sh """
                       java -jar cx-flow*.jar --spring.config.location=/var/jenkins/CxFlow/application.yml --iast --scan-tag=\"${appTag}\" --bug-tracker=\"jira\"
                       """
                    }

                    // ------------- OR ------------

                    // If bug tracker is GitHub
                    dir("cx-flow"){
                        sh """
                        java -jar cx-flow*.jar --spring.config.location=/var/jenkins/CxFlow/application.yml --iast --scan-tag=\"${appTag}\" --bug-tracker=\"githubissue\" --github.token=\"${githubToken}\" --repo-name=\"cxflow-github\" --namespace=\"CxIAST\" --assignee=\"CxIastCi\"
                        """
                    }

                }
            }
        }
    }
}
```
### <a name="IASTYaml">Yaml - application.yml file</a>
```
server:
  port: CXFLOW-PORT # used when CxFlow is in server mode

logging:
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{15}){cyan}  [%clr(%X{cx}){blue}] %clr(:){faint} %replace(%m){'([\\|])','\\$1'}%n%wEx"
    file: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{15}){cyan}  [%clr(%X{cx}){blue}] %clr(:){faint} %replace(%m){'([\\|])','\\$1'}%n%wEx"
  file:
    name: cx-flow.log


cx-flow:
  # Agreed upon shared API token
  token: xxxx
  bug-tracker: JIRA
  bug-tracker-impl:
    - CxXml
    - Json
    - JIRA
    - GitLab
    - GitHub
    - Csv
    - Azure
    - Rally
    - ServiceNow
    - Sarif
    -SonarQube
  branches:
    - develop
    - master
    - security
  filter-severity:
    - High
  filter-category:
  filter-cwe:
  filter-status:
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html

checkmarx:
  username: xxxxx
  password: xxxxx
  client-secret: xxxxx
  base-url: http://cx.local
  multi-tenant: true
  configuration: Default Configuration
  scan-preset: Checkmarx Default
  team: \CxServer\SP\Checkmarx
  url: ${checkmarx.base-url}/cxrestapi
#WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx

jira:
  url: https://cx-iast-ci.atlassian.net
  username: xxxxx
  token: xxxxx
  project: CIC
  issue-type: Bug
  priorities:
    X: Y # Map Checkmarx severity : to JIRA Priority
  open-transition: Reopen Issue
  close-transition: Close Issue

iast:
  url: http://CXIAST-HOSTNAME
  manager-port: CXIAST-PORT
  username: xxxxx
  password: xxxxx
  update-token-seconds: 150  # By default token live only 5 minutes
  filter-severity:
    - HIGH
    - MEDIUM
    - LOW
  #    - INFO
  thresholds-severity:
    HIGH: -1
    MEDIUM: -1
    LOW: -1
    INFO: -1

cxgo:
  client-secret: xxx
  base-url: https://cxgo-url
  portal-url: https://cxgo-url
  # CxOD Business unit that will contain the project/application/scan
  team: \Demo\CxFlow
  url: ${cxgo.base-url}
  multi-tenant: true
  configuration: Default Configuration
  scan-preset: 1,2,3,4,5,9

cx-integrations:
  url: https://cx.local
  read-multi-tenant-configuration: false

rally:
  token: xxxx
  rally-project-id: xxxx
  rally-workspace-id: xxxx
  url: https://rallydev.com
  api-url: https://xxxxx.rallydev.com/slm/webservice/v2.0

servicenow:
  token: 123
  servicenow-project-id: xxxx
  servicenow-workspace-id: xxxx
  url: https://servicenow.com
  apiUrl: https://xxxxx.service-now.com/api/now/table
  username: xxxx
  password: xxxx

github:
  webhook-token: xxxx
  token: xxxxx
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true

gitlab:
  webhook-token: xxxx
  token: xxxx
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  block-merge: true

azure:
  webhook-token: xxxx
  token: xxxx
  url: https://dev.azure.com
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive

json:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].json"
  data-folder: "/tmp/cxflow"

cx-xml:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].xml"
  data-folder: "/tmp/cxflow"

csv:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].csv"
  data-folder: "/tmp/cxflow"
  include-header: true
  fields:
    - header: Application
      name: application
      default-value: unknown
    - header: severity
      name: severity
    - header: Vulnerability ID
      name: summary
      prefix: "[APP]:"
    - header: file
      name: filename
    - header: Vulnerability ID
      name: summary
    - header: Vulnerability Name
      name: category
    - header: Category ID
      name: cwe
    - header: Description
      name: description
    - header: Severity
      name: severity
    - header: recommendation
      name: recommendation
    - header: Similarity ID
      name: similarity-id
```

## <a name="SonarQubeIntegrations">CxFlow SonarQube Integration</a>
[Back to Table of Contents](#tableofcontents)
### <a name="SonarQubePrerequisites">Prerequisites</a>
<br>The following must be set up:<br>

* SonarQube Server (refer to SonarQube Setup Guide on [here](https://docs.sonarqube.org/latest/setup/install-server))

* SonarQube Scanner (refer to SonarQube Scanner Installation on [here](https://docs.sonarqube.org/latest/setup/get-started-2-minutes))

* Generate the Sonar Qube Issue Report by configuring bug tracker as SonarQube.

### <a name="SonarQubeGeneralprocedures">General Procedure</a>

<br> Upload the CxFlow Sonar Qube Report generated for SAST or SCA scan:<br>
1. Set Sonar Scanner in Windows PATH Varaible.
2. Edit {SONAR_SCANNER_HOME}\conf\sonar-scanner.properties for below properties:
```
    sonar.host.url=http://localhost:9000
    sonar.projectKey={PROJECT_KEY}
    sonar.projectName={PROJECT_NAME}
    sonar.projectVersion=1.0
    sonar.sources=.
    sonar.externalIssuesReportPaths={PATH_TO_CX_FLOW_SONAR_REPORT}
    sonar.issuesReport.json.enable=true
    sonar.verbose=true
    sonar.showProfiling=true
    sonar.login={SONARQUBE_USER_TOKEN}
```

3. Create sonar-project.properties in base path of code or repository as below:
```
   sonar.projectKey={PROJECT_KEY}
   sonar.organization={ORGANIZATION}
   sonar.java.binaries={CLASS_FOLDER}(e.g:./target/classes)
   sonar.exclusions={EXCLUDE_FOLDER}(e.g:src/test/resources/**)
```

4. Run the below command from the floder where sonar-project.properties is located:
```
   sonar-scanner -X
```
