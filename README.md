#Build
`gradlew clean build`


#Parse

_Parse mode will use the Checkmarx Scan XML as input to drive the automation_

```
java -jar ${AUTOMATION_JAR} \
--spring.config.location=${APPLICATION_YML} \
--parse \
--namespace=Custodela \
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

#Project Results (ad-hoc)
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

#Batch 

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
--namespace	| Repository group (Gitlab)/organization (Github)/namesapce (BitBucket). Used as higher level grouping of repositories.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed.  These values are stored in a Tracking label within Jira.  This value is also stored in the body of the issue.
--repo-name	| Name of the repository.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed (--app).  These values are stored in a Tracking label within Jira.  This value is also stored in the body of the issue.
--branch |	Branch Used along with repo-name and branch for tracking purposes (Jira Only).  If these 3 are not present, then application attribute must be passed  (--app).  These values are stored in a Tracking label within Jira. This value is also stored in the body of the issue.
--app |	Alternatively used for Tracking purposes within Jira.  This value is also stored in the body of the issue.
--repo-url	| Required if issues tracking with GitHub Issues or GitLab Issues.  This value is also stored in the body of the issue.
--bug-tracker |	Optional.  Default is whatever is specified in the application.yml properties.  Options are github, gitlab, jira, email, none
--config |	Optional.  Configuration override file (JSON).  See details below.

#WebHook Web Service

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
  port: ${PORT:8080} #If WebHook Web Service is used, this will specify the port the Web Service listens on

logging:
  file: machina.log #specify log file location.  Log file rotates daily

machina:
  bug-tracker: JIRA #specify default bug tracker - GITHUB, GITHUB, JIRA
  filter-severity: #specify which issues are to be tracked with bug tracking based on severity from checkmarx (High, Medium, Low)
    - Critical
    - High
  filter-category: #specify the categories within Checkmarx results to track via bug tracker (SQL_Injection, XSS_Reflected, etc)
  filter-cwe: #specify the cwe within Checkmarx results to track via bug tracker (79, 89, etc)
  filter-status: #specify the Issue status in Checkmarx results to track via bug tracker (New, Confirmed, etc)
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html #used within recommendation link (cwe based)
  wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance #Custom organization specific wiki/guidance link
  codebash-url: https://cxa.codebashing.com/courses/
  mail: #specify if email is enabled (default turned off for command line mode)
    enabled: false

checkmarx:
  username: xxxx
  password: xxxx
  client-secret: xxxx #OIDC API Client Secret
  base-url: http://checkmarx.local #Base URL for checkmarx
  url: ${checkmarx.base-url}/cxrestapi
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl

github:
  token: xxxx #API token for GitHub integration - must have access to read repositories and create issues
  webhook-token: xxxx #Used as WebHook Shared secret in Web Service
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive

gitlab:
  token: #API token for GitLab integration - must have access to read repositories and create issues
  webhook-token: xxxx #Used as WebHook Shared secret in Web Service
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive

bitbucket:
  webhook-token: XXXX
  token: xxx
  url: http://452ad9ac.ngrok.io
  api-path: /rest/api/1.0
  false-positive-label: false-positive

jira:
 url: https://xxxx.atlassian.net #Base URL for JIRA
 username: xxxx #JIRA username
 token: xxxx #JIRA api token or password
 project: APPSEC #Default/Global Project
 issue-type: Application Security Bug #Default/Global IssueType
 priorities: #JIRA priorities
 Critical: Highest
High: High
Medium: Medium
Low: Low
informational: Lowest
open-transition: Reopen Issue #Transition to re-open an issue
 close-transition: Close Issue #Transition to close an issue
 close-transition-field: resolution
close-transition-value: Done
open-status: #Statuses that represent an open issue
 - Open
- In Progress
- Reopened
closed-status: #Statuses that represent a closed issue
 - Closed
- Resolved
fields: #Custom Field Mappings
 - type: result
name: application
jira-field-name: Application
jira-field-type: label
- type: result
name: repo-url
jira-field-name: Source Code Repository
jira-field-type: text
- type: result
name: branch
jira-field-name: Branch/Tag Details
jira-field-type: text
- type: result
name: severity
jira-field-name: Severity
jira-field-type: single-select
- type: result
name: recommendation
jira-field-name: Recommendations
jira-field-type: text
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
jira-field-name: Line Numbers
jira-field-type: label
- type: result
name: issue-link
jira-field-name: Issue URL
jira-field-type: text
- type: static
name: identified-by
jira-field-name: Identified By
jira-field-type: single-select
jira-default-value: Automation
```

Note: All of the above configurations can be overridden with environment variables and command line arguments.  The github token can be overridden with:
`
Environment variable GITHUB_TOKEN
`
or Command line argument `--github.token=XXXXXXX`

#Jira Configuration
**Jira Custom Fields (Command line)**
**type**
*static:* Used for static values (specifically requires jira-default-value to be provided)
*cx:* Used to map specific Checkmarx Custom Field value
*result:* Used to map known values from checkmarx results or repository/scan request details.  See Result values below. |

**name**
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

**jira-field-name**	Custom field name in Jira (readable name, not custom field ID)
**jira-field-type** Type of custom field in JIRA:

*label* (if using static or cx values, csv format is used and broken into multiple labels)
*text* (applies to many custom field types: url, text box, text, etc
*multi-select* (csv format is used and broken into multiple select values)
*single-select*
*security* (used for issue security levels)
*jira-default-value*	Static value if no value can be determined for field (Optional)

#Override Files
When providing --config override file you can override many elements associated with the bug tracking within Jira.

```json
{
"application": "test app", //WebHook Web Service Only
"branches": ["develop", "master"], //WebHook Web Service Only
"incremental": true, //WebHook Web Service Only
"scan_preset": "Checkmarx Default", //WebHook Web Service Only
"exclude_folders": "tmp/", //WebHook Web Service Only
"exclude_files": "*.tst", //WebHook Web Service Only
"emails": ["xxxx@custodela.com", "xxxx@checkmarx.com"], //Override email addresses if email issue tracking is enabled
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
 "status": ["Confirmed", "New"]
}
}
```
All overrides are optional.  If a value is not provided, the default provided in the application.yml is used.  If a value is provided with an empty attribute, it is overridden as empty.  i.e. if severity is high, medium within the default configuration and an empty filter attribute is provided, it will no long apply any filters ("filters":{})


#Source
**Packages**
|Package|	Description|
---------|---------
com.custodela.machina.config|	All bean configurations and Property file POJO mappings.
com.custodela.machina.controller|	All HTTP Endpoints.  GitHub/GitLab/Bitbucket WebHook services.
com.custodela.machina.dto|	Sub-packages contain all DTO objects for Checkmarx, GitHub, GitLab, etc.
com.custodela.machina.exception|	Exceptions
com.custodela.machina.filter|	Specify any filters applied to Web Traffic flow.  Currently passthrough, but can be used for IP filtering.
com.custodela.machina.service|	Core logic.  Each Issue tracker has a Service along with a main MachinaService, which drives the overall flow.
com.custodela.machina.utils|	Utilities package

**Services**
|Service	|Description|
--------|----------
CxService|	SOAP Based API Client for Checkmarx
CxLegacyService|	REST Based API Client for Checkmarx
JiraIssueService|	REST Based API Client for Jira (JRJC - Jira Java REST Client)
GitHubService|	REST Based API Client GitHub
GitLabService|	REST Based API Client GitLab
EmailService|	Email (SMTP) client
MachinaService|	Main Service driving integrations with other Service components
BitbucketService|	TBD, this is not created yet.

**Controllers (WebHook Web Service Only)**
|Controller|	Description|
------|---------
GitHubController|	Ping, Push, Pull  (TBD) event HTTP listeners
GitLabController|	Push, Merge (TBD) event HTTP listeners
BitbucketController|	Push event HTTP listener
MachinaController|	Unused, but intended for Call-back implementation

#Build
Executable JAR is compiled using Gradle (tested with version 4.10 and 5.0)

**Java 8 JRE - WebHook Web Service:**
`gradle --build-cache assemble`

**Java 8 JRE - CLI:**
```
cp cmd/MachinaApplication.java src/main/java/com/custodela/machina/
gradle -b build-cmd.gradle --build-cache assemble
```

*Note: the cmd folder holds the CLI version of the application  (this is due to the fact that Spring boot cannot have 2 application contexts under the same package structure.  
This was the approach to keep a single code base.*

**Java 11 JRE - WebHook Web Service:**
`gradle -b build-11.gradle --build-cache assemble`

**Java 11 JRE - CLI:**
```
cp cmd/MachinaApplication.java src/main/java/com/custodela/machina/
gradle -b build-cmd-11.gradle --build-cache assemble
```

*Note: the cmd folder holds the CLI version of the application (this is due to the fact that Spring boot cannot have 2 application contexts under the same package structure.
This was the approach to keep a single code base.*
