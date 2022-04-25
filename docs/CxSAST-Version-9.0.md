* [9.0 Configuration changes](#nine)
* [CxSAST v9.0 .yml Example File](#ninedotzero)
* [Checkmarx Application Service Account](#cxserviceaccount)
  * [How to create account in CxSAST](#accountCreation)
  * [Roles required for CxFlow](#rolesForCxFlow)

### <a name="nine">9.0 Configuration Changes</a>

**The Two Changes needed from 8.9:**
* Make sure to include **version: 9.0** (or higher) and **scope:  access_control_api sast_rest_api**
* The Team path must include the unix path separator **/**, the path is for example defined as follows: **/CxServer/Checkmarx/CxFlow**

```
checkmarx:
   version: 9.0
   username: xxxxx
   password: xxxxx
   client-id: resource_owner_client
   client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
   scope: access_control_api sast_rest_api
   base-url: http://cx.local
   multi-tenant: true
   configuration: Default Configuration
   cx-branch: true
   scan-preset: Checkmarx Default
   team: /CxServer/Checkmarx/CxFlow
   url: ${checkmarx.base-url}/cxrestapi
   preserve-xml: true
   incremental: true
   #WSDL Config
   portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
   #project-script: D:\\tmp\CxProject.groovy
   #team-script: D:\\tmp\CxTeam.groovy
   #exclude-files:
   #exclude-folders:
   scan-queuing: false
   scan-queuing-timeout: 720
```

### <a name="ninedotzero">CxSAST v9.0 .yml Example File</a>
```
server:
  port: ${PORT:8982}
logging:
  file:
    name: flow.log

cxflow:
  bug-tracker: JIRA
  #bug-tracker-impl:
    # - Azure
    # - Csv
    # - CxXml
    # - GitHub
    # - GitLab
    # - Rally
    # - Json
  branches:
  - main
  - dev\w+
  - release-\w+
  filter-severity:
  # - High
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  # - Urgent
  # - Confirmed
  # - To Verify
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  version: 9.0
  username: ###<cxsast_username>###
  password: ###<cxsast_password>###
  client-id: resource_owner_client
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  scope: access_control_api sast_rest_api
  base-url: http://cx.local
  #multi-tenant: true
  configuration: Default Configuration
  cx-branch: true
  #scan-preset: Checkmarx Defaul
  preserve-xml: true
  team: /CxServer/Checkmarx/CxFlow
  url: ${checkmarx.base-url}/cxrestapi
  #scan-preset: Checkmarx Default
  #incremental: true
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl
  scan-queuing: false
  scan-queuing-timeout: 720

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

bitbucket:
  webhook-token: 12345
  token: ###<bitbucketuser>###:###<bitbuckettoken>###
  url: https://api.bitbucket.org
  api-path: /2.0

github:
  webhook-token: 12345
  token: ###<githubtoken>###
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  #block-merge: true
  #error-merge: true
  #cx-summary: true

gitlab:
  webhook-token: 12345
  token: ###<gitlabtoken>###
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  #block-merge: true

jira:
  url: ###<jira url>###
  username: ###<jira user email>###
  token: ###<jira api token>###
  project: APPSEC
  issue-type: Bug
  label-prefix: < CUSTOM PREFIX NAME >
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

## Post-back Mode

The default CxFlow mode polls for results until scans are complete, but post-back mode turns the process around and puts CxFlow into an event-drive mode of operation. When using post-back mode a Checkmarx post-back-action will be added to the Checkmarx project and that action will trigger the `/postbackAction` endpoint on CxFlow. The `/postbackAction` endpoint will use information contained in the scan results to resume the results handling process.

First, it's recommended to configure CxFlow and verify if it is working correctly before enabling post-back action mode. Before you enable post-back mode, you'll need to configure a post-back action in your Checkmarx instance. Do this by openning *Settings -> Scan Settings -> Pre & Post Scan Actions*. It is important that you know the database ID for the action that will be used by CxFlow: this is easy to determine if you only create one action because it will be id 1. If you have created more then one action or deleted one and created a new one, you will need look up the ID directly in the database, but doing that is beyond the scope of this guide. This guide assumes you've created only one post-back action and that its database ID will be 1.

Do the following to create the post back action:

1. Click "Create New Action"
2. Name the action whatever you want
3. Set the "command" to cxflow.bat
4. Set the "arguments" to *[XML_output];<your-token>;<your-server-url>/postbackAction*

The *your-token* value needs to match the token value from the cx-flow section of your CxFlow `application.yml` file. The *your-server-url* value needs to point back to your Checkmarx instance. Now that the post-back-action is configured you need add the post-back scripts to the following folder:

```batch
  c:\Program Files\Checkmarx\Executables
```

The following two scripts need to be added to the Executables folder:

### `cxflow.bat`

```batch
  cd "%~dp0"
  powershell ".\cxflow.ps1" '%1' '%2' '%3'
```

### `cxflow.ps1`

```powershell
  $resultsFile = $args[0]
  $resultsFile >> $logFile
  $token = $args[1]
  $cxURL = $args[2]
  [xml]$report = Get-Content -Path $resultsFile
  $body = @{
    token = $token
    scanComments = $report.CxXMLResults.scanComments
  }
  $cxURL += "/" + $report.CxXMLResults.ScanId
  Invoke-RestMethod -Method 'Post' -Uri $cxURL -Body $body 
```

Once the inital setup is out of the way you can update your CxFlow application file so it looks like this to activate the post-back action mode:

```yaml
  checkmarx:
    # THIS ENABLES POST BACK "action" MODE
    enable-post-action-monitor: false
    # THIS SPECIFIES THE DB ID OF THE POSTBACK ACTION TO TRIGGER "required"!
    post-action-postback-id: 1
```
If you restart CxFlow post-back-action mode should be enabled.


### <a name="cxserviceaccount">Checkmarx Application Service Account</a>

#### <a name="accountCreation">How to create account in CxSAST</a>
* Access control -> Users -> Add user
* The basic details of the user needs to be filled, followed by the team and then roles for the user.

#### <a name="rolesForCxFlow">Roles required for CxFlow</a>
CxFlow requires a SAST service account to log in to the SAST APIs to crawl scans. The service account has the following requirements:

* It should be assigned at a team level that allows visibility to all projects that require crawling. Usually this is the /CxServer team but will depend on your team organization. Any projects assigned to teams above or at a sibling level of the service account's assigned team will not be visible to crawling requests.
* The service account users must be assigned the following roles:
  * **SAST Scanner** -> This role grants permissions to create and manage projects, and run scans.
  * **SAST Reviewer** -> This role grants "read only" permissions to view scan results and generate reports
* In order to use the feature of deleting projects in CxSAST when its branch is deleted from the respective SCM, the user must have the role of **SAST Data Cleaner** -> This role grants permissions to delete projects and scans.
* If there are log messages indicating 403: Forbidden when attempting to access CxSAST REST API methods, this usually indicates the user does not have appropriate roles assigned to them.
* Cx-flow uses the basic mode for authentication i.e the username and password of the user must be provided in the application.yml under `checkmarx` section, in order to create projects, scans and generating reports.

