* [Monitoring](#monitoring)
* [Encryption](#encryption)
* [External Scripting](#external)
* [Main (Global) Properties](#main)
* [Configuration Definitions](#configuration)
  * [9.0 Configuration Changes](#nine)
  * [Filtering](#filtering)
  * [Excluding Files from Zip Archive](#excludezip)
  * [Break build](#break)
  * [Override SAST project setting](#override)
* [WebHook Configuration](#webhook)
  * [WebHook URL Parameters - Code](#code)
  * [WebHook URL Override Parameters - Details](#details)
* [Repository configuration blocks](#repository)
  * [GitHub](#github)
  * [GitLab](#gitlab)
  * [Azure DevOps](#azure)
  * [Bitbucket (Cloud and Server)](#bitbucket)
* [JSON Config Override](#json)
* [BugTrackers](#bugtrackers)

CxFlow uses **Spring Boot** and, for Server Mode, it requires an `application.yml` file to drive the execution. The sections below outlines available properties and when/how they can be used in different execution modes. In addition, all the Spring Boot configuration rules apply. For additional information on Spring Boot, refer to 
https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html

Command-line arguments and environment variables prevail over values specified in `application.yml` file. To allow for bootstrapping the launch process with various configurations, especially with containers, CxFlow uses overrides on the command line using the `--property.name=Value` format as well as `PROPERTY_NAME` environment variable overrides.

All the relevant configuration is defined by the `application.yml` file that resides in the same directory as the JAR file, or if an explicit configuration override is provided on the command line as follows:

```bash
$ java -jar cx-flow-<version>.jar \
    --spring.config.location=/path/to/application.yml
```

## <a name="main">Main (Global) Properties</a>

### Application Properties Sample

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
     - Reoccured
  filter-state:
     - Confirmed
     - Urgent
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://cxa.codebashing.com/courses/
  track-application-only: false
  web-hook-queue: 20
  scan-result-queue: 8
  break-build: false
  scan-resubmit: false
  preserve-project-name: false
  http-connection-timeout: xxx # milliseconds - default 30000
  http-read-timeout: xxx # milliseconds - default 120000
  mail:
     host: smtp.gmail.com
     port: 587
     username: xxx
     password: xxx
     enabled: true
  zip-exclude: \.git/.*, .*\.png

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
  jira-project-field: jira-project
  jira-issuetype-field: jira-issuetype
  jira-custom-field: jira-fields
  jira-assignee-field: jira-assignee
  preserve-xml: true
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
  cx-branch: false

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
  url: https://api.bitbucket.org
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

## <a name="configuration">Configuration Definitions</a>
Refer to the sample configuration above for the entire YAML structure.

### `server` section

```yaml
server:
  port: ${PORT:8080}
```

| Config                    | Default               | Required | WebHook | Command Line | Notes                                                                    |
|---------------------------|-----------------------|----------|---------|--------------|--------------------------------------------------------------------------|
| `port`                    | 8080                  | No       | Yes     | No           | The default value is 8080 unless an environment variable port is defined |

### `cx-flow` section

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
     - Reoccured
  filter-state:
     - Confirmed
     - Urgent
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://cxa.codebashing.com/courses/
  track-application-only: false
  web-hook-queue: 20
  scan-result-queue: 8
  break-build: false
  scan-resubmit: false
  preserve-project-name: false
  http-connection-timeout: xxx # milliseconds - default 30000
  http-read-timeout: xxx # milliseconds - default 120000
  mail:
     host: smtp.gmail.com
     port: 587
     username: xxx
     password: xxx
     enabled: true
  zip-exclude: \.git/.*, .*\.png
```

| Config                    | Default               | Required | WebHook | Command Line | Notes                                                                    |
|---------------------------|-----------------------|----------|---------|--------------|--------------------------------------------------------------------------|
| `contact`                 |                       | No       |         |              | Contact email for the CxFlow administrator                               |
| `bug-tracker`             |                       | Yes      | Yes     | Yes          | Must be one of the following: <br />- None<br />- Jira<br />- Email<br />- Any value specified in the bug-tracker-impl custom bean implementations list (a white list of bug tracker implementations)<br /><br /> **Note**:  JIRA/EMAIL/NONE are built in and not required in the bug-tracker-impl list |
| `bug-tracker-impl`        |                       | No (Only if using one of the applicable bug tracker implementations) | Yes | Yes | List of available bug trackers (feedback channels). Currently support for: <br />- Csv<br />- Json<br />- CxXML<br />- GitLab<br />- GitHub<br />- Azure<br />- Rally |
| `branches`                |                       | No       | Yes     | No           | List of protected branches that drive scanning within the WebHook flow. If a pull or push event is initiated to one of the protected branches listed here, the scan is initiated. For example:<br />- develop<br />- main<br />- security<br />- release-\w+<br /><br />If no value is provided, all branches are applicable.<br /><br />Regular expressions are supported. (i.e. release-\w+ will match any branches starting with "release-") |
| `branch-script`           |                       | No       | Yes     | No           | A **groovy** script that can be used to decide, if a branch is applicable for scanning. This applies to any client custom lookups and other integrations.  The script is passed as a **"request"** object of the type **com.checkmarx.flow.dto.ScanRequest** and must return **boolean** (true/false). If this script is provided, it is used for all decisions associated with determining applicability for a branch event to be scanned. **A sample script is attached to this page. |
| `filter-severity`         |                       | No       | Yes     | Yes          | The severity can be filtered during feedback (**High**, **Medium**, **Low**, **Informational**).  If no value is provided, all severity levels are applicable. |
| `filter-category`         |                       | No       | Yes     | Yes          | The list of vulnerability types to be included with the results (**Stored_XSS**, **SQL_Injection**) as defined within Checkmarx.  If no value is provided, all categories are applicable. |
| `filter-cwe`              |                       | No       | Yes     | Yes          | The list of CWEs to be included with the results (**79**, **89**).  If no value is provided, all categories are applicable. |
| `filter-state`            |                       | No       | Yes     | Yes          | The available options are **To Verify**, **Confirmed**, **Urgent** and **Proposed Not Exploitable**.  This only allows for filtering the results that have been confirmed/validated within Checkmarx. |
| `mitre-url`               |                       | No       | Yes     | Yes          | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues** to help guide developers.  The link is not provided, if left empty or omitted. |
| `wiki-url`                |                       | No       | Yes     | Yes          | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues** associated with internal program references (program/assessment methodology, remediation guidance, etc).  The link is not provided, if left empty or omitted. |
| `codebash-url`            |                       | No       | Yes     | Yes          | Provides a link in the issue body for **Jira**, **GitLab Issues** and **GitHub Issues**  associated with training. The link is titled **'Training'** and is not provided, if left empty or omitted. |
| `track-application-only`  | false                 | No*      | Yes     | Yes          |                                                                          |
| `web-hook-queue`          | 100                   | No*      | Yes     | No           | The maximum number of active scans initiated via WebHook at a given time. Requests remain queued until a slot is free. |
| `scan-result-queue`       | 4                     | No*      | Yes     | Yes          | The maximum number of scan results being processed at the same time. Requests remain queued until a slot is free. <br />As XML files can become large, it is important to limit the number that can be processed at the same time. |
| `break-build`             | false                 | No*      | No      | Yes          | A non-zero return code (10) is provide when any of the filtering criteria is met within scan results. |
| `http-connection-timeout` | 30000                 | No*      | Yes     | Yes          | Http client connection timeout setting.  Not applied for the Jira client. |
| `http-read-timeout`       | 120000                | No*      | Yes     | Yes          | Http client read timeout setting.  Not applied for the Jira client. |
| `mail`                    | enabled:false         | No*      | Yes     | Yes          | SMTP configuration - host, port, username, password, enabled (false by default).  When enabled, email is a valid feedback channel, and an html template is used to provide result details. During WebHook execution, the email is sent to the list of committers in the push event.
| `zip-exclude`             |                       | No       | No      | Yes          | Comma-separated list of regexes. Instructs CxFlow to exclude specific files when it creates a zip archive. See the details [here](Excluding-Files-from-Zip-Archive.md).|
| `auto-profile`            | false                 | No       | Yes     | No           | During WebHook execution, language stats and files are gathered to help determine an appropriate preset to use.  By default, the profiling initially occurs only when a project is new/created for the first time.  Refer to [CxFlow Automated Code](https://checkmarx.atlassian.net/wiki/spaces/PTS/pages/1345586126/CxFlow+Automated+Code+Profiling) Profiling for details.
| `always-profile`          | false                 | No       | Yes     | No           | This enforces the auto-profile execution for each scan request regardless of whether the project is new or not. |
| `profiling-depth`         | 1                     | No       | Yes     | No           | The folder depth that is inspected for file names during the profiling process, which means looking for specific file references, i.e. web.xml/Web.config |
| `profile-config`          | CxProfile.json        | No       | Yes     | No           | The file that contains the profile configuration mapping. |
| `scan-resubmit`           | false                 | No       | Yes     | No           | When **True**: If a scan is active for the same project, CxFlow cancels the active scan and submits a new scan. When **False**: If a scan is active for the same project, CxFlow does not submit a new scan. |
| `preserve-project-name`   | false                 | No       | Yes     | Yes          | When **False**: The project name will be the repository name after normalization (i.e. Front-End-dev). Legal characters are: `a-z`, `A-Z`, `0-9`, `-`, `_`, `.`. All other characters will be replaced in the normalization process with "-". <br/> When **True**: The project name will be the exact project name inputted without normalization (i.e. Front End-dev). <br/> **For attention:** <br/> 1. Not all scanners allow project names with invalid characters.<br/> 2. The preserve-project-name parameter is also effective for project name coming from config-as-code. |

No* = Default is applied

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

#### <a name="excludezip">Excluding Files from Zip Archive</a>

The `cx-flow.zip-exclude` configuration option instructs CxFlow to exclude specific files when it creates a zip archive.

##### Example
The following option excludes all `.png` files from the archive, as well as all files inside a root-level `.git` directory:

```yaml
cx-flow:
  zip-exclude: \.git/.*, .*\.png
```

##### Details
The meaning and syntax of the `cx-flow.zip-exclude` option are different as opposed to the `checkmarx.exclude-folders` and `checkmarx.exclude-files` options.

| `cx-flow.zip-exclude`                                | `checkmarx.exclude-folders`, `checkmarx.exclude-files` |
|------------------------------------------------------|---------|
| Uses regexes                                         | Use wildcards |
| Works locally, before the sources are sent for scan  | Work in CxSAST when it already has the sources |

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

#### <a name="break">Break Build</a>
The configuration can be set or overridden at execution time using the command line (`--cx-flow.break-build=true`) to exit the command line execution flow for a single project result or scan for results that meet the filter criteria.

**Note**:  This does not apply to WebHooks or for batch cli execution (instance and team).  It only works, if one project result is processed.


### `checkmarx` section

| Config                  | Default              | Required | WebHook | Command Line | Notes                                                                    |
|-------------------------|----------------------|----------|---------|--------------|--------------------------------------------------------------------------|
| `username`              |                      | Yes      | Yes     | Yes      | Service account for Checkmarx                                             |
| `password`              |                      | Yes      | Yes     | Yes      | Service account password Checkmarx                                        |
| `client-secret`         |                      | Yes      | Yes     | Yes      | OIDC client secret for API login to Checkmarx                             |
| `base-url`              |                      | Yes      | Yes     | Yes      | Base FQDN and port for Checkmarx                                          |
| `multi-tenant`          | false                | No*      | Yes     | Yes (Scan only) | If yes, the name space is created or reused, if it has been pre-registered or already created for previous scans)    |
| `scan-preset`           | Checkmarx Default    | No*      | Yes     | Yes (Scan only) | The default preset used for the triggered scan                 |
| `configuration`         | Default Configuration | No*      | Yes     | Yes (Scan only) | Checkmarx scan configuration setting |
| `team`                  |                      | Yes (not for XML parse mode) | Yes | Yes (Scan only)  | Base team in Checkmarx to drive scanning and retrieving of results |
| `scan-timeout`          | 120                  | No*      | Yes     | Yes (scan only) | The amount of time (in minutes) that CxFlow will wait for a scan to complete to process the results.  The Checkmarx scan remains as is, but no feedback is provided |
| `jira-project-field`    | jira-project         | No       | Yes     | Yes | Custom Checkmarx field name to override Jira Project setting for a given Checkmarx scan result / project |
| `jira-issuetype-field`  | jira-issuetype       | No       | Yes     | Yes | Custom Checkmarx field name to override Jira Issue Type settings for a given Checkmarx scan result / project |
| `jira-custom-field`     | jira-fields          | No       | Yes     | Yes | Custom Checkmarx field name to override Jira custom field mappings for a given Checkmarx scan result / project |
| `jira-assignee-field`   | jira-assignee        | No       | Yes     | Yes | Custom Checkmarx field name to override Jira assignees for a given Checkmarx scan result / project |
| `preserve-xml`          | false                | No*      | Yes     | Yes | This flag is used to preserve the original XML results retrieved by the Checkmarx scan inside the ScanResults object to be later used by a Custom bug tracker implementation, if required.  Currently, **CxXMLIssueTracker** uses this flag |
| `incremental`           | false                | No*      | Yes     | Yes | Enables support for incremental scan support when CxFlow is triggering scans.  The incremental-num-scans and incremental-threshold values must not be exceeded for the last available full scan criteria. |
| `incremental-num-scans` | 5                    | No*      | Yes     | Yes (scan only) | The maximum number of scans before a full scan is required |
| `project-script`        |                      | No       | Yes     | Yes | A **groovy** script that can be used for deciding the name of the project to create/use in Checkmarx. This is to allow for any client custom lookups and other integrations.  The script is passed a "**request**" object, which is of type **com.checkmarx.flow.dto.ScanRequest**, and must return **String** representing the **team name** to be used. If this script is provided, it is used for all decisions associated with the determining project name |
| `team-script`           |                      | No       | Yes     | Yes | A **groovy** script that can be used for deciding the team to use in Checkmarx.  This is to allow for any client custom lookups and other integrations.  The script is passed a "request" object, which is of type **com.checkmarx.flow.dto.ScanRequest**, and must return **String** representing the team path to be used. If this script is provided, it is used for all decisions associated with determining project name.
| `incremental-threshold` | 7                    | No*      | Yes     | Yes (scan only) | The maximum number of days before a full scan is required |
| `offline`               | false                | No*      | No      | Yes (parse only) | Use Table this only when parsing Checkmarx XML, this flag removes the dependency from Checkmarx APIs when parsing results.  This skips retrieving the issue description from Checkmarx. |
| `exclude-files`         |                      | No       | Yes     | Yes      | Files to be excluded from Scan                                            |
| `exclude-folders`       |                      | No       | Yes     | Yes      | Folders to be excluded from Scan                                          |
| `custom-state-map`      |                      | No       | No      | Yes      | A map of custom result state identifiers to custom result state names |
| `cx-branch`             | false                | No       | Yes     | Yes      | A Flag to enable branching of projects in cxSAST server. |

No* = Default is applied

#### Branched Projects

A branched project is a child of a base project. Only the base project will take  space of a project in CxSAST server and all the branched project will be under the base project. The first scan performed for any new project will create a base project and any event generated afterwards from any branches of the project will create branched project. Branching of projects can be enabled by setting the `cx-branch` property to true. Any PUSH/PULL event generated from a branch creates a branched project having the project name as base project's name followed by the branch name.

**Example**
* When a **PUSH\PULL** scan is generated for a new project from a branch e.g `master` of repo `ABC` a base project will be created in CxSAST with name `ABC-master`.
* When any **PUSH/PULL** event is generated from a branch `XYZ`  of a repo `ABC` and a base project is present in CxSAST server  then  a branched project will be created with name `ABC-XYZ`. 

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
   portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
   exclude-files: "*.tst,*.json"
   exclude-folders: ".git,test"
```
**Note:**
* Make sure to include `version: 9.0` (or higher) and `scope: access_control_api sast_rest_api`
* The Team path must include the unix path separator **/**, the path is for example defined as follows: `/CxServer/Checkmarx/CxFlow`


### <a name="override">Override project settings</a>
The configuration can be set to override project settings with Cxflow configuration when triggering new scan for SAST project, or to avoid project setting update if property set to 'false'
```yaml
checkmarx
  ...
  settings-override: true #default false if not provide
```


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
@RequestParam(value = "state", required = false) List<String> state
```

### <a name="details">WebHook URL Override Parameters - Details</a>
These parameters are related to the WebHook URL parameters above.

| Configuration     | Description            |
|-------------------|------------------------|
| `application`     | Override the application name, which is directly linked to Jira and other defect management implementations for tracking purposes. |
| `branch`          | Override the protected branches that drive the scan. For multiple branches, simply list the branch multiple times. i.e. `branch=XXX&branch=YYYY` |
| `severity`        | Override the severity filters. For multiple severity simply list multiple times, i.e. `severity=High&severity=Medium`    |
| `cwe`             | Override the cwe filters. For multiple cwe, simply list the cwe multiple times, i.e. `cwe=89&cwe=79` |
| `category`        | Override the category filters. For multiple category, simply list category multiple times, i.e. `category=Stored_XSS&category=SQL_Injection` |
| `project`         | Override the project name that will be created/used in Checkmarx. This allows for greater flexibility for incremental scan relating to pull requests,  i.e. use a standardized pull project name that is always used regardless of the branch - `?project=repo-pull` |
| `team`            | Override the team within Checkmarx to use/create project under. |
| `state`           | Override the state filters (Confirmed/Urgent). For multiple state, simply list the state multiple times, i.e. `status=Confirmed&status=Urgent` |
| `status`          | Override the status filter. For multiple status, simply list the status multiple times, i.e. `status=New&status=Reoccured` |
| `assignee`        | Override the assignee  |
| `preset`          | Override the Checkmarx preset rules for scanning |
| `incremental`     | Override incremental property to enable/disable incremental scan support |
| `exclude-files`   | Override file exclusions |
| `exclude-folders` | Override folder exclusions |
| `override`        | Override a complete **JSON** blob as defined below |
| `bug`             | Override the default configured bug |
| `app-only`        | This forces Jira issues to be tracked according to the defined application / repo name, as opposed to defining uniqueness per namespace/repo/branch |

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
```

| Configuration            | Default        | Description     |
|--------------------------|----------------|-----------------|
| `webhook-token`          |                | Token used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request. GitHub signs the request with this value, and the signature is validated on the receiving end. |
| `token`                  |                | The API token with access to the repository, with at least Read only access to the code, the ability to add comments to pull requests, and the ability to create GitHub Issues. |
| `url`                    |                | Main repo url for GitHub |
| `api-url`                |                | The API endpoint for GitHub, which is a different context or potentially FQDN than the main repo url.
| `false-positive-label`   | false-positive | A label that can be defined within the GitHub Issue feedback that is used to ignore issues |
| `block-merge`            | false          | When triggering scans based on PullRequest, this will create a new status of pending, which will block the merge ability until the scan is complete in Checkmarx. |
| `scan-submitted-comment` | true           | Comment on PullRequest with "Scan submitted (or not submitted) to Checkmarx ...". | 

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
```

| Configuration          | Default        | Description      |
|------------------------|----------------|------------------|
| `webhook-token`        |                | Token used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request. |
| `token`                |                | This is the API token with access to the repository, with at least Read only access to code, the ability to add comments to pull requests, and the ability to create GitLab issues. |
| `url`                  |                | Main repo url for GitLab. |
| `api-url`              |                | The API endpoint for GitLab, which serves a different context or potential FQDN than the main repo url. |
| `false-positive-label` | false-positive | A label that can be defined within the GitLab Issue feedback to ignore issues |
| `block-merge`          | false          | When triggering scans based on Merge Request, the Merge request is marked as WIP in GitLab, which blocks the merge ability until the scan is complete in Checkmarx. |
| `scan-submitted-comment` | true           | Comment on Merge Request with "Scan submitted (or not submitted) to Checkmarx ...". | 

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
```

| Configuration          | Default        | Description      |
|------------------------|----------------|------------------|
| `webhook-token`        |                | **<user>:<token>** as defined when registering the event in ADO.  Used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request. |
| `token`                |                | This is the API token with access to the repository. It has at least Read only access to code, the ability to add comments to pull requests, and the ability to create Azure WorkItems. |
| `url`                  |                | Main repo url for Azure DevOps, including high level namespace.  **Note**: this is only required when running from the command line and not for WebHooks. |
| `issue-type`           | issue          | The WorkItem type within Azure, i.e. issue / impediment. |
| `issue-body`           | description    | The body to enter free text regarding the issue.  The default across various workItem types are **Description** or **System.Description**. |
| `app-tag-prefix`       | app            | Used for tracking existing issues.  Issues are tagged with this value, if **app** is provided (without namespace/repo/branch) | 
| `owner-tag-prefix`     | owner          | Used for tracking existing issues.  Issues are tagged with this value |
| `repo-tag-prefix`      | repo           | Used for tracking existing issues.  Issues are tagged with this value |
| `branch-label-prefix`  | branch         | Used for tracking existing issues.  Issues are tagged with this value |
| `api-version`          | 5.0            | Azure DevOps API version to use |
| `open-status`          |                | Status when re-opening a a workItem |
| `closed-status`        |                | Status when closing a workItem |
| `false-positive-label` | false-positive | A label/tag that can be defined within the Azure Issue feedback being used to ignore issues. |
| `block-merge`          | false | When triggering scans is based on pull request, this marks the Pull in blocked state until the scan is complete at Checkmarx. |

**Note**: A service account is required with access to the repositories that are scanned, pull requests that are commented on, and Azure WorkItems that are created/updated.

### <a name="bitbucket">Bitbucket (Cloud and Server)</a>
```yaml
bitbucket:
   webhook-token
   token: <user>:xxx
   url: [http://api.bitbucket.org](http://api.bitbucket.org)
   api-path: /2.0
```

| Configuration            | Default | Description |
|--------------------------|---------|-------------|
| `webhook-token`          |         | Token used as a shared secret when calling the CxFlow WebHook WebService.  It authenticates users for the request.  The Bitbucket cloud does not allow for a shared secret, therefore a URL parameter called token, must be provided in this case. |
| `token`                  |         | This is the API token with access to the repository with at least Read only access to code and the ability to add comments to pull requests.  BitBucket requires the **<user>:<token>** format in the configuration. |
| `url`                    |         | - [https://api.bitbucket.org](https://api.bitbucket.org) (URL for the Cloud BitBucket)<br />- [https://api.companyxyzbitbucket](https://api.companyxyzbitbucket) (URL for the BitBucket server is just the server hostname with `api.` prefixed)|
| `api-path`               |         | The API URL path (appended to the URL) for BitBucket |
| 'scan-submitted-comment` | true    | Comment on Merge Request with "Scan submitted (or not submitted) to Checkmarx ...". | 

**Note**: As mentioned in the prerequisites, a service account is required that has appropriate access to the repositories that will be scanned, pull requests that will be commented on, GitHub issues that will be created/updated.

## <a name="json">JSON Config Override</a>
The sample below illustrates an override configuration in JSON format. It has similarities with the YAML config blocks.  Its main use is to override cx-flow and Jira yaml configuration.

```jsonc
{
   "application": "test app",
   "branches": ["develop", "main"],
   "incremental": true,
   "scan_preset": "Checkmarx Default",
   "exclude_folders": "tmp/,test/",
   "exclude_files": "*.tst,*.tmp",
   "emails": ["xxxx@checkmarx.com"],
   "filters": {
     "severity": ["High", "Medium"],
     "cwe": ["79", "89"],
     "category": ["XSS_Reflected", "SQL_Injection"],
     "state": ["Confirmed", "New"]
   },
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
           "type": "cx", //cx, static, result
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
* Deciding which branch is applicable for scanning (Sample script attached to this page)
* The project name to be used
* The team to be used
  For additional information, refer to the configuration options above.
