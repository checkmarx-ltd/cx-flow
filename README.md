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
`gradlew clean build`

## Parse



_Parse mode will use the Checkmarx Scan XML as input to drive the automation_

```
java -jar ${AUTOMATION_JAR} \
--spring.config.location=${APPLICATION_YML} \
--parse \
--namespace=checkmarx \
--repo-name=Riches.NET \
--repo-url=https://github.com/Custodela/Riches.NET.git \
--branch=master \
--app=Riches.NET \
--f=Checkmarx/Reports/ScanReport.xml \
```

| Option | Description |
---------|-------------|
--spring.config.location|	Override the main application.yml/properties file for the application.  Defaults to the application.yml packaged within the jar
--parse	| Indicates that a result XML file from Checkmarx will be provided (–f will also be mandatory)
--namespace |	Repository group (Gitlab)/organization (Github)/namesapce (BitBucket). Used as higher level grouping of repositories.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed.  These values are stored in a Tracking label within Jira.  This value is also stored in the body of the issue.
--repo-name	| Name of the repository.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed (--app).  These values are stored in a Tracking label within Jira.  This value is also stored in the body of the issue.
--branch |	Branch Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed  (--app).  These values are stored in a Tracking label within Jira. This value is also stored in the body of the issue.
--app |	Alternatively used for Tracking purposes within Jira.  This value is also stored in the body of the issue.
--repo-url	| Required if issues tracking with GitHub Issues or GitLab Issues.  This value is also stored in the body of the issue.
--f	| File to be processed.  This the output from Checkmarx CLI, Jenkins/Bamboo Plugin, etc
--config |	Optional.  Configuration override file (JSON).  See details below.
--bbs | Optional.  Indicates the repository is of type BitBucket Server
--bb | Optional.  Indicates the repository is of type BitBucket Cloud

## Project Results (ad-hoc)
_Project Results/Ad-hoc mode retrieves the latest results for a given project under a specific team within Checkmarx and publishes issues to the configured bug tracking system._


```
java -jar ${AUTOMATION_JAR} \
--spring.config.location=${APPLICATION_YML} \
--project \
--cx-team="CxServer\SP\Checkmarx\custodela-test" \
--cx-project="riches-master" \
--namespace=Custodela \
--repo-name=Riches.NET \
--repo-url=https://github.com/Custodela/Riches.NET.git \
--branch=master \
--app=Riches.NET \
```

## Batch 

**By Team**
```
java -jar ${AUTOMATION_JAR} \
--spring.config.location=${APPLICATION_YML} \
--batch --cx-team="CxServer\SP\Checkmarx\development"
```

**Entire Checkmarx Instance**
```
java -jar ${AUTOMATION_JAR} \
--spring.config.location=${APPLICATION_YML} \
--batch
```


|Option	|Description|
-----|----|
--spring.config.location |	Optional.  Override the main application.yml/properties file for the application.  Defaults to the application.yml packaged within the jar
--project|	Indicates that this will be a project results request.
--cx-team|	Team within Checkmarx
--cx-project |	Project under the specify team that the latest results should be pulled for
--alt-project | Project under Azure ADO that should recieve the workitem
--alt-fields | Add additional fields to the workitem. Format is field-name1:field-value1,field-name2:field-value2,etc. Wrap in double quotes if any values contains spaces.
--namespace	| Repository group (Gitlab)/organization (Github)/namesapce (BitBucket). Used as higher level grouping of repositories.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed.  These values are stored in a Tracking label within Jira.  This value is also stored in the body of the issue.
--repo-name	| Name of the repository.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed (--app).  These values are stored in a Tracking label within Jira.  This value is also stored in the body of the issue.
--branch |	Branch Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed  (--app).  These values are stored in a Tracking label within Jira. This value is also stored in the body of the issue.
--app |	Alternatively used for Tracking purposes within Jira.  This value is also stored in the body of the issue.
--repo-url	| Required if issues tracking with GitHub Issues or GitLab Issues.  This value is also stored in the body of the issue.
--bug-tracker |	Optional.  Default is whatever is specified in the application.yml properties.  Options are github, gitlab, jira, email, none
--config |	Optional.  Configuration override file (JSON).  See details below.

## WebHook Web Service

**Workflow**

<li>Webhook is registered at the namespace level (aka group, organization) or at the individual project/repository level within GitLab, GitHub, or Bitbucket using a shared key/token and pointing to the Automation Service
Developer commit's code (PUSH Request)
<li>WebHook fires a request to the Service along with commit details
<li>All developers identified within the commit(s) of a PUSH request are notified via email that the scan has been submitted (note: email can be disabled)
<li>Service determines if the branch is applicable to the service (see Branch details below)
<li>Service creates a new team (if multi-tenant mode is on) and a project for the particular organization/repository within Checkmarx.  If a project already exists with the same name, the existing project is used
<li>Project is set to use specific scanning rules (Preset)
<li>Repository details are updated in the project within Checkmarx
<li>Scan request is submitted for the project in Checkmarx
<li>Service monitors the state of the scan, and waits until it is finished
<li>Once scan is finished, a report is generated and retrieved by the Service
<li>Findings are filtered based on defined criteria (see Filter details below)
<li>Service sends an email notification to all committers that scan is complete
<li>Email includes direct links to issues based on Filtering (optional)
<li>Service publishes findings to defined Bug Tracking tool
<li>Issues are collapsed (multiple issues of the same type in the same file are updated within 1 ticket) - See Bug Tracking details below
<li>Tickets are closed if the issue is remediated on next iteration of scan
<li>Tickets are re-opened in the event an issue is reintroduced
<li>All references within a ticket must be addressed before the Ticket is closed

**Branch**
Branches are applicable to the scanning platform can be specified through global configuration, url parameter overrides, JSON override file (both repository based and Base64 encoded url parameter)

**Filters**
Filters help filter out unwanted issues from making it through to the bug tracking systems.  Filtering can be done by Severity (High, Medium) , Category (XSS, SQL_Injection) and CWE (79, 89) numbers.  Any combination of these can be leveraged.

**Preset**
The preset used within Checkmarx, which defines scanning rules, is set globally but can be overridden by URL parameters or JSON override file 

**Bug Tracking**
Bug tracking systems is specified at a global level and can be one of JIRA, GitHub, GitLab or BitBucket.  In the case of JIRA, you can use a standard bug, or enable an advanced bug that includes additional fields to aid with metrics for application security.  The bug tracking system per project can be overridden by URL parameters or JSON override file.

**Overrides**
Overrides to global configuration are possible through URL parameters or JSON configuration file, which can be loaded from the applicable repository upon scan request or provided as a base64 encoded value within a URL parameter.

See configuration/Override details below
```yaml
Configuration(s)
server:
  port: ${PORT:8080}

logging:
  file: cx-flow.log
#  level:
#    com:
#      checkmarx:
#        flow:
#          service: TRACE
#    org:
#      apache:
#        http:
#          wire: TRACE
#      springframework:
#        web:
#          client:
#            RestTemplate: TRACE

cx-flow:
  bug-tracker: JIRA
  bug-tracker-impl:
    - CxXml
    - Json
    - GitLab
    - GitHub
    - Csv
  branches:
    - develop
    - master
    - security
  filter-severity:
    - High
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://cxa.codebashing.com/courses/

checkmarx:
  username: xxx
  password: xxx
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: http://localhost:8100
  url: ${checkmarx.base-url}/cxrestapi
  multi-tenant: true
  incremental: true
  scan-preset: Checkmarx Default
  configuration: Default Configuration
  team: \CxServer\SP\Checkmarx
  scan-timeout: 120
#WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl
  portal-package: checkmarx.wsdl.portal
  preserve-xml: true
  jira-project-field:
  jira-custom-field:
  jira-issuetype-field:
  jira-assignee-field:

jira:
  url: http://localhost:8180
  username: xxxx
  token: xxxx
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
    - To Do
    - In Progress
  closed-status:
    - Done
  fields:
    - type: result
      name: system-date
      skip-update: true
      offset: 60
      jira-field-name: Due Date #Due date (cloud)
      jira-field-type: text
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: severity
      jira-field-name: Severity
      jira-field-type: single-select
    - type: result
      name: loc
      jira-field-name: Line Numbers
      jira-field-type: label
    - type: static
      name: identified-by
      jira-field-name: Identified By
      jira-field-type: single-select
      jira-default-value: Automation
    - type: static
      name: dependencies
      jira-field-name: Dependencies
      jira-field-type: multi-select
      jira-default-value: Java, AngularJS

github:
  webhook-token: 1234
  token: xxx
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true

gitlab:
  webhook-token: 1234
  token: xxx
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  block-merge: true

azure:
  webhook-token: cxflow:1234
  token: xxxx
  url: https://dev.azure.com
  api-url: https://dev.azure.com
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  block-merge: true

json:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].json"
  data-folder: "D:\\tmp"

cx-xml:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
  data-folder: "D:\\tmp"

csv:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].csv"
  data-folder: "D:\\tmp"
  include-header: true
  fields:
    - header: Customer field (Application)
      name: application
      default-value: unknown
    - header: Primary URL
      name: static
      default-value: ${tmp.url}
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
      name: summary
      prefix: "*"
      postfix: "*"
    - header: Severity
      name: severity
    - header: recommendation
      name: recommendation
```

Note: All of the above configurations can be overridden with environment variables and command line arguments.  The github token can be overridden with:
`
Environment variable GITHUB_TOKEN
`
or Command line argument `--github.token=XXXXXXX`

## Jira Configuration
**Jira Custom Fields**
* *type*:
  * static: Used for static values (specifically requires jira-default-value to be provided)
  * cx: Used to map specific Checkmarx Custom Field value
  * result: Used to map known values from checkmarx results or repository/scan request details.  See Result values below. |

* *name*:
When cx is the type, this is the name of the custom field within Checkmarx

When result is provided it must be one of the following:

        application - Command line option --app
        project - Command line option --cx-project
        namespace - Command line option --namespace
        repo-name - Command line option --repo-name
        repo-url - Command line option --repo-url
        branch - Command line option --branch
        severity - Severity of issue in Checkmarx
        category - Category of issue in Checkmarx
        cwe - CWE of issue in Checkmarx
        recommendation - Recommendation details based on Mitre/Custom Wiki
        loc - csv of lines of code
        issue-link - Direct link to issue within Checkmarx
        filename - Filename provided by Checkmarx issue
        language - Language provided by Checkmarx issue
        similarirty-id - Checkmarx Similarity ID
        system-date - Current system time

* jira-field-name	Custom field name in Jira (readable name, not custom field ID)
* jira-field-type** Type of custom field in JIRA:
  * label (if using static or cx values, csv format is used and broken into multiple labels)
  * text (applies to many custom field types: url, text box, text, etc
  * multi-select (csv format is used and broken into multiple select values)
  * single-select
  * security (used for issue security levels)
  * component (used for build in Jira Component/s field)
  * jira-default-value	Static value if no value can be determined for field (Optional)
* *skip-update*: The value is only provided during the initial creation of the ticket and not updated during subsequent iterations
* *offset*: Used with system-date, the value of offset is added to the system date

## Override Files
When providing --config override file you can override many elements associated with the bug tracking within Jira.

```json
{
"application": "test app", //WebHook Web Service Only
"branches": ["develop", "master"], //WebHook Web Service Only
"incremental": true, //WebHook Web Service Only
"scan_preset": "Checkmarx Default", //WebHook Web Service Only
"exclude_folders": "tmp/", //WebHook Web Service Only
"exclude_files": "*.tst", //WebHook Web Service Only
"emails": [checkmarx, "xxxx@checkmarx.com"], //Override email addresses if email issue tracking is enabled
 "jira": { //override JIRA specific configurations
"project": "APPSEC", //JIRA project
 "issue_type": "Bug", // JIRA issueType
 "assignee": "admin", // Defaul assignee
 "opened_status": ["Open","Reopen"], //Transitions in JIRA workflow that represent Open status
 "closed_status": ["Closed","Done"], //Transitions in JIRA workflow that represent Closed status
 "open_transition": "Reopen Issue", //Transition to apply to re-open an issue
 "close_transition": "Close Issue", //Transition to apply to close an issue
 "close_transition_field": "resolution", //Transition field if required during closure
 "close_transition_value": "Done", //Transition value if required during closue
 "priorities": { //Override ticket priorities
"High": "High",
 "Medium": "Medium",
 "Low": "Low"
 },
 "fields": [ //JIRA custom field mappings. See field mapping details above
{
"type": "cx", //cx, static, result.
 "name": "xxx",
 "jira_field_name": "xxxx",
 "jira_field_type": "text", //security text | label | single-select | multi-select
 "jira_default_value": "xxx"
 },
 {
"type": "result",
 "name": "xxx",
 "jira_field_name": "xxxx",
 "jira_field_type": "label"
 },
 {
"type": "static",
 "name": "xxx",
 "jira_field_name": "xxxx",
 "jira_field_type": "label",
 "jira_default_value": "xxx"
 }
]
},
 "filters" : { //Override filters used when determining whether a result from Checkmarx will be tracked with a defect
"severity": ["High", "Medium"],
 "cwe": ["79", "89"],
 "category": ["XSS_Reflected", "SQL_Injection"],
 "status": ["New", "Reoccured"],
 "state": ["Urgent", "Confirmed"]
}
}
```
All overrides are optional.  If a value is not provided, the default provided in the application.yml is used.  If a value is provided with an empty attribute, it is overridden as empty.  i.e. if severity is high, medium within the default configuration and an empty filter attribute is provided, it will no long apply any filters ("filters":{})

## Post-back Mode

The default CxFlow mode polls for results until scans are complete, but post-back mode turns the process around and puts CxFlow into an event-drive mode of operation. When using post-back mode a Checkmarx post-back-action will be added to the Checkmarx project and that action will trigger the /postbackAction endpoint on CxFlow. The /postbackAction endpoint will use information contained in the scan results to resume the results handling process.

First, I recommend you configured CxFlow and verify it is working correctly before enabling post-back-action mode. Before you enable post-back mode you need to configure a post-back-action in your Checkmarx instance. Do this by openning settings -> scan settings -> Pre & Post Scan Actions. It is important that you know the database ID for the action that will be used by CxFlow, this is easy to determine if you only create one action because it will be 1. If you have created more then one action or deleted one and created a new one you will need look up the ID directly in the database, but doing that beyond the scope of this guide. This guide assumes you've created only one postback action and that its database ID will be 1. 

Do the following to create the post back action:

1. Click "Create New Action"
2. Name the action whatever you want
3. Set the "command" to cxflow.bat
4. Set the "arguments" to *[XML_output];<your-token>;<your-server-url>/postbackAction*

The *your-token* value needs to match the token value from the cx-flow section of your CxFlow application.yml file. The *your-server-url* value needs to point back to your Checkmarx instance. Now that the post-back-action is configured you need add the post-back scripts to the following folder
  
```batch
  c:\Program Files\Checkmarx\Executables
```
  
The following two scripts need to be added to the Executables folder:

cxflow.bat

```batch
  cd "%~dp0"
  powershell ".\cxflow.ps1" '%1' '%2' '%3'
```

cxflow.ps1

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

Once the inital setup is out of the way you can update your CxFlow application file so it looks like this to activate the post-back-action mode:

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

## Build
Executable JAR is compiled using Gradle (tested with version 4.10 and 5.0)

**Java 8 JRE:**
`gradle --build-cache assemble`

**Java 11 JRE:**
`gradle -b build-11.gradle --build-cache assemble`

## Contributing

See [Development Operations](https://checkmarx.atlassian.net/wiki/spaces/PTS/pages/1325007123/Development+Operations) (Checkmarx employees only)
