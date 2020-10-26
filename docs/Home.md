# CxFlow
[[/Images/cxLogo.PNG]]
<br>[What is it?](#whatisit)
<br>[Quick Start](#quickstart)
<br>[Configuration](#configuration)
<br>[Server Configuration](#serverconfiguration)
<br>[CxFlow Configuration](#cxflowconfiguration)
<br>[Checkmarx Environment Configuration](#checkmarxenvironmentconfiguration)
<br>[Gotchas!](#gotchas)
<br>
## <a name="whatisit">What is it?</a>
CxFlow is a springboot application that can run anywhere Java is installed. CxFlow glues together Checkmarx CxSAST, CxSCA, and CxOSA scans with feedback to issue tracking systems via webhooks triggered by SCM events. CxFlow can also run as a CLI tool embedded in CI/CD pipelines. 

## <a name="quickstart">Quick Start</a>
This quick start guide describes how to trigger a CxSAST scan on a Pull Request and a Push to a protected Github branch. Pushes to a protected branch will create GitHub Issues from the scan results.

Requirements:
* Create a folder called CxFlow
* Into this folder, download the latest CxFlow .jar for JDK8
<br>https://github.com/checkmarx-ltd/cx-flow/releases
<br>**Note** This guide is using CxFlow version 1.5.4, if you download another version, input your version in the command below
* In the folder create a file titled application.yml
* Add the text below to the application.yml file replacing any values enclosed in ###\<\>### with your appropriate value 
<br> Under the Checkmarx heading, you should enter your service account's username, password, and confirm the base-url. Under the Github heading please enter your Github token and web-hook token if you have entered a value for the web-token different from this guide's value of 12345. Finally, enter another port if you are using a port other than 8982.
<br>**Note** This .yml file is for CxSAST version 8.9. For later versions, navigate to the 9.0 link on the side bar
<br>**Note** The client-secret value included here is the correct value for CxSAST and is not actually a secret value. It is the OIDC client secret used for API login to Checkmarx.

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
ngrok http 8982
```
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
java -jar cx-flow-1.5.4.jar --spring.config.location="<path\to>\CxFlow\application.yml" --web
```
* Create a webhook by selecting your profile and selecting the repo you just created
    * Navigate to Settings > Webhooks > Add Webhook and fill in the details
        * Payload URL: ngrok example: http://4d91e7ed.ngrok.io
        * Content type:  application/json
        * Secret: Webhook token from .yml file, in this example 12345
        * Select Events:  Pull Requests, Pushes
    * Click Add Webhook, there should be a checkmarx next to the hook name now
* Open your IDE of choice. This demo will use IntelliJ
    * Check out code using Check out from Version Control, input the URL for your repo example:
<br>[https://github.com/<username\>/CxFlowGithub](http://github.com)
    * Open README.md and add a line, example: CxFlowMasterPush-Test1
    * Commit to local git repo and push to origin with comments by clicking the following: VCS > Git > Commit File enter a message like CxFlow push to a protected branch
    * Click commit and push
    * Click Push and enter Github credentials on popup. Username is your username, password is the token you created above.
* Navigate to the Checkmarx web portal. You should see a new scan in the CxSAST queue
<br>Notice the project name is the RepoName-Branch
<br>Notice the team is the Github organization. This is set by the team line in the .yml file. It auto creates a team if it does not exist. This can be overridden in the config file with the multi-tenant setting. Please see the CxFlow configuration page for more information.
* When the scan finishes, you should see issues on the Issue tab of your Github repo 
<br>[https://github.com/<username\>/CxFlowGithub/issues](http://github.com/)
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

## <a name="configuration">Configuration Details</a>
<br>**Note** This information is duplicated on the Configuration page

### <a name="serverconfiguration">Server Configuration</a>
| Config      | Default | Required | WebHook | Command Line | Notes                                                                    |
|-------------|---------|----------|---------|--------------|--------------------------------------------------------------------------|
| **server**  |         |          |         |              |                                                                          |
| port        | 8080    | No       | Yes     | No           | The default value is 8080 unless an environment variable port is defined |

### <a name="cxflowconfiguration">CxFlow Configuration</a>
| Config      | Default | Required | WebHook | Command Line | Notes                                                                    |
|-------------|---------|----------|---------|--------------|--------------------------------------------------------------------------|
| **cx-flow** |         |          |         |              |                                                                          |
| contact     |         | No       |         |              | Contact email for the CxFlow administrator                               |
| bug-tracker |         | Yes      | Yes     | Yes          | Must be one of the following:                                            |
|             |         |          |         |              | None                                                                     |
|             |         |          |         |              | Jira                                                                     |
|             |         |          |         |              | Email                                                                     |
|             |         |          |         |              | Any value specified in the bug-tracker-impl custom bean implementations list (a white list of bug tracker implementations) |
|             |         |          |         |              | **Note**:  JIRA/EMAIL/NONE are built in and not required in the bug-tracker-impl list |
| bug-tracker-impl |    | No (Only if using one of the applicable bug tracker implementations | Yes | Yes | List of available bug trackers (feedback channels).  Currently support for: |
|             |         |          |         |              | Csv |
|             |         |          |         |              | Json |
|             |         |          |         |              | CxXML |
|             |         |          |         |              | GitLab |
|             |         |          |         |              | GitHub |
|             |         |          |         |              | Azure |
|             |         |          |         |              | Rally |
| branches    |         | No       | Yes     | No           | List of protected branches that drive scanning within the WebHook flow.  If a pull or push event is initiated to one of the protected branches listed here, the scan is initiated.  For example |
|             |         |          |         |              | develop |
|             |         |          |         |              | master |
|             |         |          |         |              | security |
|             |         |          |         |              | release-\w+ |
|             |         |          |         |              | If no value is provided, all branches are applicable |
|             |         |          |         |              | Regular expressions are supported. (i.e. release-\w+ will match any branches starting with release-) |
| branch-script |       | No       | Yes     | No           | A **groovy** script that can be used to decide, if a branch is applicable for scanning. This applies to any client custom lookups and other integrations.  The script is passed as a **"request"** object of the type **com.checkmarx.flow.dto.ScanRequest** and must return **boolean** (true/false). If this script is provided, it is used for all decisions associated with determining applicability for a branch event to be scanned. **A sample script is attached to this page. |
| filter-severity |     | No       | Yes    | Yes           | The severity can be filtered during feedback (**High**, **Medium**, **Low**, **Informational**).  If no value is provided, all severity levels are applicable. |
| filter-category |     | No       | Yes    | Yes           | The list of vulnerability types to be included with the results (**Stored_XSS**, **SQL_Injection**) as defined within Checkmarx.  If no value is provided, all categories are applicable. |
| filter-cwe      |     | No       | Yes    | Yes           | The list of CWEs to be included with the results (**79**, **89**).  If no value is provided, all categories are applicable. |
| filter-status   |     | No       | Yes    | Yes           | The available options are **Urgent** and **Confirmed**.  This only allows for filtering the results that have been confirmed/validated within Checkmarx. |
| mitre-url       |     | No       | Yes    | Yes           | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues** to help guide developers.  The link is not provided, if left empty or omitted. |
| wiki-url        |     | No       | Yes    | Yes           | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues** associated with internal program references (program/assessment methodology, remediation guidance, etc).  The link is not provided, if left empty or omitted. |
| codebash-url    |     | No       | Yes    | Yes           | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues**  associated with training. The link is titled **'Training'** and is not provided, if left empty or omitted. |
| track-application-only | false | No* | Yes | Yes          |                                                                        |
| web-hook-queue         | 100   | No* | Yes | No           | The maximum number of active scans initiated via WebHook at a given time.  Requests remain queued until a slot is free. |
| scan-result-queue      | 4     | No* | Yes | Yes          | The maximum number of scan results being processed at the same time.  Requests remain queued until a slot is free.  As XML files can become large, it is important to limit the number that can be processed at the same time. |
| break-build            | false | No* | No  | Yes          | A non-zero return code (10) is provide when any of the filtering criteria is met within scan results. |
| http-connection-timeout | 30000 | No* | Yes | Yes         | Http client connection timeout setting.  Not applied for the Jira client. |
| http-read-timeout | 120000      | No* | Yes | Yes         | Http client read timeout setting.  Not applied for the Jira client. |
| mail              | enabled:false | No* | Yes | Yes       | SMTP configuration - host, port, username, password, enabled (false by default).  When enabled, email is a valid feedback channel, and an html template is used to provide result details. During WebHook execution, the email is sent to the list of committers in the push event.
| auto-profile      | false         | No  | Yes | No        | During WebHook execution, language stats and files are gathered to help determine an appropriate preset to use.  By default, the profiling initially occurs only when a project is new/created for the first time.  Refer to [CxFlow Automated Code](https://checkmarx.atlassian.net/wiki/spaces/PTS/pages/1345586126/CxFlow+Automated+Code+Profiling) Profiling for details.
| always-profile    | false         | No  | Yes | No        | This enforces the auto-profile execution for each scan request regardless of whether the project is new or not. |
| profiling-depth   | 1             | No  | Yes | No        | The folder depth that is inspected for file names during the profiling process, which means looking for specific file references, i.e. web.xml/Web.config |
| profile-config    | CxProfile.json | No | Yes | No        | The file that contains the profile configuration mapping. |
| scan-resubmit     | false          | No | Yes | Yes       | When **True**: If a scan is active for the same project, CxFlow cancels the active scan and submits a new scan. When **False**: If a scan is active for the same project, CxFlow does not submit a new scan. |

### <a name="checkmarxenvironmentconfiguration">Checkmarx Environment Configuration</a>
| Config      | Default | Required | WebHook | Command Line | Notes                                                                    |
|-------------|---------|----------|---------|--------------|--------------------------------------------------------------------------|
| **checkmarx**     |                |    |     |           |                                                                           |
| username          |                | Yes | Yes | Yes      | Service account for Checkmarx                                             |
| password          |                | Yes | Yes | Yes      | Service account password Checkmarx                                        |
| client-secret     |                | Yes | Yes | Yes      | OIDC client secret for API login to Checkmarx                             |
| base-url          |                | Yes | Yes | Yes      | Base FQDN and port for Checkmarx                                          |
| multi-tenant      | false          | No* | Yes | Yes (Scan only) | If yes, the name space is created or reused, if it has been pre-registered or already created for previous scans)    |
| scan-preset       | Checkmarx Default | No* | Yes | Yes (Scan only) | The default preset used for the triggered scan                 |
| configuration      | Default Configuration | No* | Yes | Yes (Scan only) | Checkmarx scan configuration setting |
| team          |                | Yes (not for XML parse mode) | Yes | Yes (Scan only)  | Base team in Checkmarx to drive scanning and retrieving of results |
| scan-timeout       | 120 | No* | Yes | Yes (scan only) | The amount of time (in minutes) that CxFlow will wait for a scan to complete to process the results.  The Checkmarx scan remains as is, but no feedback is provided |
| jira-project-field | jira-project | No | Yes | Yes | Custom Checkmarx field name to override Jira Project setting for a given Checkmarx scan result / project |
| jira-issuetype-field | jira-issuetype | No | Yes | Yes | Custom Checkmarx field name to override Jira Issue Type settings for a given Checkmarx scan result / project |
| jira-custom-field | jira-fields | No | Yes | Yes | Custom Checkmarx field name to override Jira custom field mappings for a given Checkmarx scan result / project |
| jira-assignee-field | jira-assignee | No | Yes | Yes | Custom Checkmarx field name to override Jira assignees for a given Checkmarx scan result / project |
| preserve-xml | false | No* | Yes | Yes | This flag is used to preserve the original XML results retrieved by the Checkmarx scan inside the ScanResults object to be later used by a Custom bug tracker implementation, if required.  Currently, **CxXMLIssueTracker** uses this flag |
| incremental | false | No* | Yes| Yes | Enables support for incremental scan support when CxFlow is triggering scans.  The incremental-num-scans and incremental-threshold values must not be exceeded for the last available full scan criteria. |
| incremental-num-scans | 5 | No* | Yes | Yes (scan only) | The maximum number of scans before a full scan is required |
| project-script |          | No | Yes | Yes | A **groovy** script that can be used for deciding the name of the project to create/use in Checkmarx. This is to allow for any client custom lookups and other integrations.  The script is passed a "**request**" object, which is of type **com.checkmarx.flow.dto.ScanRequest**, and must return **String** represeting the **team name** to be used. If this script is provided, it is used for all decisions associated with the determining project name |
| team-script |            | No | Yes | Yes | A **groovy** script that can be used for deciding the team to use in Checkmarx.  This is to allow for any client custom lookups and other integrations.  The script is passed a "request" object, which is of type **com.checkmarx.flow.dto.ScanRequest**, and must return **String** representing the team path to be used. If this script is provided, it is used for all decisions associated with determining project name.
| incremental-threshold | 7 | No* | Yes | Yes (scan only) | The maximum number of days before a full scan is required |
| offline | false | No* | No | Yes (parse only) | Use Table this only when parsing Checkmarx XML, this flag removes the dependency from Checkmarx APIs when parsing results.  This skips retrieving the issue description from Checkmarx. |

No* = Default is applied

## <a name="gotchas">Gotchas!</a>
* Make sure the path to Git is configured in the Checkmarx web portal under Settings > Application Settings > General
* The guides here were written for CxSAST version 8.9. For version 9.0+ please see the 9.0 update page. For example, team path now uses / instead of \\
* Tokens have a maximum lifespan of 365 days. Having a secret rotation cycle in place is an important practice
* It is important to choose an encryption algorithm appropriate for your deployment 
* The automatic team name creation and assignment can be overwritten using the multi-tenant parameter in the .yml file