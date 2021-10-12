![CircleCI](https://img.shields.io/circleci/build/github/checkmarx-ltd/cx-flow)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=checkmarx-ltd_cx-flow&metric=security_rating)](https://sonarcloud.io/dashboard?id=checkmarx-ltd_cx-flow)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=checkmarx-ltd_cx-flow&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=checkmarx-ltd_cx-flow)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=checkmarx-ltd_cx-flow&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=checkmarx-ltd_cx-flow)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/checkmarx-ltd/cx-flow)

## Documentation

https://github.com/checkmarx-ltd/cx-flow/wiki

## Release Notes

Please read latest features and fixes from the Release.txt file.

## Build

Refer to [Build](https://github.com/checkmarx-ltd/cx-flow/wiki/Building-CxFlow-from-the-Source).

## Execution

Refer to [Parse](https://github.com/checkmarx-ltd/cx-flow/wiki/Execution#parse) for Parse Mode. Parse mode will use the Checkmarx Scan XML as input to drive the automation.

Refer to [Batch](https://github.com/checkmarx-ltd/cx-flow/wiki/Execution#batch) for Batch Mode. Batch can be done per project, per team or entire instance. Project Results/Ad-Hoc mode retrieves the latest results for a given project under a specific team within Checkmarx and publishes issues to the configured bug tracking system.

## WebHook Web Service

Refer to [Workflow](https://github.com/checkmarx-ltd/cx-flow/wiki/Workflows) for detailed information on the workflows available.

**Branch**
Branches are applicable to the scanning platform can be specified through global configuration, URL parameter overrides and a JSON override file (both repository based and Base64 encoded URL parameter).

**Filters**
Filters help filter out unwanted issues from making it through to the bug tracking systems. Filtering can be done by Severity (High, Medium), by Category (XSS, SQL_Injection) and CWE (79, 89) numbers. Any combination of these can be leveraged.

**Preset**
The preset used within Checkmarx, which defines scanning rules, is set globally but can be overridden by URL parameters or JSON override file.

**Bug Tracking**
Bug tracking systems is specified at a global level and can be one of JIRA, GitHub, GitLab or BitBucket. In JIRA's case, you can use a standard bug, or enable an advanced bug that includes additional fields to aid with metrics for application security. The bug tracking system per project can be overridden by URL parameters or JSON override file.

**Overrides**
Overrides to global configuration are possible through URL parameters or JSON configuration file, which can be loaded from the applicable repository upon scan request or provided as a Base64 encoded value within a URL parameter.

All the configurations can also be overridden with environment variables and command line arguments. The Github token can be overridden with an Environment Variable `GITHUB_TOKEN` or command line argument `--github.token=XXXXXXX`. 

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
    - main
    - master
    - security
  filter-severity:
    - High
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://cxa.codebashing.com/courses/

checkmarx:
  # version: 9.0   # Add this if your CxSAST instance is version 9.x
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
# WSDL Config
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

```jsonc
{
  "application": "test app", // Webhook Web Service Only
  "branches": ["develop", "main"], // Webhook Web Service Only
  "incremental": true, // Webhook Web Service Only
  "scan_preset": "Checkmarx Default", // Webhook Web Service Only
  "exclude_folders": "tmp/", //Webhook Web Service Only
  "exclude_files": "*.tst", //Webhook Web Service Only
  "emails": [checkmarx, "xxxx@checkmarx.com"], // Override email addresses if email issue tracking is enabled
  "jira": { // Override JIRA specific configurations
    "project": "APPSEC", // JIRA project
    "issue_type": "Bug", // JIRA issueType
    "assignee": "admin", // Defaul assignee
    "opened_status": ["Open","Reopen"], // Transitions in JIRA workflow that represent Open status
    "closed_status": ["Closed","Done"], // Transitions in JIRA workflow that represent Closed status
    "open_transition": "Reopen Issue", // Transition to apply to re-open an issue
    "close_transition": "Close Issue", // Transition to apply to close an issue
    "close_transition_field": "resolution", // Transition field if required during closure
    "close_transition_value": "Done", // Transition value if required during closue
    "priorities": { // Override ticket priorities
    "High": "High",
    "Medium": "Medium",
    "Low": "Low"
  },
  "fields": [ // JIRA custom field mappings. See field mapping details above
    {
    "type": "cx", //cx, static, result.
    "name": "xxx",
    "jira_field_name": "xxxx",
    "jira_field_type": "text", // security text | label | single-select | multi-select
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
 "filters": { // Override filters used when determining whether a result from Checkmarx will be tracked with a defect
   "severity": ["High", "Medium"],
   "cwe": ["79", "89"],
   "category": ["XSS_Reflected", "SQL_Injection"],
   "status": ["New", "Reoccured"],
   "state": ["Urgent", "Confirmed"]
  }
}
```

All overrides are optional. If a value is not provided, the default provided in the `application.yml` file is used. If a value is provided with an empty attribute, it is overridden as empty. i.e. if severity is high, medium within the default configuration and an empty filter attribute is provided, it will no long apply any filters ("filters":{})

## Post-back Mode

The default CxFlow mode polls for results until scans are complete, but post-back mode turns the process around and puts CxFlow into an event-drive mode of operation. When using post-back mode a Checkmarx post-back-action will be added to the Checkmarx project and that action will trigger the `/postbackAction` endpoint on CxFlow. The `/postbackAction` endpoint will use information contained in the scan results to resume the results handling process.

First, it's recommended to configure CxFlow and verify if it is working correctly before enabling post-back action mode. Before you enable post-back mode, you'll need to configure a post-back action in your Checkmarx instance. Do this by openning *Settings -> Scan Settings -> Pre & Post Scan Actions*. It is important that you know the database ID for the action that will be used by CxFlow: this is easy to determine if you only create one action because it will be id 1. If you have created more then one action or deleted one and created a new one, you will need look up the ID directly in the database, but doing that is beyond the scope of this guide. This guide assumes you've created only one post-back action and that its database ID will be 1. 

Do the following to create the post back action:

1. Click "Create New Action"
2. Name the action whatever you want
3. Set the "command" to cxflow.bat
4. Set the "arguments" to *[XML_output];<your-token>;<your-server-url>/postbackAction*

The *your-token* value needs to match the token value from the cx-flow section of your CxFlow `application.yml` file. The *your-server-url* value needs to point back to your Checkmarx instance. Now that the post-back action is configured you need add the post-back scripts to the following folder:
  
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
  
If you restart CxFlow, the post-back action mode should be enabled.

## Source
**Packages**

|Package                     |	Description|
-----------------------------|---------
com.checkmarx.flow.config    |	All bean configurations and Property file POJO mappings.
com.checkmarx.flow.controller|	All HTTP Endpoints.  GitHub/GitLab/Bitbucket WebHook services.
com.checkmarx.flow.dto       |	Sub-packages contain all DTO objects for Checkmarx, GitHub, GitLab, etc.
com.checkmarx.flow.exception |	Exceptions
com.checkmarx.flow.filter    |	Specify any filters applied to Web Traffic flow. Currently passthrough, but can be used for IP filtering.
com.checkmarx.flow.service   |	Core logic. Each Issue tracker has a Service along with a main flowService, which drives the overall flow.
com.checkmarx.flow.utils     |	Utilities package

**Services**

|Service	      |Description|
----------------|----------
CxService       |	SOAP Based API Client for Checkmarx
CxLegacyService |	REST Based API Client for Checkmarx
JiraIssueService|	REST Based API Client for Jira (JRJC - Jira Java REST Client)
GitHubService   |	REST Based API Client GitHub
GitLabService   |	REST Based API Client GitLab
EmailService    |	Email (SMTP) client
flowService     |	Main Service driving integrations with other Service components
BitbucketService|	TBD, this is not created yet.

**Controllers (WebHook Web Service Only)**

|Controller        |	Description|
-------------------|---------
GitHubController   |	Ping, Push, Pull  (TBD) event HTTP listeners
GitLabController   |	Push, Merge (TBD) event HTTP listeners
BitbucketController|	Push event HTTP listener
flowController     |	Unused, but intended for Call-back implementation

## Contributing

See [Development Operations](https://checkmarx.atlassian.net/wiki/spaces/PTS/pages/1325007123/Development+Operations) (Checkmarx employees only)
