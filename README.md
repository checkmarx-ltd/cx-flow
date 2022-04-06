![CircleCI](https://img.shields.io/circleci/build/github/checkmarx-ltd/cx-flow)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=checkmarx-ltd_cx-flow&metric=security_rating)](https://sonarcloud.io/dashboard?id=checkmarx-ltd_cx-flow)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=checkmarx-ltd_cx-flow&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=checkmarx-ltd_cx-flow)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=checkmarx-ltd_cx-flow&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=checkmarx-ltd_cx-flow)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/checkmarx-ltd/cx-flow)

## Documentation
https://github.com/checkmarx-ltd/cx-flow/wiki

## Release Notes
Please read latest features and fixes from the Release.txt file

## Build

Refer to [Build](https://github.com/checkmarx-ltd/cx-flow/wiki/Building-CxFlow-from-the-Source)

## Execution

Refer to [Parse](https://github.com/checkmarx-ltd/cx-flow/wiki/Execution#parse), for Parse Mode.  Parse mode will use the Checkmarx Scan XML as input to drive the automation.

Refer to [Batch](https://github.com/checkmarx-ltd/cx-flow/wiki/Execution#batch), for Batch Mode.  Batch can be done per project, per team or entire instance.  Project Results/Ad-Hoc mode retrieves the latest results for a given project under a specific team within Checkmarx and publishes issues to the configured bug tracking system.

## WebHook Web Service

Refer to [Workflow](https://github.com/checkmarx-ltd/cx-flow/wiki/Workflows) for detailed information on the workflows available.

**Branch**
Branches are applicable to the scanning platform can be specified through global configuration, URL parameter overrides and a JSON override file (both repository based and Base64 encoded URL parameter).

**Filters**
Filters help filter out unwanted issues from making it through to the bug tracking systems. Filtering can be done by Severity (High, Medium) , by Category (XSS, SQL_Injection) and CWE (79, 89) numbers.  Any combination of these can be leveraged.

**Preset**
The preset used within Checkmarx, which defines scanning rules, is set globally but can be overridden by URL parameters or JSON override file. 

**Bug Tracking**
Bug tracking systems is specified at a global level and can be one of JIRA, GitHub, GitLab or BitBucket.  In JIRA's, you can use a standard bug, or enable an advanced bug that includes additional fields to aid with metrics for application security.  The bug tracking system per project can be overridden by URL parameters or JSON override file.

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

## Source
**Packages**

|Package|	Description|
---------|---------
com.checkmarx.flow.config|	All bean configurations and Property file POJO mappings.
com.checkmarx.flow.controller|	All HTTP Endpoints.  GitHub/GitLab/Bitbucket WebHook services.
com.checkmarx.flow.dto|	Sub-packages contain all DTO objects for Checkmarx, GitHub, GitLab, etc.
com.checkmarx.flow.exception|	Exceptions
com.checkmarx.flow.filter|	Specify any filters applied to Web Traffic flow.  Currently passthrough, but can be used for IP filtering.
com.checkmarx.flow.service|	Core logic.  Each Issue tracker has a Service along with a main flowService, which drives the overall flow.
com.checkmarx.flow.utils|	Utilities package

**Services**

|Service	|Description|
--------|----------
CxService|	SOAP Based API Client for Checkmarx
CxLegacyService|	REST Based API Client for Checkmarx
JiraIssueService|	REST Based API Client for Jira (JRJC - Jira Java REST Client)
GitHubService|	REST Based API Client GitHub
GitLabService|	REST Based API Client GitLab
EmailService|	Email (SMTP) client
flowService|	Main Service driving integrations with other Service components
BitbucketService|	TBD, this is not created yet.

**Controllers (WebHook Web Service Only)**

|Controller|	Description|
------|---------
GitHubController|	Ping, Push, Pull  (TBD) event HTTP listeners
GitLabController|	Push, Merge (TBD) event HTTP listeners
BitbucketController|	Push event HTTP listener
flowController|	Unused, but intended for Call-back implementation

## Contributing

See [Development Operations](https://checkmarx.atlassian.net/wiki/spaces/PTS/pages/1325007123/Development+Operations) (Checkmarx employees only)
