* [Main (Global) Properties](#main)
  * [Yaml Sample](#yaml)
  * [CLI Examples](#cli)
  * [Environment Variable Examples](#env)
* [Configuration Definitions](#configuration)
  * [Server Section](#server)
  * [Cx-Flow Section](#cxflow)
    * [E-Mail notifications](#email)
    * [Filtering](#filtering)
    * [Excluding Vulnerability](#excludeFilter)
    * [Excluding Files from Zip Archive](#excludezip)
    * [Break build](#break)
  * [Checkmarx Section](#checkmarx)
    * [9.0 Configuration Changes](#nine)
    * [Postback action](#postback)
    * [Override SAST project setting](#override)
    * [Create Branched Project](#branchproject)
    * [Scan Queuing and Scan Queuing Timeout](#scanqueuing)
    * [Scan Timeout and Scan polling](#scantimeoutandscanpolling)
    * [Report Timeout and Report polling](#reporttimeoutandreportpolling)
    * [Checkmarx E-mail Notifications](#emailnotifications)
* [WebHook Configuration](#webhook)
  * [WebHook URL Parameters - Code](#code)
  * [WebHook URL Override Parameters - Details](#details)
* [Repository configuration blocks](#repository)
  * [GitHub](#github)
  * [GitLab](#gitlab)
  * [Azure DevOps](#azure)
  * [Sarif](#sarif)
  * [Bitbucket (Cloud and Server)](#bitbucket)
* [JSON Config Override](#json)
* [BugTrackers](#bugtrackers)
* [Encryption](#encryption)
* [External Scripting](#external)
* [SAST Scan ID in Github Action Output variable](#outputscanid)
* [Streaming CxFlow logs to AWS OpenSearch or ElasticSearch](#awslogs)
* [Issue Labels](#issuelbls)
* [Jasypt](#jasypt)

CxFlow uses **Spring Boot** and for Server Mode, it requires an `application.yml` file to drive the execution. The sections below outlines available properties and when/how they can be used in different execution modes. In addition, all the Spring Boot configuration rules apply. For additional information on Spring Boot, refer to
https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html

Command-line arguments and environment variables prevail over values specified in `application.yml` file. To allow for bootstrapping the launch process with various configurations, especially with containers, CxFlow uses overrides on the command line using the `--property.name=Value` format as well as `PROPERTY_NAME` environment variable overrides.

All the relevant configuration is defined by the `application.yml` file that resides in the same directory as the JAR file, or if an explicit configuration override is provided on the command line as follows:

```bash
$ java -jar cx-flow-<version>.jar \
    --spring.config.location=/path/to/application.yml
```

## <a name="main">Main (Global) Properties</a>

### <a name="yaml">Yaml Sample</a>
Please refer to the sample configuration for the entire YAML structure.
```yaml
server:
  port: ${PORT:8080}

logging:
  file: flow.log

cx-flow:
  contact: admin@cx.com
  bug-tracker: Json
  bug-tracker-impl:
    - CxXml
    - Csv
    - Json
    - GitLab
    - GitHub
    - Azure
    - Rally
  branches:
    - develop
    - main
    - release`-\w+ # regular expressions supported. If branch-script is provided, this is ignored. branch-script: D:\\tmp\Branch.groovy #default empty/not used
  filter-severity:
    - High
  filter-category:
    - Stored_XSS
    - SQL_Injection
  filter-cwe:
    - 89
    - 79
  filter-status:
    - New
    - Recurrent
  filter-state:
    - Confirmed
    - Urgent
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  deleteForkedProject: true
  project-custom-field: "git:git test,test:test1"
  scan-custom-field: "git:git test,test:test1"
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  track-application-only: false
  web-hook-queue: 20
  scan-result-queue: 8
  break-build: false
  scan-resubmit: false
  branch-protection-enabled: false
  preserve-project-name: false
  http-connection-timeout: xxx # milliseconds - default 30000
  http-read-timeout: xxx # milliseconds - default 120000
  mail:
    host: smtp.gmail.com
    port: 587
    username: xxx
    password: xxx
    enabled: true
    notification: true # default is false
    cc: myemail@mycompany.com # comma-separated list of e-mails
  zip-exclude: \.git/.*, .*\.png
  zip-include: \.git/.*, .*\.png
  comment-script: location/to/commentScript.groovy

checkmarx:
  version: 9.0 # Not required for CxSAST version 8.x
  username: xxx
  password: xxx
  client-secret: xxx
  base-url: https://cx.aws.checkmarx.com
  multi-tenant: true
  scan-preset: Checkmarx Default
  configuration: Default Configuration
  team: /CxServer/SP/Machina # \CxServer\SP\Machina for CxSAST 8.x
  scan-timeout: 120 # Webhook and --scan command line only, number of minutes
  scan-polling: 20000
  report-timeout: 300000
  report-polling: 5000
  preserve-xml: true
  enabled-zip-scan: false
  url: ${checkmarx.base-url}/cxrestapi
  # WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl
  project-script: D:\\tmp\CxProject.groovy # default empty/not used
  team-script: D:\\tmp\CxTeam.groovy # default empty/not used
  custom-state-map:
    "5": "SUSPICIOUS"
  custom-state-false-positive-map:
    "5": "SUSPICIOUS"
  modify-branch-name-by-pattern-map:
    "[[^a-zA-Z0-9-_.]+]": "_" # Key is having regular expression
    # "[/]": "_"   #Use this expression if you want to replace / by underscore 
  post-action-postback-id: 123456
  settings-override: true #default false if not provide
  cx-branch: false
  scan-queuing: true
  scan-queuing-timeout: 720 # Webhook and --scan command line only, number of minutes
  email-notifications:
    after-scan:
      - user1@example.com
    before-scan:
      - user2@example.com
    failed-scan:
      - user3@example.com
  project-branching-check-count: 5
  project-branching-check-interval: 10
  restrict-results-to-branch: true

github:
  webhook-token: XXXXX
  token: XXXXX
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true
  cx-summary-header: Checkmarx Scan Summary
  cx-summary: true # default false if not provided
  flow-summary-header: Violation Summary
  flow-summary: true # default true if not provided
  detailed-header: Details
  detailed: true # default true if not provided

gitlab:
  webhook-token: XXXXX
  token: XXXXX
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  block-merge: true

bitbucket:
  webhook-token: XXXXX
  token: XXXXX
  url: https://bitbucket.org
  api-url: https://api.bitbucket.org
  api-path: /2.0
  false-positive-label: false-positive

azure:
  webhook-token: xxxx
  token: xxxx
  url: https://dev.azure.com
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive

rally:
  token: xxxx
  rally-project-id: xxxx
  rally-workspace-id: xxxx
  url: https://rallydev.com
  api-url: https://rally1.rallydev.com/slm/webservice/v2.0

jira:
  url: https://xxxx.atlassian.net
  username: XXXXX
  token: XXXXX
  project: <JIRA PROJECT KEY>
  issue-type: <JIRA ISSUE TYPE>
  label-prefix: <CUSTOM PREFIX NAME >
  priorities:
    Critical: Highest
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: In Review
  close-transition: Done
  open-status:
    - To Do
    - In Progress
    - In Review
  closed-status:
    - Done
  suppress-code-snippets:
    - Hardcoded_Password_in_Connection_String
    - Password_In_Comment
    - Use_Of_Hardcoded_Password
  fields:
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: cve
      jira-field-name: CVEs
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

json:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].json"
  data-folder: "C:\\tmp"

cx-xml:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
  data-folder: "C:\\tmp"

csv:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].csv"
  data-folder: "C:\\tmp"
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
### <a name="cli">CLI Examples</a>
Examples to convert YAML property to CLI property

```
#YAML
cx-flow:
  bug-tracker:Json
  
#CLI
--cx-flow.bug-tracker=json  
```
```
#YAML
cx-flow:
  filter-category:
    - Stored_XSS
    - SQL_Injection
  
#CLI
--cx-flow.filter-category="Stored_XSS,SQL_Injection"  
```
```
#YAML
cx-flow:
  thresholds:
    high: 10
    medium: 10
    low: 10
  
#CLI
--cx-flow.thresholds.high=10  
--cx-flow.thresholds.medium=10  
--cx-flow.thresholds.low=10  
```
### <a name="env">Environment Variable Examples</a>
Examples to convert YAML property to Environment variables
```
#YAML
cx-flow:
  bug-tracker:Json
  
#Environment variables
CXFLOW_BUGTRACKER=Json
```
```
#YAML
cx-flow:
  filter-category:
    - Stored_XSS
    - SQL_Injection
  
#Environment variables
CXFLOW_FILTERCATEGORY="Stored_XSS,SQL_Injection"  
```
```
#YAML
cx-flow:
  thresholds:
    high: 10
    medium: 10
    low: 10
  
#Environment variables
CXFLOW_THRESHOLDS_HIGH=10 
CXFLOW_THRESHOLDS_MEDIUM=10  
CXFLOW_THRESHOLDS_LOW=10  
```
## <a name="configuration">Configuration Definitions</a>

### <a name="server">Server Section</a>

```yaml
server:
  port: ${PORT:8080}
```

| Config                    | Default               | Required | WebHook | Command Line | Notes                                                                    |
|---------------------------|-----------------------|----------|---------|--------------|--------------------------------------------------------------------------|
| `port`                    | 8080                  | No       | Yes     | No           | The default value is 8080 unless an environment variable port is defined |

### <a name="cxflow">Cx-Flow Section</a>

```yaml
cx-flow:
  contact: admin@cx.com
  bug-tracker: Json
  bug-tracker-impl:
    - CxXml
    - Csv
    - Json
    - GitLab
    - GitHub
    - Azure
    - Rally
  branches:
    - develop
    - main
    - release`-\w+ # regular expressions supported. If branch-script is provided, this is ignored. branch-script: D:\\tmp\Branch.groovy #default empty/not used
  filter-severity:
    - High
  filter-category:
    - Stored_XSS
    - SQL_Injection
  filter-cwe:
    - 89
    - 79
  filter-status:
    - New
    - Recurrent
  filter-state:
    - Confirmed
    - Urgent
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  deleteForkedProject: true
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  track-application-only: false
  web-hook-queue: 20
  scan-result-queue: 8
  branch-protection-enabled: false
  break-build: false
  scan-resubmit: false
  preserve-project-name: false
  project-custom-field: "git:git test,test:test1"
  scan-custom-field: "git:git test,test:test1"
  http-connection-timeout: xxx # milliseconds - default 30000
  http-read-timeout: xxx # milliseconds - default 120000
  mail:
    host: smtp.gmail.com
    port: 587
    username: xxx
    password: xxx
    enabled: true
    notification: true # default is false
    cc: myemail@mycompany.com # comma-separated list of e-mails
  zip-exclude: \.git/.*, .*\.png
  zip-include: \.git/.*, .*\.png
  comment-script: location/to/commentScript.groovy
  core-poolsize: 54
  max-poolsize: 302
  queue-capacityarg: 600
```

| Config                         | Default        | Required                                                             | WebHook | Command Line | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|--------------------------------|----------------|----------------------------------------------------------------------|---------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `contact`                      |                | No                                                                   |         |              | Contact email for the CxFlow administrator                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `bug-tracker`                  |                | Yes                                                                  | Yes     | Yes          | Must be one of the following: <br />- None<br />- Jira<br />- Email<br />- Any value specified in the bug-tracker-impl custom bean implementations list (a white list of bug tracker implementations)<br /><br /> **Note**:  JIRA/EMAIL/NONE are built in and not required in the bug-tracker-impl list                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `bug-tracker-impl`             |                | No (Only if using one of the applicable bug tracker implementations) | Yes     | Yes          | List of available bug trackers (feedback channels). Currently support for: <br />- Csv<br />- Json<br />- CxXML<br />- GitLab<br />- GitHub<br />- Azure<br />- Rally                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `branches`                     |                | No                                                                   | Yes     | Yes          | List of protected branches that drive scanning within the WebHook flow. If a pull or push event is initiated to one of the protected branches listed here, the scan is initiated. For example:<br />- develop<br />- main<br />- security<br />- release-\w+<br /><br />If no value is provided, all branches are applicable.<br /><br />Regular expressions are supported. (i.e. release-\w+ will match any branches starting with "release-" followed by one or more alphanumeric characters. If a regular expression is used, it must match the complete branch name, not just a prefix.)                                                                                                                                                 |
| `branch-script`                |                | No                                                                   | Yes     | No           | A **groovy** script that can be used to decide, if a branch is applicable for scanning. This applies to any client custom lookups and other integrations.  The script is passed as a **"request"** object of the type **com.checkmarx.flow.dto.ScanRequest** and must return **boolean** (true/false). If this script is provided, it is used for all decisions associated with determining applicability for a branch event to be scanned. **A sample script is attached to this page.                                                                                                                                                                                                                                                      |
| `filter-severity`              |                | No                                                                   | Yes     | Yes          | The severity can be filtered during feedback (**High**, **Medium**, **Low**, **Informational**).  If no value is provided, all severity levels are applicable.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `filter-category`              |                | No                                                                   | Yes     | Yes          | The list of vulnerability types to be included with the results (**Stored_XSS**, **SQL_Injection**) as defined within Checkmarx.  If no value is provided, all categories are applicable.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `filter-cwe`                   |                | No                                                                   | Yes     | Yes          | The list of CWEs to be included with the results (**79**, **89**).  If no value is provided, all categories are applicable.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `filter-state`                 |                | No                                                                   | Yes     | Yes          | The available options are **To Verify**, **Confirmed**, **Urgent** and **Proposed Not Exploitable**.  This only allows for filtering the results that have been confirmed/validated within Checkmarx.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `mitre-url`                    |                | No                                                                   | Yes     | Yes          | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues** to help guide developers.  The link is not provided, if left empty or omitted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `wiki-url`                     |                | No                                                                   | Yes     | Yes          | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues** associated with internal program references (program/assessment methodology, remediation guidance, etc).  The link is not provided, if left empty or omitted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `codebash-url`                 |                | No                                                                   | Yes     | Yes          | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues**  associated with training. The link is titled **'Training'** and is not provided, if left empty or omitted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `track-application-only`       | false          | No*                                                                  | Yes     | Yes          |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `web-hook-queue`               | 100            | No*                                                                  | Yes     | No           | The maximum number of active scans initiated via WebHook at a given time. Requests remain queued until a slot is free.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `scan-result-queue`            | 4              | No*                                                                  | Yes     | Yes          | The maximum number of scan results being processed at the same time. Requests remain queued until a slot is free. <br />As XML files can become large, it is important to limit the number that can be processed at the same time.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `break-build`                  | false          | No*                                                                  | No      | Yes          | A non-zero return code (10) is provide when any of the filtering criteria is met within scan results. See detail [here](https://github.com/checkmarx-ltd/cx-flow/wiki/Thresholds-and-policies#breakBuild)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `branch-protection-enabled`    | false          | No*                                                                  | No      | Yes          | If Value set is true then only protected branches will scanned by cx-flow                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `http-connection-timeout`      | 30000          | No*                                                                  | Yes     | Yes          | Http client connection timeout setting.  Not applied for the Jira client.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `http-read-timeout`            | 120000         | No*                                                                  | Yes     | Yes          | Http client read timeout setting.  Not applied for the Jira client.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `mail`                         | enabled:false  | No*                                                                  | Yes     | Yes          | SMTP configuration - host, port, username, password, enabled (false by default).  When enabled, email is a valid feedback channel, and an html template is used to provide result details. During WebHook execution, the email is sent to the list of committers in the push event.                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `zip-exclude`                  |                | No                                                                   | No      | Yes          | Comma-separated list of regexes. Instructs CxFlow to exclude specific files when it creates a zip archive. See the details [here](Excluding-Files-from-Zip-Archive.md).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `zip-include`                  |                | No                                                                   | No      | Yes          | Comma-separated list of regexes. Instructs CxFlow to include specific files when it creates a zip archive. See the details [here](Excluding-Files-from-Zip-Archive.md).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `auto-profile`                 | false          | No                                                                   | Yes     | No           | During WebHook execution, language stats and files are gathered to help determine an appropriate preset to use.  By default, the profiling initially occurs only when a project is new/created for the first time.  Refer to [CxFlow Automated Code](https://checkmarx.atlassian.net/wiki/spaces/PTS/pages/1345586126/CxFlow+Automated+Code+Profiling) Profiling for details.                                                                                                                                                                                                                                                                                                                                                                |
| `always-profile`               | false          | No                                                                   | Yes     | No           | This enforces the auto-profile execution for each scan request regardless of whether the project is new or not.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `profiling-depth`              | 1              | No                                                                   | Yes     | No           | The folder depth that is inspected for file names during the profiling process, which means looking for specific file references, i.e. web.xml/Web.config                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `profile-config`               | CxProfile.json | No                                                                   | Yes     | No           | The file that contains the profile configuration mapping.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `scan-resubmit`                | false          | No                                                                   | Yes     | No           | When **True**: If a scan is active for the same project, CxFlow cancels the active scan and submits a new scan. When **False**: If a scan is active for the same project, CxFlow does not submit a new scan.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `preserve-project-name`        | false          | No                                                                   | Yes     | Yes          | When **False**: The project name will be the repository name after normalization (i.e. Front-End-dev). Legal characters are: `a-z`, `A-Z`, `0-9`, `-`, `_`, `.`. All other characters will be replaced in the normalization process with "-". <br/> When **True**: The project name will be the exact project name inputted without normalization (i.e. Front End-dev). <br/> **For attention:** <br/> 1. Not all scanners allow project names with invalid characters.<br/> 2. The preserve-project-name parameter is also effective for project name coming from config-as-code.                                                                                                                                                           |
| `merge-id`                     | Merge Id       | No                                                                   | No      | Yes          | Pass Merge Id from CLI mode for Specific Merge Request. Used in by GiLab CI/CD Pipeline.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `merge-title`                  | Merge Title    | No                                                                   | No      | Yes          | Pass Merge Title from CLI mode for Specific Merge Request. Used in by GiLab CI/CD Pipeline.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `comment-script`               |                | No                                                                   | Yes     | Yes          | A **groovy** script that can be used to determine the scan comment to be sent to the CxSAST server during a scan. see details [here](https://github.com/checkmarx-ltd/cx-flow/wiki/External-Scripts#scanComment)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `core-poolsize`                | 50             | No                                                                   | No      | Yes          | The amount of worker which can work on a thread parallel.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `max-poolsize`                 | 200            | No                                                                   | No      | Yes          | The amount of threads which can be created parallel.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `queue-capacity`               | 10000          | No                                                                   | No      | Yes          | When the amount of threads present are more than the max poolsize then the threads will wait in the queue. This parameter defines the size of that queue.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `comment`                      |                | No                                                                   | No      | Yes          | User can store comments field in metadata about the scan.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `overrideProjectSetting`       |                | No                                                                   | No      | Yes          | The utilization of this boolean variable empowers the user to restrict the override of project settings. By setting this variable, users can prevent any unauthorized alterations to the project's settings, ensuring stability and adherence to predefined configurations. This functionality serves as a safeguard against inadvertent or malicious changes that could potentially disrupt the project's operations. Thus, the boolean variable offers a valuable mechanism for maintaining the integrity and consistency of project settings, enhancing overall control and security within the system. Its implementation empowers users with the ability to govern and protect vital project parameters from unwarranted modifications. |
| `enabledVulnerabilityScanners` | false          | No                                                                   | Yes     | Yes          | User can define which checkmarx tool they want to use like SAST, SCA or both.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `scanUnprotectedBranches` | false          | No                                                                   | Yes     | No           | If no branch list is provided for checking the protected branch and the user still wants their scan to occur, set this variable to true.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `deleteForkedProject`          | false          | No                                                                   | Yes     | No           | User can delete forked projects created on SAST portal.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `delete-branched-project`      | false          | No                                                                   | Yes     | No           | User can delete branched project created on SAST portal.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `project-custom-field`         |                | No                                                                   | Yes     | No           | User can add multiple project-custom-field in SAST portal. `checkmarx.settings-override` property should be true.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `scan-custom-field`            |                | No                                                                   | Yes     | No           | User can add multiple scan-custom-field in SAST portal. `checkmarx.settings-override` property should be true.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |


No* = Default is applied

#### <a name="email">E-Mail notifications</a>

```yaml
cx-flow:
  mail:
    host: smtp.gmail.com
    port: 587
    username: xxx
    password: xxx
    enabled: true
    notification: true # default is false
    cc: myemail@mycompany.com # comma-separated list of e-mails
```

#### <a name="filtering">Filtering</a>
Filtering, as specified above, is available on the following criteria:

```yaml
cx-flow:
  filter-severity: Medium
  filter-category: Stored_XSS
  filter-cwe: 79
  filter-state: Confirmed
```

* **Severity** → Severity from Checkmarx
* **Category** → Vulnerability name within Checkmarx
* **CWE** → CWE value from Checkmarx
* **State** → Urgent | Confirmed

All values are case-sensitive as per the output from Checkmarx (i.e. High severity, Stored_XSS, Confirmed).
#### <a name="excludeFilter">Excluding Vulnerability</a>
We can exclude vulnerabilities according to category, cwe and state.  

```yaml
cx-flow:
 exclude-category: Stored_XSS
 exclude-cwe: 79
 exclude-state: Confirmed
```
* **Category** → Vulnerability name within Checkmarx
* **CWE** → CWE value from Checkmarx
* **State** → Urgent | Confirmed

All values are case-sensitive as per the output from Checkmarx (Stored_XSS, Confirmed).

#### <a name="excludezip">Excluding and Including Files from Zip Archive</a>

The `cx-flow.zip-exclude` configuration option instructs CxFlow to exclude specific files when it creates a zip archive.

##### Example
The following option excludes all `.png` files from the archive, as well as all files inside a root-level `.git` directory:

```yaml
cx-flow:
  zip-exclude: \.git/.*, .*\.png
```

##### Details
The meaning and syntax of the `cx-flow.zip-exclude` option are different as opposed to the `checkmarx.exclude-folders` and `checkmarx.exclude-files` options.
* If User want to exclude folders or exclude files, **settings-override** should be true. If user is using CLI they can pass parameter as `--checkmarx.settings-override=true`
* If user is using Github Action value of exclude files or folders should not be in double or single quotes. Example : If user want to exclude folder name abc he should pass parameter as `--checkmarx.exclude-folders=abc` or `--checkmarx.exclude-folders=*abc` or ``--checkmarx.exclude-folders=\*abc*

| `cx-flow.zip-exclude`                               | `checkmarx.exclude-folders`, `checkmarx.exclude-files` |
|-----------------------------------------------------|--------------------------------------------------------|
| Uses regexes                                        | Use wildcards                                          |
| Works locally, before the sources are sent for scan | Work in CxSAST when it already has the sources         |

`cx-flow.zip-exclude` is a comma-separated list. Each of the list items is a regex (not a wildcard). Spaces before and after a comma are ignored.

During zipping, CxFlow checks each file in the target directory against each of the regexes in `cx-flow.zip-exclude`. If there is a match, CxFlow excludes the file from the archive. In this case, when log level is **debug**, CxFlow writes a message to the log having the following format:
```
match: <regex>|<relative_file_path>
```

CxFlow uses relative file path to test the regex match. E.g. if the following file exists:
```   
c:\cxdev\projectsToScan\myproj\bin\internal-dir\exclude-me.txt
```
and we specify this CLI option: `--f="c:\cxdev\projectsToScan\myproj`,

then CxFlow will check the following relative file path against the regexes:
```
bin/internal-dir/exclude-me.txt
```
CxFlow normalizes slashes in the relative path into a forward slash (`/`).

For a file to be excluded, a regex must match the **whole** relative file path. Thus, the `.*` regex expression should be used where necessary.

##### Zip Include
The `cx-flow.zip-include` configuration option instructs CxFlow to include specific files when it creates a zip archive.
```yaml
cx-flow:
  zip-include: \.git/.*, .*\.png
```

*Note:* Process of zip-exclude and zip-include are same, the only difference is zip-exclude excludes the files from zip and zip-include includes only mentioned files to zip.

#### <a name="break">Break Build</a>
The configuration can be set or overridden at execution time using the command line (`--cx-flow.break-build=true`) to exit the command line execution flow for a single project result or scan for results that meet the filter criteria.

For more details on break build, please refer to [Thresholds and policies](https://github.com/checkmarx-ltd/cx-flow/wiki/Thresholds-and-policies#breakbuild) chapter.

**Note**:  This does not apply to WebHooks or for batch cli execution (instance and team).  It only works, if one project result is processed.


### <a name="checkmarx">Checkmarx Section</a>

| Config                                 | Default               | Required                            | WebHook | Command Line      | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|----------------------------------------|-----------------------|-------------------------------------|---------|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `username`                             |                       | Yes                                 | Yes     | Yes               | Service account for Checkmarx                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `password`                             |                       | Yes                                 | Yes     | Yes               | Service account password Checkmarx                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `client-secret`                        |                       | Yes                                 | Yes     | Yes               | OIDC client secret for API login to Checkmarx                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `scope`                                |                       | Yes                                 | Yes     | Yes               | While using scope value in CLI, it must be provided in double quotes "" otherwise an exception will be thrown.                                                                                                                                                                                                                                                                                                                                                                          |
| `base-url`                             |                       | Yes                                 | Yes     | Yes               | Base FQDN and port for Checkmarx                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `multi-tenant`                         | false                 | No*                                 | Yes     | Yes (Scan only)   | If yes, the name space is created or reused, if it has been pre-registered or already created for previous scans)                                                                                                                                                                                                                                                                                                                                                                       |
| `version`                              |                       | Yes (if Using CxSAST 9.0 or higher) | Yes     | Yes               | Required for CxSAST version 9.0 and higher                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `scan-preset`                          | Checkmarx Default     | No*                                 | Yes     | Yes (Scan only)   | The default preset used for the triggered scan                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `configuration`                        | Default Configuration | No*                                 | Yes     | Yes (Scan only)   | Checkmarx scan configuration setting                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `team`                                 |                       | Yes (not for XML parse mode)        | Yes     | Yes (Scan only)   | Base team in Checkmarx to drive scanning and retrieving of results                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `scan-timeout`                         | 120                   | No*                                 | Yes     | Yes (scan only)   | The amount of time (in minutes) that cx-flow will wait for a scan to complete to process the results.  The Checkmarx scan remains as is, but no feedback is provided                                                                                                                                                                                                                                                                                                                    |
| `scan-polling`                         | 20000                 | No                                  | Yes     | Yes               | The amount of time (in milliseconds) in which cx-flow pings CxSAST server to get the status of the scan (i.e Queued, Finished or Failed).                                                                                                                                                                                                                                                                                                                                               |
| `report-timeout`                       | 300000                | No                                  | Yes     | Yes               | The amount of time (in milliseconds) for which cx-flow will wait for CxSAST to generate scan report.If report is not generated within  300000(in miliseconds)it will through Timeout exceeded during report generation as error message.                                                                                                                                                                                                                                                |
| `report-polling`                       | 5000                  | No                                  | Yes     | Yes               | The amount of time (in milliseconds) in which cx-flow pings CxSAST server to get the status of the report.                                                                                                                                                                                                                                                                                                                                                                              |
| `preserve-xml`                         | false                 | No*                                 | Yes     | Yes               | This flag is used to preserve the original XML results retrieved by the Checkmarx scan inside the ScanResults object to be later used by a Custom bug tracker implementation, if required.  Currently, **CxXMLIssueTracker** uses this flag                                                                                                                                                                                                                                             |
| `incremental`                          | false                 | No*                                 | Yes     | Yes               | Enables support for incremental scan support when CxFlow is triggering scans.  The incremental-num-scans and incremental-threshold values must not be exceeded for the last available full scan criteria.                                                                                                                                                                                                                                                                               |
| `incremental-num-scans`                | 5                     | No*                                 | Yes     | Yes (scan only)   | The maximum number of scans before a full scan is required                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `project-script`                       |                       | No                                  | Yes     | Yes               | A **groovy** script that can be used for deciding the name of the project to create/use in Checkmarx. This is to allow for any client custom lookups and other integrations.  The script is passed a "**request**" object, which is of type **com.checkmarx.flow.dto.ScanRequest**, and must return **String** representing the **team name** to be used. If this script is provided, it is used for all decisions associated with the determining project name                         |
| `team-script`                          |                       | No                                  | Yes     | Yes               | A **groovy** script that can be used for deciding the team to use in Checkmarx.  This is to allow for any client custom lookups and other integrations.  The script is passed a "request" object, which is of type **com.checkmarx.flow.dto.ScanRequest**, and must return **String** representing the team path to be used. If this script is provided, it is used for all decisions associated with determining project name.                                                         |
| `incremental-threshold`                | 7                     | No*                                 | Yes     | Yes (scan only)   | The maximum number of days before a full scan is required                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `offline`                              | false                 | No*                                 | No      | Yes (parse only)  | Use Table this only when parsing Checkmarx XML, this flag removes the dependency from Checkmarx APIs when parsing results.  This skips retrieving the issue description from Checkmarx.                                                                                                                                                                                                                                                                                                 |
| `exclude-files`                        |                       | No                                  | Yes     | Yes               | Files to be excluded from Scan                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `exclude-folders`                      |                       | No                                  | Yes     | Yes               | Folders to be excluded from Scan                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `custom-state-map`                     |                       | No                                  | No      | Yes               | A map of custom result state identifiers to custom result state names                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `custom-state-false-positive-map`      |                       | No                                  | No      | Yes               | If user want to consider custom result state identifiers as false positive they can use this map.                                                                                                                                                                                                                                                                                                                                                                                       |
| `modify-branch-name-by-pattern-map`    |                       | No                                  | No      | Yes               | If a user's project name contains the branch name and the Govy script is modifying both the project name and the branch name because the branch name was detected, we can utilize this parameter to synchronize and adjust the branch name accordingly. E.g- Project name : project name : abc-feature/1.0 and Branch Name : feature/1.0  and grrovy script changed projectname to : abc-feature_1.0 Branch then we have to replace / by _ which cann be done using this parameter map. |
| `post-action-postback-id`              |                       | No                                  | Yes     | Yes               | Sets the SAST project's post-scan action to use the post-scan action with the provided Id defined in SAST.If not provided, the project does not get configured to use a post-scan action.                                                                                                                                                                                                                                                                                               |
| `settings-override`                    |                       | No                                  | Yes     | Yes               | Defaults value false, if set to true the projects settings are re-written/overridden when each SAST scan is invoked from CxFlow                                                                                                                                                                                                                                                                                                                                                         |
| `cx-branch`                            | false                 | No                                  | Yes     | Yes               | A flag to enable branching of projects in CxSAST.                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `scan-queuing`                         | false                 | No                                  | Yes     | Yes               | A flag to enable queuing of scan events.                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `scan-queuing-timeout`                 | 720                   | No                                  | Yes     | Yes               | The amount of time (in minutes) for which cx-flow will keep a scan event data in its queue before sending to CxSAST, when all the available concurrent scans in CxSAST are in use.                                                                                                                                                                                                                                                                                                      | 
| `disable-clubbing`                     | false                 | No                                  | Yes     | Yes               | If set to true, results are not grouped at all.By default, results are grouped only by vulnerability and filename.                                                                                                                                                                                                                                                                                                                                                                      |
| `email-notifications`                  |                       | No                                  |         | Yes (Scan only)   | A map containing any or all of the following keys: `after-scan`, `before-scan`, `failed-scan`. The vaue of each key is a list of email addresses to which a notification should be sent in the case of the relevant event.                                                                                                                                                                                                                                                              |
| `project-branching-check-count`        | 3                     | No                                  | Yes     | Yes (Scan only)   | The number of times to check the project branching status after a project has been branched. Only relevant for versions of CxSAST that support the querying of the branching status (API version 4 and higher).                                                                                                                                                                                                                                                                         |
| `project-branching-check-interval`     | 5                     | No                                  | Yes     | Yes (Scan only)   | The interval between checks of the project branching status. For versions of CxSAST that do not support querying the project branching status, execution will pause once for the specified duration.                                                                                                                                                                                                                                                                                    |
| `restrict-results-to-branch`           | false                 | No                                  | Yes     | Yes (Scan only)   | If set to `true`, when scanning a branched project, only results detected on the branch are reported. As the OData API is needed for this functionality, the CxSAST user used must be a assigned a role with the “API” permission. Also, the `client-id` property should be set to “resource_owner_sast_client” and the `scope` property should be set to “access_control_api sast_api”.                                                                                                |
| `forcescan`                            | false                 | No                                  | Yes     | Yes (--forcescan) | Specifies whether the code should be scanned regardless of unchanged code                                                                                                                                                                                                                                                                                                                                                                                                               |
| `delete-running-scans`                 | false                 | No                                  | Yes     | Yes               | used while deleting running scan when project needs to be deleted                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `GITLAB_ERROR_MERGE`   (YML Parameter) | false                 | No                                  | No      | No                | To enable pull request status checks in GITLAB, set value to true.                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `GITLAB_BLOCK_MERGE`   (YML Parameter) | false                 | No                                  | No      | No                | To enable pull request status checks in GITLAB, set value to true.                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `cxflow.enabledVulnerabilityScanners`  | false                 | No                                  | Yes     | Yes               | User can define which checkmarx tool they want to use like SAST, SCA or both.                                                                                                                                                                                                                                                                                                                                                                                                           |
| `checkmarx.considerScanningStatus`     | false                 | No                                  | Yes     | Yes               | By default, Checkmarx only includes completed scans (finished status) in incremental scans. This means it ignores scans that are currently running (scanning) or waiting to be processed (new queue). Enabling a feature this variable "cxflow" expands what incremental scans consider. With cxflow, scans in progress and those queued up are also taken into account, providing a more comprehensive view of your code's security posture.                                           |
| `enabled-zip-scan`                     | false                 | No                                  | Yes     | Yes               | When `enabled-zip-scan` is set to `true` then cx-flow will first clone the repository locally, and then it will zip the repository and send it for scanning.                                                                                                                                                                                                                                                                                                                            |
| `truststorepath`                     | false                 | No                                  | Yes     | Yes               | User need to provide path of custom keystore along with file name.                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `truststorepassword`                     | false                 | No                                  | Yes     | Yes               | User need to provide custom keystore password.                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `customkeystore`                     | false                 | No                                  | Yes     | Yes               | When `customkeystore` is set to `true` then cx-flow will consider custom keystore.                                                                                                                                                                                                                                                                                                                                                                                                      |
| `trustcerts`                     | false                 | No                                  | Yes     | Yes               | If this option is true Cx-flow will bypass SSL. Default value is false so it will not bypass SSL.                                                                                                                                                                                                                                                                                                                                                                                       |
| `isBranchedIncremental`                     | false                 | No                                  | Yes     | Yes               | If this option is true Cx-flow will do incremental scan for first time created branched project.                                                                                                                                                                                                                                                                                                                                                                                        |
| `cancelInpregressScan`                     | false                 | No                                  | Yes     | Yes               | If a scan timeout occurs and the user requests to cancel the in-progress scan, set this boolean variable to true.                                                                                                                                                                                                                                                                                                                                                                       |
No* = Default is applied

### Custom Checkmarx Fields

| Custom Checkmarx Field Name | Required | WebHook | Command Line | Notes                                                                                                          |
|-----------------------------|----------|---------|--------------|----------------------------------------------------------------------------------------------------------------|
| `jira-project`              | No       | Yes     | Yes          | Custom Checkmarx field name to override Jira Project setting for a given Checkmarx scan result / project       |
| `jira-issuetype`            | No       | Yes     | Yes          | Custom Checkmarx field name to override Jira Issue Type settings for a given Checkmarx scan result / project   |
| `jira-fields`               | No       | Yes     | Yes          | Custom Checkmarx field name to override Jira custom field mappings for a given Checkmarx scan result / project |
| `jira-assignee`             | No       | Yes     | Yes          | Custom Checkmarx field name to override Jira assignees for a given Checkmarx scan result / project             |



#### <a name="nine">9.0 Configuration Changes</a>
```yaml
checkmarx:
  version: 9.0 # Required for version 9.x
  username: xxxxx
  password: xxxxx
  client-id: resource_owner_client
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  scope: access_control_api sast_rest_api # Required for version 9.x
  base-url: http://cx.local
  multi-tenant: true
  configuration: Default Configuration
  scan-preset: Checkmarx Default
  team: /CxServer/Checkmarx/CxFlow
  url: ${checkmarx.base-url}/cxrestapi
  preserve-xml: true
  incremental: true
  trustcerts: true
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  exclude-files: "*.tst,*.json"
  exclude-folders: ".git,test"
  cx-branch: false
  scan-queuing: true
  scan-timeout: 120
  scan-polling: 20000
  report-timeout: 300000
  report-polling: 5000
  scan-queuing-timeout: 720 # Webhook and --scan command line only, number of minutes  
```
**Note:**
* Make sure to include `version: 9.0` (or higher) and `scope: access_control_api sast_rest_api`
* The Team path must include the unix path separator **/**, the path is for example defined as follows: `/CxServer/Checkmarx/CxFlow`

### <a name="postback">Postback action </a>
On using post back mode, Checkmarx post back action will be added to the Checkmarx project and that action will trigger the /postbackAction endpoint on CxFlow.

```yaml
checkmarx
  ...
post-action-postback-id: 123456
```
For more details on postback mode, Please refer to [CxSAST Version 9.0](https://github.com/checkmarx-ltd/cx-flow/wiki/CxSAST-Version-9.0#postback) chapter

### <a name="override">Override project settings</a>
The configuration can be set to override project settings with Cxflow configuration when triggering new scan for SAST project, or to avoid project setting update if property set to 'false'.


Defaults value is false, if set to true the projects settings like "scan-preset","post-action-postback-id" are re-written/overridden when each SAST scan is invoked from CxFlow.
```yaml
checkmarx
  ...
settings-override: true #default false if not provide
```

### <a name="branchproject">Branched Project</a>
A branched project is a child of a base project in CxSAST. Upon initiating a scan from the default branch of a repository, CxSAST creates a base project in the server with name `RepoName-defaultBranchName` and any subsequent scans from the branches of that repository will create child projects off of it with name `RepoName-currentBranchName`. The project count in CxSAST does not increase when a branched project is added. Branching of projects can be enabled by setting the `cx-branch` property to `true`.

### Base Project Creation:

* When you initiate a scan from the default branch of a repository, CxSAST automatically creates a base project on the server.
* The naming convention used is RepoName-defaultBranchName (e.g., `MyRepo-main` if `main` is your default branch).

### Child Projects (Branched Projects):

* For scans initiated from other branches (not the default), CxSAST creates branched projects or child projects.
* These child projects are named using the convention `RepoName-currentBranchName` (e.g., `MyRepo-feature1`).
* Importantly, the total project count in CxSAST does not increase when these branched projects are added. They are treated as extensions of the base project.

### Enabling Project Branching:
* To enable project branching, you need to set the `cx-branch` property to `true`.
* Additionally, you must provide both the `default branch name` and the `current branch name`.
### Project Count

- The overall project count in CxSAST does not increase when branched projects are added. Branched projects are managed under the umbrella of the base project, which keeps the total project count static.

### Enabling Project Branching

### Implementation Example:
* When configuring the scan (e.g., via a CxSAST API or build pipeline), you might set these properties like this:

```yaml
checkmarx:
  ...
  cx-branch: true #default false if not provided
```

* CLI parameter to provide default branch name is `default-branch = main` and current branch can be passed `branch = feature1`. 
* In case of webhook it will be passed automatically in payloads.

### <a name="scanqueuing"> Scan Queuing and Scan Queuing Timeout</a>
If the number of concurrent scans which can run on CxSAST server is all utilized, then enabling `scan-queuing` will allow CxFlow to keep the event of the scan within itself and let the existing scans finish before sending the new scan event to CxSAST. Cx-flow keeps events with itself for a number of minutes, specified by the `scan-queuing-timeout` parameter, with a default value of **120** minutes.
```yaml
checkmarx:
  ...
  scan-queuing: true
  scan-queuing-timeout: 720 #Amount of time in minutes
```
### <a name="scantimeoutandscanpolling"> Scan Timeout and Scan Polling</a>
The amount of time (in minutes) for which cx-flow will wait for CxSAST scan to finish.If scan is not completed within 120(in minutes) then it will gives Timeout exceeded during scan as error messase.The default value of scanTimeout **120** minutes.
The amount of time (in milliseconds) in which cx-flow pings CxSAST server to get the status of the scan (i.e Queued, Finished or Failed).The default value of scanPolling **20000** miliseconds.
```yaml
checkmarx:
  ...
  scan-timeout: 120 #Amount of time in minutes
  scan-polling: 20000 #Amount of time in miliseconds
```

### <a name="reporttimeoutandreportpolling"> Report Timeout and Report Polling</a>
The amount of time (in milliseconds) for which cx-flow will wait for CxSAST to generate scan report.If report is not generated within  300000(in miliseconds)it will through Timeout exceeded during report generation as error message.The default value of reportTimeout **30000** miliseconds.
The amount of time (in milliseconds) in which cx-flow pings CxSAST server to get the status of the report.The default value of reportPolling **5000** miliseconds.
```yaml
checkmarx:
  ...
  report-timeout: 300000 #Amount of time in miliseconds
  report-polling: 5000 #Amount of time in miliseconds
```

### <a name="emailnotifications"> Checkmarx Email Notifications</a>
If present, this property specifies the email notifications to be sent when a SAST scan is run. Notifications can be sent before a scan starts, after a scan finishes, and when a scan fails. The content of the `email-notifications` property is a map from the following keys to lists of email receipients: `after-scan`, `before-scan`, `failed-scan`.
```yaml
checkmarx:
  ...
  email-notifications:
    after-scan:
      - user1@example.com
      - user2@example.com
    before-scan:
      - user3@example.com
      - user4@example.com
    failed-scan:
      - user5@example.com
      - user6@example.com
```

Email notifications can also be configured via conf-as-code.

## <a name="webhook">WebHook Configuration</a>
Each repository type requires its own specific configuration block as defined below.  Each of these have available overrides that can be provided in the form of URL parameters or as a JSON configuration blob that is base64 encoded and provided as an url parameter (override=<XXXXXX>).

WebHook scans are triggered based on the protected branches list. This configuration is under the config block.

The protected branches list can be provided in the application.yml file under the `cx-flow` section, or it can be provided in the config-as-code file. If branches are provided in `application.yml` as well as config-as-code file then the branches in config-as-code file will override the branches in `application.yml`. If protected branches is not provided in either of the files then CxFlow triggers a scan for Push/Pull/Merge event for all the branches.

For **Pull/Merge**, if a request is made to pull/merge code into one of the listed protected branches, CxFlow triggers the scan. The pull/merge is commented with the filtered findings from Checkmarx.

For **Push**, the findings are published according to the specified bug-tracker in the main or overridden configuration - i.e. JSON/CSV/XML output or Jira defect.

For additional information, refer to the workflow for [WebHooks](https://github.com/checkmarx-ltd/cx-flow/wiki/Workflows).

### <a name="code">WebHook URL Parameters - Code</a>
```java
@RequestParam(value = "application", required = false) String application,
@RequestParam(value = "branch", required = false) List<String> branch,
@RequestParam(value = "severity", required = false) List<String> severity,
@RequestParam(value = "cwe", required = false) List<String> cwe,
@RequestParam(value = "category", required = false) List<String> category,
@RequestParam(value = "project", required = false) String project,
@RequestParam(value = "team", required = false) String team,
@RequestParam(value = "status", required = false) List<String> status,
@RequestParam(value = "assignee", required = false) String assignee,
@RequestParam(value = "preset", required = false) String preset,
@RequestParam(value = "incremental", required = false) Boolean incremental,
@RequestParam(value = "exclude-files", required = false) List<String> excludeFiles,
@RequestParam(value = "exclude-folders", required = false) List<String> excludeFolders,
@RequestParam(value = "override", required = false) String override,
@RequestParam(value = "bug", required = false) String bug,
@RequestParam(value = "app-only", required = false) Boolean appOnlyTracking,
@RequestParam(value = "state", required = false) List<String> state,
@RequestParam(value = "threshold-high", required =false) Integer thresholdHigh,
@RequestParam(value = "threshold-medium", required =false) Integer thresholdMedium,
@RequestParam(value = "threshold-low", required = false) Integer thresholdLow,
@RequestParam(value = "threshold-info", required = false) Integer thresholdInfo
```

### <a name="details">WebHook URL Override Parameters - Details</a>
These parameters are related to the WebHook URL parameters above.

| Configuration      | Description                                                                                                                                                                                                                                                          |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `application`      | Override the application name, which is directly linked to Jira and other defect management implementations for tracking purposes.                                                                                                                                   |
| `branch`           | Override the protected branches that drive the scan. For multiple branches, simply list the branch multiple times. i.e. `branch=XXX&branch=YYYY`                                                                                                                     |
| `severity`         | Override the severity filters. For multiple severity simply list multiple times, i.e. `severity=High&severity=Medium`                                                                                                                                                |
| `cwe`              | Override the cwe filters. For multiple cwe, simply list the cwe multiple times, i.e. `cwe=89&cwe=79`                                                                                                                                                                 |
| `category`         | Override the category filters. For multiple category, simply list category multiple times, i.e. `category=Stored_XSS&category=SQL_Injection`                                                                                                                         |
| `project`          | Override the project name that will be created/used in Checkmarx. This allows for greater flexibility for incremental scan relating to pull requests,  i.e. use a standardized pull project name that is always used regardless of the branch - `?project=repo-pull` |
| `team`             | Override the team within Checkmarx to use/create project under.                                                                                                                                                                                                      |
| `state`            | Override the state filters (Confirmed/Urgent). For multiple state, simply list the state multiple times, i.e. `status=Confirmed&status=Urgent`                                                                                                                       |
| `status`           | Override the status filter. For multiple status, simply list the status multiple times, i.e. `status=New&status=Recurrent`                                                                                                                                           |
| `assignee`         | Override the assignee                                                                                                                                                                                                                                                |
| `preset`           | Override the Checkmarx preset rules for scanning                                                                                                                                                                                                                     |
| `incremental`      | Override incremental property to enable/disable incremental scan support                                                                                                                                                                                             |
| `exclude-files`    | Override file exclusions                                                                                                                                                                                                                                             |
| `exclude-folders`  | Override folder exclusions                                                                                                                                                                                                                                           |
| `override`         | Override a complete **JSON** blob as defined below                                                                                                                                                                                                                   |
| `bug`              | Override the default configured bug                                                                                                                                                                                                                                  |
| `app-only`         | This forces Jira issues to be tracked according to the defined application / repo name, as opposed to defining uniqueness per namespace/repo/branch                                                                                                                  |
| `threshold-high`   | Override High severity count threshold                                                                                                                                                                                                                               | 
| `threshold-medium` | Override Medium severity count threshold                                                                                                                                                                                                                             | 
| `threshold-low`    | Override Low severity count threshold                                                                                                                                                                                                                                | 
| `threshold-info`   | Override Info severity count threshold                                                                                                                                                                                                                               | 
**Note**:  Overrides are not required. You only need it if you want to override the global configuration specified from the main `application.yml`

## <a name="repository">Repository Configuration Blocks</a>

**Notes**:
All the repository configurations have common elements such as:

* **token** → api token for the repo, to gain access (typically personal access token for a service account)
* **web-token** → CxFlow shared secret to be used when registering the webhook on the repo
* **url** → base url for the repo
* **api-url** → base url for the api endpoints for the repo
* **block-merge** → boolean, determine if merge should be blocked while scan is completing in Checkmarx
* **cx-summary-header** → Pull/Merge Markdown comment header for the Checkmarx Summary, if used
* **cx-summary** → boolean, determine if the base Checkmarx Summary is displayed (unfiltered)
* **flow-summary-header** → Pull/Merge Markdown comment header for the CxFlow Violation Summary, if used
* **flow-summary** →  boolean, determine if the base CxFlow Violation Summary is displayed (filtered)
* **detailed-header** → Pull/Merge Markdown comment header for the CxFlow details, if used
* **detailed** →  boolean, determine if the detailed CxFlow results (vulnerability lines/files are displayed

### <a name="github">GitHub</a>
```yaml
github:
  webhook-token: xxxx
  token: xxxx
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true
  detailed: false
  scan-submitted-comment: false
  max-description-length : <should be greater than 4 and less than 50000>
  max-delay : <minimum value should be 3>
  comment-update: false
  zero-vulnerability-summary: true
  fields:
    - type: result
      name: application
    - type: result
      name: project
```

| Configuration                | Default        | Description                                                                                                                                                                                                       |
|------------------------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `webhook-token`              |                | Token used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request. GitHub signs the request with this value, and the signature is validated on the receiving end. |
| `token`                      |                | The API token with access to the repository, with at least Read only access to the code, the ability to add comments to pull requests, and the ability to create GitHub Issues.                                   |
| `url`                        |                | Main repo url for GitHub                                                                                                                                                                                          |
| `api-url`                    |                | The API endpoint for GitHub, which is a different context or potentially FQDN than the main repo url.                                                                                                             |
| `false-positive-label`       | false-positive | A label that can be defined within the GitHub Issue feedback that is used to ignore issues                                                                                                                        |
| `block-merge`                | false          | When triggering scans based on PullRequest, this will create a new status of pending, which will block the merge ability until the scan is complete in Checkmarx.                                                 |
| `scan-submitted-comment`     | true           | Comment on PullRequest with "Scan submitted (or not submitted) to Checkmarx ...".                                                                                                                                 | 
| `max-description-length`     | 50000          | Manages number of characters to view in issue description.(value should be greater than 4 and less than 50000)                                                                                                    |
| `max-delay`                  |                | When Secondary rate limit is hit, it will delay each API call for issue creation(Mininum value should be 3)                                                                                                       |
| `comment-update`             | true           | if false, will create a new comment for every scan                                                                                                                                                                |
| `zero-vulnerability-summary` | false          | if true, will not comment in PR decoration any details for scans as vulnerabilities are zero.                                                                                                                     |
| `fields`                     |                | Refer page: [Bug-Trackers-and-Feedback-Channels Chapter Github Fields](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#githubfields)                                             |
**Note**: A service account is required with access to the repositories that will be scanned, pull requests that will be commented on, and GitHub issues that will be created/updated.


### <a name="gitlab">GitLab</a>
```yaml
gitlab:
  webhook-token: xxxx
  token: xxxx
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  block-merge: true
  comment-update: false
  zero-vulnerability-summary: true
  fields:
    - type: result
      name: application
    - type: result
      name: project
```

| Configuration                | Default        | Description                                                                                                                                                                         |
|------------------------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `webhook-token`              |                | Token used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request.                                                                  |
| `token`                      |                | This is the API token with access to the repository, with at least Read only access to code, the ability to add comments to pull requests, and the ability to create GitLab issues. |
| `url`                        |                | Main repo url for GitLab.                                                                                                                                                           |
| `api-url`                    |                | The API endpoint for GitLab, which serves a different context or potential FQDN than the main repo url.                                                                             |
| `false-positive-label`       | false-positive | A label that can be defined within the GitLab Issue feedback to ignore issues                                                                                                       |
| `block-merge`                | false          | When triggering scans based on Merge Request, the Merge request is marked as WIP in GitLab, which blocks the merge ability until the scan is complete in Checkmarx.                 |
| `scan-submitted-comment`     | true           | Comment on Merge Request with "Scan submitted (or not submitted) to Checkmarx ...".                                                                                                 |
| `comment-update`             | true           | if false, will create a new comment for every scan                                                                                                                                  |
| `zero-vulnerability-summary` | false          | if true, will not comment in PR decoration any details for scans as vulnerabilities are zero.                                                                                       |
| `fields`                     |                | Refer page: [Bug-Trackers-and-Feedback-Channels Chapter Gitlab Fields](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#gitlabfields)               |

**Note**: A service account is required with access to the repositories that are going to be scanned, pull requests that are commented on, and GitLab issues that are created/updated.

### <a name="azure">Azure DevOps</a>
```yaml
azure:
  webhook-token: cxflow:1234
  token: xxxx
  url: https://dev.azure.com/XXXXXX
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  block-merge: true
  closed-status: Closed
  open-status: Active
  zero-vulnerability-summary: true
```

| Configuration                | Default        | Description                                                                                                                                                                             |
|------------------------------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `webhook-token`              |                | **<user>:<token>** as defined when registering the event in ADO.  Used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request.          |
| `token`                      |                | This is the API token with access to the repository. It has at least Read only access to code, the ability to add comments to pull requests, and the ability to create Azure WorkItems. |
| `url`                        |                | Main repo url for Azure DevOps, including high level namespace.  **Note**: this is only required when running from the command line and not for WebHooks.                               |
| `issue-type`                 | issue          | The WorkItem type within Azure, i.e. issue / impediment.                                                                                                                                |
| `issue-body`                 | description    | The body to enter free text regarding the issue.  The default across various workItem types are **Description** or **System.Description**.                                              |
| `app-tag-prefix`             | app            | Used for tracking existing issues.  Issues are tagged with this value, if **app** is provided (without namespace/repo/branch)                                                           | 
| `owner-tag-prefix`           | owner          | Used for tracking existing issues.  Issues are tagged with this value                                                                                                                   |
| `repo-tag-prefix`            | repo           | Used for tracking existing issues.  Issues are tagged with this value                                                                                                                   |
| `branch-label-prefix`        | branch         | Used for tracking existing issues.  Issues are tagged with this value                                                                                                                   |
| `api-version`                | 5.0            | Azure DevOps API version to use                                                                                                                                                         |
| `open-status`                |                | Status when re-opening a a workItem                                                                                                                                                     |
| `closed-status`              |                | Status when closing a workItem                                                                                                                                                          |
| `false-positive-label`       | false-positive | A label/tag that can be defined within the Azure Issue feedback being used to ignore issues.                                                                                            |
| `block-merge`                | false          | When triggering scans is based on pull request, this marks the Pull in blocked state until the scan is complete at Checkmarx.                                                           |
| `zero-vulnerability-summary` | false          | if true, will not comment in PR decoration any details for scans as vulnerabilities are zero.                                                                                           |
**Note**: A service account is required with access to the repositories that are scanned, pull requests that are commented on, and Azure WorkItems that are created/updated.


### <a name="sarif">Sarif</a>
```yaml
sarif:
 hassnippet: true
 enableOriginalUriBaseIds: true
 srcRootPath: %SRCROOT%
```

| Configuration          | Default   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `hassnippet`        | false     | In Checkmarx CX-Flow, when the hasSnippet flag is set to true, the tool displays relevant code snippets under the "Region" section of the UI. These snippets provide a portion of the code where potential vulnerabilities are detected, giving developers context to better understand the issue. This feature helps in identifying the exact location of security concerns, streamlining the remediation process by offering precise, actionable insights directly within the code. |
| `enableTextNHelpSame`        | false     | In Checkmarx CX-Flow, when the enableTextNHelpSame flag is set to true, sarif report will have same value in help and text under rules section.                                                                                                                                                                                                                                                                                                                                       |
| `enableOriginalUriBaseIds`        | false     | In Checkmarx CX-Flow, when the enableOriginalUriBaseIds flag is set to true, Sarif report will have modules details scanned in project.                                                                                                                                                                                                                                                                                                                                               |
| `srcRootPath`        | %SRCROOT% | In Checkmarx CX-Flow, when the srcRootPath has value, It will display same root path in report.                                                                                                                                                                                                                                                                                                                                                                                       |
| `sourceNodefound`        | false     | In Checkmarx CX-Flow, if the source node is not found at node 1, it will search for the next lowest node, alternatively node 1, if this boolean variable is set to true.                                                                                                                                                                                                                                                                                                                                                                             |

**Note**: Command line parameter for snippet is `--sarif.hassnippet=true`





### <a name="bitbucket">Bitbucket (Cloud and Server)</a>
```yaml
bitbucket:
  webhook-token: xxx
  token: <user>:xxx
  url: http://bitbucket.org
  api-url: http://api.bitbucket.org
  api-path: /2.0
  zero-vulnerability-summary: true
```

| Configuration                | Default | Description                                                                                                                                                                                                                                                                                                                                       |
|------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `webhook-token`              |         | Token used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request.  The Bitbucket cloud does not allow for a shared secret, therefore a URL parameter called token, must be provided in this case.                                                                                                |
| `token`                      |         | This is the API token with access to the repository with at least Read only access to code and the ability to add comments to pull requests.  BitBucket requires the **<user>:<token>** format in the configuration. <br />`userid:app password`(Format while using BitBucket Cloud) <br />`userid:password`(Format while using BitBucket Server) |
| `api-url`                    |         | - [https://api.bitbucket.org](https://api.bitbucket.org) (URL for the Cloud BitBucket)<br />- [https://api.companyxyzbitbucket](https://api.companyxyzbitbucket) (URL for the BitBucket server is just the server hostname with `api.` prefixed)                                                                                                  |
| `url`                        |         | - [https://bitbucket.org](https://api.bitbucket.org) (URL for the Cloud BitBucket)<br />- [https://companyxyzbitbucket](https://api.companyxyzbitbucket)(URL for the BitBucket server is just the server hostname)                                                                                                                                |
| `api-path`                   |         | The API URL path (appended to the URL) for BitBucket                                                                                                                                                                                                                                                                                              |
| 'scan-submitted-comment`     | true    | Comment on Merge Request with "Scan submitted (or not submitted) to Checkmarx ...".                                                                                                                                                                                                                                                               | 
| `zero-vulnerability-summary` | false   | if true, will not comment in PR decoration any details for scans as vulnerabilities are zero.                                                                                                                                                                                                                                                     |
**Note**: As mentioned in the prerequisites, a service account is required that has appropriate access to the repositories that will be scanned, pull requests that will be commented on, GitHub issues that will be created/updated.

## <a name="json">JSON Config Override</a>
The sample below illustrates an override configuration in JSON format. It has similarities with the YAML config blocks.  Its main use is to override cx-flow and Jira Yaml configuration.

for more details, please refer to [Config as Code](https://github.com/checkmarx-ltd/cx-flow/wiki/Config-As-Code)

```jsonc
{
  "version": 1.0,
  "project": "XYZ-${repo}-${branch}",
  "team": "/a/b/c",
  "sast": {
    "preset": "",
    "engineConfiguration": "",
    "incremental": "false", // values: "true" or "false"
    "forceScan": "true", // values: "true" or "false"
    "fileExcludes": "*.pyc, *.test, *.class",
    "folderExcludes": "*test, out/, *bin"
  },
  "additionalProperties": {
    "cxFlow": {
      "application": "test app",
      "branches": ["develop", "main", "master"],
      "emails": ["xxxx@checkmarx.com"],
      "bugTracker": "JIRA", // other possible values: "GitLab", "GitHub", "Azure"
      "scanResubmit": "true", // values: "true" or "false"
      "sshKeyIdentifier": "Key of the ssh-key-list parameter present in application.yml file."
      "jira": {
        "project": "APPSEC",
        "issue_type": "Bug",
        "assignee": "admin",
        "opened_status": ["Open","Reopen"],
        "closed_status": ["Closed","Done"],
        "open_transition": "Reopen Issue",
        "close_transition": "Close Issue",
        "close_transition_field": "resolution",
        "close_transition_value": "Done",
        "priorities": {
          "High": "High",
          "Medium": "High",
          "Low": "High"
        },
        "fields": [
          {
            "type": "cx", // cx, static, result
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
      "filters": {
        "severity": ["High", "Medium"],
        "cwe": ["79", "89"],
        "category": ["XSS_Reflected", "SQL_Injection"],
        "status": ["New", "Recurring"],
        "state": ["Confirmed", "To Verify"]
      }
    }
  },
  "customFields": {
    "field1": "value1",
    "field2": "value2"
  },
  "scanCustomFields": {
    "field3": "value3",
    "field4": "value4"
  }
}
```

## <a name="bugtrackers">Bug Trackers</a>

Refer to the [Bug Tracker documentation](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels) for a list of all our Bug Trackers and Feedback Channels.

## <a name="encryption">Encryption</a>
CxFlow is bundled with support for Jasypt for encrypting property files:
http://www.jasypt.org/

Sample:
https://www.ricston.com/blog/encrypting-properties-in-spring-boot-with-jasypt-spring-boot/

To avoid storing the decryption password in the YAML configuration, invoke CxFlow with the password and the encryption algorithm specified on the command line:

```bash
$ java -jar cx-flow-<version>.jar \
    --jasypt.encryptor.password=passphrase \
    --jasypt.encryptor.algorithm=PBEWITHHMACSHA512ANDAES_256
```

If desired, the password can also be specified in the YAML configuration file:

```yaml
jasypt:
  encryptor:
    password: passphrase
    algorithm: PBEWITHHMACSHA512ANDAES_256
```

**NOTE**: Choose the appropriate algorithm for your deployment. This is a sample.

Environment variables can be leveraged for injecting the required jasypt cli values specified above:

```bash
JASYPT_ENCRYPTOR.PASSWORD=passphrase
JASYPT_ENCRYPTOR.ALGORITHM=PBEWITHHMACSHA512ANDAES_256
```

To create encrypted text, use the following command line to produce a base-64 encoded string:

```bash
$ java -cp cx-flow-<version>.jar \
    -Dloader.main=org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
    org.springframework.boot.loader.PropertiesLauncher \
    ivGeneratorClassName=org.jasypt.iv.RandomIvGenerator \
    input="stuff to encrypt goes here" \
    password=passphrase \
    algorithm=PBEWITHHMACSHA512ANDAES_256
    
```
Encrypted values can be assigned to fields in the YAML configuration file using the format **ENC(\<base-64 encoded ciphertext\>)**.  Example:
```
jira:
   token: ENC(r2Q0H31voNvp4NWHQ3bSkNzxmguHnHe/fuA+JiJ6DeOt8Fbzcslm6Hlly78dm6RONSaL8lGywG5atPC0xzyCsA==)
```
## <a name="external">External Scripting</a>
There are places where a custom **groovy** script can be used while executing CxFlow.  These include:
* Deciding which branch is applicable for scanning.
* The project name to be used.
* The team to be used.

For additional information, refer to the [External Scripting](https://github.com/checkmarx-ltd/cx-flow/wiki/External-Scripts) chapter.

## <a name="outputscanid">SAST Scan ID in Github Action Output variable</a>
If user want to use SAST Scan ID for further usage cx-flow stores SCAN ID in githuab output variable name : **cxflowscanid**

```
- name: Checkmarx CxFlow Action
      id: step1
      uses: cx-flow/checkmarx-cxflow-github-action@v1.6
        project: ${{ github.event.repository.name }}
        team: ${{ secrets.CHECKMARX_TEAMS }}
        checkmarx_url: ${{ secrets.CHECKMARX_URL }}
        checkmarx_username: ${{ secrets.CHECKMARX_USERNAME }}
        checkmarx_password: ${{ secrets.CHECKMARX_PASSWORD }}
        checkmarx_client_secret: ${{ secrets.CHECKMARX_CLIENT_SECRET }}
        scanners: sast
        params: --github --checkmarx.incremental=false --checkmarx.settings-override=true --namespace=${{ github.repository_owner }} --repo-name=${{ github.event.repository.name }} --branch=${{ github.ref_name }} --cx-flow.filter-severity --cx-flow.filter-category --checkmarx.disable-clubbing=true --repo-url=${{ github.event.repository.url }}
   
```
Steps to retrieve SCAN ID**** in output variable -

* Since Scan ID we are getting only after run of cx-flow, So we will use ID of Checkmarx CxFlow Action steps in output variable to fetch SCAN ID
```
outputs:
      output1: ${{ steps.step1.outputs.cxflowscanid }}
```
* Now SCAN ID is stored in output1 variable which can be used in any jobs as per user convince.


**NOTE**: If SAST scan is taking time to scan files and other jobs are stuck due to this so user can run cx-flow in Async mode and with the help of SCAN ID from output variable, User can fetch results.
In This way there is no jobs will be blocked due to processing of cx-flow.

## <a name="awslogs">Streaming CxFlow logs to AWS OpenSearch or ElasticSearch</a>

### Step 1: Create a Logback logging configuration file
Create an XML file named `logback-spring.xml` with a [Logback](https://logback.qos.ch/documentation.html) configuration similar to the XML below:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <layout class="ch.qos.logback.classic.PatternLayout">
      <pattern>%d{dd-MM-yyyy HH:mm:ss.SSS} %magenta([%thread]) %highlight(%-5level) %logger{36}.%M - %msg%n</pattern>
    </layout>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <encoder>
      <pattern>%d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] %-5level %logger{36}.%M - %msg%n</pattern>
    </encoder>

    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>/logs/cxflow.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
      <maxFileSize>10MB</maxFileSize>
      <totalSizeCap>20GB</totalSizeCap>
      <maxHistory>7</maxHistory>
    </rollingPolicy>
  </appender>

  <logger name="com.checkmarx" level="INFO" />
  <logger name="org.apache.http.wire" level="ERROR" />
  <logger name="org.springframework.ws" level="ERROR" />

  <root level="INFO">
    <appender-ref ref="FILE" />
    <appender-ref ref="CONSOLE" />
  </root>
</configuration>
```
Place this file in a location where CxFlow can access it during execution.

### Step 2: CxFlow logging configuration
Remove all logging configuration from the CxFlow configuration YAML file and replace it with this line:

`logging.config: logback-spring.xml`

Provide the full path of the `logback-spring.xml` file.

### Step 3: Configure a Logstash pipeline to tail the CxFlow logs
This example is showing the pipeline configured for the AWS OpenSearch version of Logstash. The ElasticSearch version of Logstash
is configured similarly. Please consult the appropriate Logstash documentation for a configuration that best fits your needs.

The pipeline configuration should look similar to the following example:
```
input {

    file {
        check_archive_validity => true
        path => "/cxflow-logs/*.log"
    }
}


output {

    opensearch {
        hosts => ["https://<your host url here>:443/"] 
        index => "cxflow-logs"
        user => "<username>"
        password => "<password>"
        ecs_compatibility => "disabled"
        ssl_certificate_verification => false
    }
}
```


Adjust the `path` configuration element as appropriate to tail logs where you set the CxFlow logs to be stored in the XML file created in Step 1. The OpenSearch configuration elements will need to be adjusted to fit your appropriate storage requirements for OpenSearch/ElasticSearch.


### Step 4: Start CxFlow and Logstash
If Logstash has been configured correctly, the CxFlow logs will now be tailed and sent to OpenSearch/ElasticSearch.

## <a name="issuelbls">Issue Labels</a>
* We can label SAST issue in GITHUB and GITLAB.If user want to create issue he can pass labels in following way-
```
github:
  webhook-token: 12345
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true
  error-merge: true
  max-description-length : 400
  cx-summary: true
  issueslabel:
    high: high,must fix
    medium: Medium,Not critical
    low: ignore
  #max-delay : 3

gitlab:
  webhook-token: 12345
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  block-merge: true
  issueslabel:
    high: high,must fix
    medium: Medium,Not critical
    low: ignore
```
* User can also pass this as command line parameter.
```
--gitlab.issueslabel.medium="Medium,Not critical" --gitlab.issueslabel.low="ignore" --gitlab.issueslabel.high="high,must fix" #assigns 2 labels, high and must fix" --gitlab.issueslabel.info="very low"
--github.issueslabel.medium="Medium,Not critical" --github.issueslabel.low="ignore" --github.issueslabel.high="high,must fix" #assigns 2 labels, high and must fix" --github.issueslabel.info="very low"
```


### <a name="jasypt">Jasypt </a>
```yaml
jasypt:
  encryptor:
    password: key
    algorithm: PBEWithMD5AndDES
    iv-generator-classname: org.jasypt.iv.NoIvGenerator
    isBase64: false
```

| Configuration          | Default | Description                                                                  |
|------------------------|---------|------------------------------------------------------------------------------|
| `isBase64`        | false   | If isBase64 is true user can pass base64 encryption password key to cx-flow. |



