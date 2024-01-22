* [Understanding the Data](#data)
* [Understanding the result summary](#resultsummary)
* [Understanding issue creation process](#issuecreation)
  * [Issue creation for all bug trackers except JIRA](#forallbt)
  * [Issue creation when bug tracker is JIRA](#forjirabt)
* [Jira](#jira)
  * [Credentials](#cred) 
  * [Label Prefix](#labelprefix)
  * [Priorities](#priorities)
  * [Transitions](#transitions)
  * [Fields](#fields)
  * [Assigning tickets to a user](#assigningtickets)
  * [Configuring the Jira Issue Summary](#issuesummaryformat)
  * [Jira Issue Handling](#issuehandling)
  * [Adding Certifications](#certs)
  * [Jira Timeout](#timeout)
* [Custom Bug trackers](#custom)
* [Azure DevOps Work Items](#azure)
* [GitLab Issues](#gitlab)
* [GitLab Security Dashbaord](#dashboard)
* [GitHub Issues](#github)
* [Rally Issues](#rally)
* [Sarif](#sarif)
* [Service Now](#service)
* [CxXML](#cxxml)
* [Json](#json)
* [CSV](#csv)
* [Email](#email)
* [NONE|WAIT](#none)
* [PDF](#pdf)

##  <a name="data">Understanding the Data</a>
Checkmarx results are processed according to the following [scheme](https://raw.githubusercontent.com/checkmarx-ltd/cx-flow/develop/src/main/resources/samples/cx.xsd).

The breakdown of the XML includes the following:

**Query→ Result→ Path**

Issues are filtered based on the criteria found in the main configuration of CxFlow along with any particular overrides (severity, category, cwe, status).  From the XML structure, the **Source** identifier is the main tracking element.  The **Vulnerability+File** path is the key, and as additional line references are found for the same key, it is appended to the same issue reference.  See the development section for details on the ScanResults/Issue object structure.

The best fix location would be a more appropriate value to track, which is currently unavailable.

## <a name="resultsummary">Understanding the Result Summary</a>
Once scan is finished and the results are retrieved then, cx-flow combines all issues in a single file that have the same category into a single issue, then the provided overrides for filters(severity,cwe,status,category,etc) are applied on those issues and a final list of Xissues is generated. Issues in the provided bug tracker are opened for the generated Xissues.

**Example**</br>
If **12 Medium level issues** were found in a file **src/authService.js** as described below

| Sr. No. | Line Number | Issue Category                                                               |
|---------|-------------|------------------------------------------------------------------------------|
| 1       | 56 58       | Client HTML5 Insecure Storage\Path 1: -1(Clubbed with 5)                     |
| 2       | 54 58       | Client HTML5 Insecure Storage\Path 2:                                        |
| 3       | 165 167     | Client HTML5 Insecure Storage\Path 3: -1(Clubbed with 6)                     |
| 4       | 161 167     | Client HTML5 Insecure Storage\Path 4:                                        |
| 5       | 56 58       | Client HTML5 Insecure Storage\Path 4: -2(Clubbed with 1)                     |
| 6       | 165 167     | Client HTML5 Insecure Storage\Path 4: -2(Clubbed with 3)                     |
| 7       | 58 58       | Client HTML5 Insecure Storage\Path 7:                                        |
| 8       | 167 167     | Client HTML5 Insecure Storage\Path 8:                                        |
| 9       | 58 58       | Client HTML5 Store Sensitive data In Web Storage\Path 1: -2(Clubbed with 10) |
| 10      | 58 58       | Client HTML5 Store Sensitive data In Web Storage\Path 2: -1(Clubbed with 9)  |
| 11      | 167 167     | Client HTML5 Store Sensitive data In Web Storage\Path 3: -2(Clubbed with 12) |
| 12      | 167 167     | Client HTML5 Store Sensitive data In Web Storage\Path 4: -1(Clubbed with 11) |

Issues **[1]**, and **[5]** are clubbed as the category of the issue and the filename is same. Similarly, issues **[3]** and **[6]**, **[9]** and **[10]**, and **[11]** and **[12]** are clubbed. 

Since the issues are clubbed, the total count of **12 Medium issues** is now converted to **8 Medium issues**.
[[/Images/CxSAST_Result_Summary.PNG|CxSAST Result Summary]]
In the above image the vulnerability count in **Cx-SAST Summary** and **Violation Summary** is different because **Violation Summary** displays all the clubbed and then filtered vulnerabilities count, while **Cx-SAST Summary** (and **Cx-SCA Summary**) displays all the vulnerabilities present in a project.

## <a name="issuecreation">Understanding issue creation process</a>
### <a name="forallbt">Issue creation for all bug trackers except JIRA</a>
* Initially if a project is scanned with both the scanners enabled(CxSAST and CxSCA), then based on their results issues will be generated in the configured bug-tracker. 
* Issues are created with a prefix(`CX ` for CxSAST and `CX:` for CxSCA) to differentiate the issues of scanners. 
* Now a subsequent scan is done on the same project with only one scanner enabled, then based on the results only those issues will be updated, closed or reopened which contains the prefix for the configured scanner. Issues which do not contain scanner specific prefix will not be touched(updated/closed/reopened).

### <a name="forjirabt">Issue creation when bug tracker is JIRA</a>
* In case of JIRA as a bug tracker, issues created for results of any scanner also contain the prefix like other scanners but this prefix is configurable in JIRA, hence to differentiate results of scanners, a label of scanner with the scanner name is added. example `Scanner:SCA`, `Scanner:SAST`. 
* If both the scanners are enabled to scan a project then issues in JIRA will be created based on the results of the scanner and the issues will be labelled with the scanner name. 
* Now any subsequent scan is done on the same project with only one scanner enabled, only those issues will be updated,closed or reopened for which the label `Scanner:` matches with the scanner configured in cxflow during scan.

## <a name="jira">Jira</a>
Jira has the most complex configuration use case as it supports a variety of custom fields, custom workflows and custom transitions.

```
jira:
   url: https://xxxx.atlassian.net
   username: xxxx
   token: xxxx
   token-type: <API,PASSWORD,PAT>
   project: SS
   issue-type: Application Security Bug
   label-prefix: < CUSTOM PREFIX NAME >
   priorities:
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
   http-timeout : 20000    
   sast-issue-summary-format: "[VULNERABILITY] in [PROJECT] with severity [SEVERITY] @ [FILENAME]"
   sast-issue-summary-branch-format: "[VULNERABILITY] in [PROJECT] with severity [SEVERITY] @ [FILENAME][[BRANCH]]"
   sca-issue-summary-branch-format: "[PREFIX] : [VULNERABILITY] in [PACKAGE] and [VERSION] @ [REPO].[BRANCH][POSTFIX]"
   sca-issue-summary-format: "[PREFIX] : [VULNERABILITY] in [PACKAGE] and [VERSION] @ [REPO][POSTFIX]"
   suppress-code-snippets:
      - Hardcoded_Password_in_Connection_String
      - Password_In_Comment
      - Use_Of_Hardcoded_Password
   fields:
#    - type: cx #[ cx | static | result ]
#      name: Platform # cx custom field name | cx-scan | cwe, category, severity, application, *project*, repo-name, branch, repo-url, namespace, recommendations, loc, site, issueLink, filename, language
#      jira-field-name: Application
#      jira-field-type: label #[ security | text | label | single-select | multi-select ]
     - type: cx
       name: cx-scan
       jira-field-name: Application
       jira-field-type: label
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
     - type: result
       name: loc
       jira-field-name: LOC
       jira-field-type: label
       jira-default-value: XXXXX
    - type: sca-results
      name: package-name
      jira-field-name: Package Name
      jira-field-type: label
    - type: sca-results
      name: current-version
      jira-field-name: Current Version
      jira-field-type: label
    - type: sca-results
      name: fixed-version
      jira-field-name: Fixed Version
      jira-field-type: label
    - type: sca-results
      name: newest-version
      jira-field-name: Newest Version
      jira-field-type: label
    - type: sca-results
      name: locations
      jira-field-name: Locations
      jira-field-type: label
    - type: sca-results
      name: risk-score
      jira-field-name: Risk Score
      jira-field-type: label
    - type: sca-results
      name: dev-dependency
      jira-field-name: Development
      jira-field-type: single-select
    - type: sca-results
      name: direct-dependency
      jira-field-name: Direct
      jira-field-type: single-select
    - type: sca-results
      name: outdated
      jira-field-name: Outdated
      jira-field-type: single-select
    - type: sca-results
      name: violates-policy
      jira-field-name: Violates Policy
      jira-field-type: single-select
```

### <a name="cred">Credentials</a>
Jira's credentials configuration differs for on-premises and cloud environments.

#### Cloud Configuration
Jira cloud supports token-type as api-token. To generate api-token for Jira, Please refer [Tutorials](https://github.com/checkmarx-ltd/cx-flow/wiki/Tutorials#cliprep) chapter.
In case of Jira Cloud token-type parameter is optional.
```yaml
jira:
   url: <Jira Cloud url>
   username: <Configured email address>
   token: <Jira api token>
   token-type: API
```

#### On-premise Configuration
Jira on-premises supports token-type, Personal Access Tokens and  Passwords. Provide the token value as the password if token-type is set to PASSWORD. Provide the token value as a personal access token if the token-type is PAT. 

To generate personal access token for Jira on-premise.
* Select your profile picture at the top right of the screen, then choose Profile. 
* Once you access your profile, select Personal Access Tokens in the left-hand menu.
* Select Create token.
* Give your new token a name.
* Optionally, for security reasons, you can set your token to automatically expire after a set number of days.
* Click Create

Your personal access token is created. Copy the token and store it in a safe space.
```yaml
jira:
   url: <Jira on-premise url>
   username: <Jira on-premise username>
   token: <password/personal access token>
   token-type: <PASSWORD/PAT>
```

**Note:** When using Jira on-premises, a password is expected as the value in the token if the token-type is not specified.
### <a name="labelprefix">Label Prefix</a>
```
label-prefix: < CUSTOM PREFIX NAME > 
```
The label-prefix property is used to set a custom prefix for Jira issues. If this value is not provided then CxFlow will use the default issue prefix "CX".

### <a name="priorities">Priorities</a>
```
priorities:
  High: High
  Medium: Medium
  Low: Low
  Informational: Lowest
```

The value on the left side reflects the Checkmarx severity. The value to the right reflects the priority assigned to the respective issue in Jira.

### <a name="transitions">Transitions</a>
It is very important that issues driven by CxFlow have the ability to transition to and from the open or close transition states regardless of what state the issue is in.  In the event that an issue cannot use the appropriate transition defined, it will fail.
```
open-transition: In Review
close-transition: Done*
open-status:
  - To Do
  - In Progress
  - In Review
closed-status:
  - Done
```

* open-transition → this is the transition to apply to an issue when **re-opening** an issue 
* close-transition → this is the transition to apply to an issue when **closing** an issue 
* open-status → this is a list of the available status an issue can be in that indicate the issue is still in **open** state according to Jira
* closed-status → this is a list of the available status an issue can be in that indicate the issue is still in **closed** state according to Jira

**Note:** 
* Issues that have been marked as not exploitable will not be closed if their Jira status does not match any of the list of available open-status issues.
* CxFlow ignores case when comparing statuses

### <a name="fields"> Fields</a>
* **type**
  * **static**: Used for static values (specifically requires a jira-default-value to be provided)
  * **cx**: Used to map specific Checkmarx Custom Field values
  * **result**: Used to map known values from Checkmarx results or repository/scan request details.  Refer to the Result values below.
* **name**: If cx reflects the type, it is the name of the custom field within Checkmarx.Also, **cx-scan** is the scan custom fields in type cx.
  * If **result** is provided as type, the name must be one of the following:

```
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
similarity-id - Cx Similarity ID
```

* **jira-field-name** - Custom field name in Jira (readable name, not Custom field name).  **NOTE: Configuring the jira-field-name parameter to Labels would affect issue tracking and might result in duplicate bug creation or bugs not closing or opening.**
* **jira-field-type** - Type of custom field in Jira:
  * _label_ (if using static or cx values, the CSV format is used and broken into multiple labels)
  * _text_ (applies to many custom field types: url, text box, text, etc
  * _multi-select_ (the CSV format is used and broken into multiple select values)
  * _single-select_
  * _security_ (used for issue security levels)
  * _component_ (used for build in Jira Component/s field)
* **jira-default-value** - Static value if no value can be determined for the respective field (Optional)
* **skip-update**: The value is only provided during the initial creation of the ticket and not updated during subsequent iterations
* **offset**: Used with system-date, the value of offset is added to the system date

### <a name="assigningtickets">Assigning tickets to a user</a>
Jira tickets can be assigned to a user when they are created. This can be achieved in the following way.

* As a webhook url parameter - The url parameter 'assignee' can be appended to the url in the webhook configuration and a user's email address to whom the tickets should be assigned in cloud and username in on-prim, is provided as the value of the parameter.

``` 
#Example for cloud
http://companyname.checkmarx.com?assignee=someUsersEmail@xyz.com

#Example for on-prim 
http://companyname.checkmarx.com?assignee=someUsersName 
```  
* As CLI Parameter - The CLi parameter --assignee can be used to assign the Jira ticket when they are created. This parameter can be used for scan, project , batch modes
``` 
#Example for cloud
java -jar cx-flow-1.6.36.jar --spring.config.location="C:\Cx-Flow\Jar\application.yml" --scan --github --repo-name="someRepoName" --namespace="NameSpace" --repo-url="https://github.com/YourGitURl.git" --cx-project="YourProjectName" --branch="master" --assignee="EmailAddressToAssign"

#Example for on-prim 
java -jar cx-flow-1.6.36.jar --spring.config.location="C:\Cx-Flow\Jar\application.yml" --scan --github --repo-name="someRepoName" --namespace="NameSpace" --repo-url="https://github.com/YourGitURl.git" --cx-project="YourProjectName" --branch="master" --assignee="UserName"
```
* As adding Custom Checkmarx field name 'jira-assignee' and setting value for this field as user's email address to whom the tickets should be assigned in cloud and username in on-prim
### <a name="issuesummaryformat">Configuring the Jira Issue Summary</a>

#### CX-SAST
The sast-issue-summary-format and sast-issue-summary-branch-format properties can be used to configure the issue summary of the issues that CxFlow creates in Jira for vulnerabilities detected by CxSAST. The following substitutions are performed on the properties’ values to generate the issue summary:

**[BASENAME]** → The basename of the file in which the vulnerabilities were found

**[BRANCH]** → The value of the `--branch` command line option

**[FILENAME]** → The full path of the file in which the vulnerabilities were found

**[POSTFIX]** → The issue summary’s suffix (as specified by the Jira issue-postfix property)

**[PREFIX]** → The issue summary’s prefix (as specified by the Jira issue-prefix property)

**[PROJECT]** → The Checkmarx project

**[SEVERITY]** → The severity of the vulnerability

**[VULNERABILITY]** → The vulnerability

The default Jira issue summary format (for CxSAST issues) is `[PREFIX][VULNERABILITY] @ [FILENAME][POSTFIX]` (`[PREFIX][VULNERABILITY] @ [FILENAME] [[BRANCH]][POSTFIX]` if the `--branch` command line option has been used).

#### SCA
The sca-issue-summary-format and sca-issue-summary-branch-format properties can be used to configure the issue summary of the issues that CxFlow creates in Jira for vulnerabilities detected by SCA. The following substitutions are performed on the properties’ values to generate the issue summary:

**[PACKAGE]** → The package name which is vulnerable

**[BRANCH]** → The value of the `--branch` command line option

**[VERSION]** → The version of vulnerable package

**[POSTFIX]** → The issue summary’s suffix (as specified by the Jira issue-postfix property)

**[PREFIX]** → The issue summary’s prefix (as specified by the Jira issue-prefix property)

**[PROJECT]** → The Checkmarx project

**[REPO]** → The name of Repository 

**[SEVERITY]** → The severity of the vulnerability

**[VULNERABILITY]** → The vulnerability

The default Jira issue summary format (for SCA issues) is `"[PREFIX] : [VULNERABILITY] in [PACKAGE] and [VERSION] @ [REPO][POSTFIX]"` and  (`[PREFIX] : [VULNERABILITY] in [PACKAGE] and [VERSION] @ [REPO].[BRANCH][POSTFIX]` if the `--branch` command line option has been used).

### <a name="suppressCodeSnippets">Suppressing Code Snippets</a>

When creating a Jira ticket, CxFlow will add a code snippet to the ticket. Sometimes, it is preferable to suppress the creation of code snippets as they may contain sensitive information (e.g., hard-coded passwords). The suppress-code-snippets property can be used to specify a list of vulnerabilities for which code snippets will not be created. For example:

```
   suppress-code-snippets:
      - Hardcoded_Password_in_Connection_String
      - Password_In_Comment
      - Use_Of_Hardcoded_Password
```

### <a name="issuehandling">Jira Issue Handling</a>
* Issues in JIRA are tagged with a label of `Scanner` with its value being either `SCA` or `SAST` based on the scanner result for which issue is created.
* There will be filtering of issues based on the type of scan (SCA or SAST) and issues will be updated/created/closed based on the type of scanner used to initiate the scan for that project. 
* If a scan is initiated for both SCA and SAST, issues of both the scanner types will be created or updated. Re-running scans for either SCA or SAST for the same project id will update issues for that scanner only. Issues of the other type scanner(created before re-run) will not be affected. 

### <a name="certs">Adding Certifications</a>
If you are using Jira on-premise over HTTPS, an SSL certificate is required to be imported into a keystore or you will see an error.

```
ERROR 11 --- [ main] com.checkmarx.flow.CxFlowRunner : An error occurred while processing request

148 org.springframework.web.client.ResourceAccessException: I/O error on POST request for "https://checkmarx.company.net/cxrestapi/auth/identity/connect/token":
 sun.security.validator.ValidatorException:
  PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
   unable to find valid certification path to requested target; nested exception is javax.net.ssl.SSLHandshakeException:sun.security.validator.ValidatorException: 
   PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

<br>See documentation on importing certificates here:
<br>[https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG180](https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG180)
<br>[https://www.baeldung.com/spring-boot-https-self-signed-certificate](https://www.baeldung.com/spring-boot-https-self-signed-certificate)

### <a name="timeout">Jira Timeout</a>
Please add the following parameter to increase the Jira socket timeout.

```yaml
jira:
  http-timeout : <Time in ms>
```

## <a name="custom">Custom Bug Trackers</a>
Refer to the [development section](https://github.com/checkmarx-ltd/cx-flow/wiki/Development) for the implementation approach.
To use one of the Custom Bug Trackers, the cx-flow tag within the application yaml config must have the specified Spring Boot bean under `bug-tracker-impl`:

```
cx-flow:
   contact: admin@cx.com
   bug-tracker: Json
   bug-tracker-impl:
     - CxXml
     - Csv
     - Json
     - GitLab
     - GitHub
```

Valid options for `bug-tracker-impl` are currently the following ones:
* Azure
* CxXML - Only available for SAST 8.x|9.x
* Csv
* Email
* JIRA
* Json
* GitHub
* GitLab
* GitLabDashboard
* Rally
* ServiceNow
* Sarif
* PDF

## <a name="azure">Azure DevOps WorkItems</a>
Azure DevOps work items only supports an issue body/description.  Custom/template field values are not available at present.  The available issue-type values are built/tested around issue and impediment (Scrum)
[[/Images/bug1.png|Screenshot of Azure Devops work item]]

* If user wants to change System.Title,System.Description and System.Tags they need to enable boolean variable and provide details in command or in application.yml file
```
java -jar cx-flow-1.6.44.jar --project --cx-team="CxServer" --app="abc"  --cx-project="bcd" --alt-fields="System.Title:abc" --namespace="abc" --branch="abc" --repo-name="abc"  --alt-project="abc" --azure.system-title=true
```
```
java -jar cx-flow-1.6.44.jar --project --cx-team="CxServer" --app="abc"  --cx-project="bcd" --alt-fields="System.Description:bcd" --namespace="abc" --branch="abc" --repo-name="abc"  --alt-project="abc" --azure.system-description=true
```
```
java -jar cx-flow-1.6.44.jar --project --cx-team="CxServer" --app="abc"  --cx-project="bcd" --alt-fields="System.Tags:SCA" --namespace="abc" --branch="abc" --repo-name="abc"  --alt-project="abc" --azure.system-tag-blocks=true
```
```
java -jar cx-flow-1.6.44.jar --alt-fields="System.Tags:SCA,System.Title:SC2,System.Description:SCA1"  --namespace="satyamchaurasia0219" --branch="shivam" --repo-name="satyamproject"  --alt-project="satyamproject"
```

* In YML File pass variables like this
```
azure:
  system-title: true
  system-description: true
  system-tag-blocks: true~~~~~~~~~~~~~~~~~~~~~~~~
```
## <a name="gitlab">GitLab Issues</a>
* GitLab Issues leverages the same configuration as specified for WebHook listeners → API token (**token**) and valid urls are required

```
gitlab:
   webhook-token: XXXX
   token: xxx
   url: https://gitlab.com
   api-url: https://gitlab.com/api/v4/
   false-positive-label: false-positive
   block-merge: true
```
[[/Images/bug2.png|Screenshot of GitLab issue]]

* Gitlab Project not found issue can be resolved either by passing reponame and namespace (This should be Gitlab namespace) in CLI command or by passing Project ID directly to CLI command.

```
java -jar cx-flow-1.6.44.jar --scan  --f="." --repo-name="abc"  --cx-team="CxServer" --app="GitLabSASTANDSCA"  --cx-project="GitLabSASTANDSCA" --namespace="c123a"

```

In configuration file
``` 
variables:
    CHECKMARX_DOCKER_IMAGE: "checkmarx/cx-flow"
    CHECKMARX_VERSION: "9.0"
    CHECKMARX_SETTINGS_OVERRIDE: "false"
    CHECKMARX_EXCLUDE_FILES: ""
    CHECKMARX_EXCLUDE_FOLDERS: ""
    CHECKMARX_CONFIGURATION: "Default Configuration"
    CHECKMARX_SCAN_PRESET: "Checkmarx Default"
    CHECKMARX_BASE_URL: "https://checkmarx.company.com"
    CX_FLOW_EXE: "java -jar /app/cx-flow.jar"
    CX_PROJECT: "$CI_PROJECT_NAME"
    CX_FLOW_ENABLED_VULNERABILITY_SCANNERS: sast
    CX_FLOW_BREAK_BUILD: "false"
    CX_FLOW_ZIP_EXCLUDE: "" 
    CX_PARAMS: "" 
    GITLAB_BLOCK_MERGE: "false"
    GITLAB_URL: "https://gitlab-master.company.com"
    GITLAB_API_URL: "${GITLAB_URL}/api/v4"
    GITLAB_TOKEN: "${CHECKMARX_GITLAB_TOKEN}"

checkmarx-scan:
  script:
  - ${CX_FLOW_EXE}
      --scan
      --bug-tracker="GitLab"
      --bug-tracker-impl="GitLab"
      --logging.level.org.springframework.web.client=TRACE
      --logging.level.com.checkmarx.flow.custom=TRACE
      --app="${CI_PROJECT_NAME}" 
      --namespace="${CI_PROJECT_NAMESPACE}" 
      --repo-name="${CI_PROJECT_NAME}" 
      --repo-url="${CI_REPOSITORY_URL}" 
      --cx-team="${CX_TEAM}" 
      --cx-project="${CX_PROJECT}" 
      --branch="${CI_COMMIT_BRANCH}"
      --spring.profiles.active="${CX_FLOW_ENABLED_VULNERABILITY_SCANNERS}" 
      --f=. 
  allow_failure: true
```

## <a name="dashboard">GitLab Security Dashboard</a>

Current Gitlab Schema supported by us is **15.0.4**

```
cx-flow:
   bug-tracker: GitLabDashboard
   bug-tracker-impl:
     - GitLab
     - GitLabDashboard
   filter-severity:
     - High
   mitre-url: [https://cwe.mitre.org/data/definitions/%s.html](https://cwe.mitre.org/data/definitions/%s.html)
   break-build: true
  
gitlab:
   file-path: ./gl-sast-report.json
   gitlab-dashboard-version="2.0" //We are supporting Gitlab Dashboard schema version 15.0.4 which is default version but if we user want to configure to old schema version we are also suporting 2.0,14.1.2 .
```

Default Gitlab dashboard schema is 15.0.4.

[[/Images/bug3.png|Screenshot of GitLab dashboard]]

## <a name="github">GitHub Issues</a>
GitHub Issues leverages the same configuration as specified for WebHook listeners → API token (**token**) and valid URLs are required.
```
github:
   webhook-token: xxxx
   token: xxxx
   url: https://github.com
   api-url: https://api.github.com/repos/
   false-positive-label: false-positive
   block-merge: true
```
[[/Images/bug4.png|Screenshot of GitHub issue]]

## <a name="rally">Rally Issues</a>
Rally Issues require the configuration displayed below. In addition to the API security token the Rally API requires that you provide both a project ID and a work space ID. The Rally plugin uses tags to track defects.
```
rally:
   token: xxxx
   rally-project-id: xxxx
   rally-workspace-id: xxxx
   url: [https://rallydev.com](https://rallydev.com)
   api-url: [https://rally1.rallydev.com/slm/webservice/v2.0](https://rally1.rallydev.com/slm/webservice/v2.0)
```
[[/Images/bug5.PNG|Screenshot of Rally issue]]

Locating Rally Project and Workspace IDs can be tricky, but the easiest way to locate the values is by looking up the URLs in your browser as there are not any screens that directly expose the values. 
To find the Project ID, simply log into Rally and you are taken to your default project. If you look at the URL you see the Projects OID, example:

[[/Images/bug6.PNG|Screenshot highlighting Rally project ID as part of URL]]

Remember that projects are associated with workspaces and you can find the workspace setup at the projects screen. Simply go to **Setup** by clicking the wrench icon in the upper right-hand corner, example:

[[/Images/bug7.PNG|Screenshot highlighting workspace settings wrench icon in the upper right-hand corner]]

From the setup screen click on 'Workspaces & Projects', you will see a list of workspaces, example:

[[/Images/bug8.PNG|Screenshot highlighting the Workspace and Projects icon used to navigate to a list of your workspaces]]

Click the workspace that you are are interesting in, in this case Checkmarx. You will be taken to the Workspace setup screen and now you find the associated OID by looking at the URL, for example:

[[/Images/bug9.PNG|Screenshot of workspace setup screen highlighting the OID in the URL]]

If you are logged in to Rally, you can generate your API token here. To do so, click 'Create New API' on the API Keys screen as illustrated in the example below:

[[/Images/bug10.PNG|Screenshot of generating an API token]]

From that dialog box, just name your key and provide it with **Full Access**:

[[/Images/bug11.PNG|Screenshot of the popup dialog box. Here give the API key a name and Full Access]]

The API key is created and the token appears on the API Keys screen. Now you have all the required information to configure the Rally bug tracker.

## <a name="sarif">Sarif</a>
Sarif as a bug tracker is used to generate a sarif report and uploading it to the associated GitHub repository. The default output format for GitHub Action is a SARIF output report stored in the working directory as ./cx.sarif. The file ./cx.sarif is created containing issue details based on the filtering policy of CxFlow.

[[/Images/sarif-report-1.png]]

[[/Images/sarif-report-2.png]]

[[/Images/sarif-report-3.png]]

## <a name="service">Service Now</a>
Integration with Incident records is available by adding the following configuration block:
```
servicenow:
  apiUrl: https://xxxx.service-now.com/api/now/table
  username: xxxx
  password: xxxx
```

The bug tracker must be specified as *ServiceNow*
```
cx-flow:
  # Agreed upon shared API token
  token: xxxx
  bug-tracker: ServiceNow
  bug-tracker-impl:
    - ServiceNow
    - Json
    - GitLab
      ...
```

## <a name="cxxml">CxXML</a>
The XML bug-tracker (defined as CxXml) is useful, if you want to retrieve the latest scan results per project (batch mode) from Checkmarx per project, Team, or the entire instance. This is the original XML report provided by Checkmarx. When using CxXML with both CxSAST and CxSCA scanners enabled, two seprate reports will be generated, one for CxSAST report and one for CxSCA report.

CxSCA currently does not support `--batch` mode, but retrieving latest scan for a particular project (project mode) is still possible.

When both the scanners are enabled it is a known issue that duplicate CxSAST reports are generated.

**Note**: The Checkmarx,Sca config blocks must specify `preserve-xml` as `true` for this feedback type.  
```
checkmarx:
   ...
   ...
     preserve-xml: true
     
sca:
  ....
  ....
  preserve-xml:true     
cx-xml:
   file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
   data-folder: "C:\\tmp"
```

The file system path as well as the file naming format is required.

## <a name="json">Json</a>
The JSON bug-tracker (defined as Json), is useful if you would like to retrieve all the latest scan results per project (batch mode) from Checkmarx per project, Team, or entire instance. The CxFlow JSON configuration block requires you to specify the path where reports are created and file name format to when creating reports, example:

```
json:
   file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
   data-folder: "C:\\tmp
   latest-committer-email: true
```

The report contents will be a JSON representation of the ScanResults object, which includes issues based on the filtering specified in the main config block (cx-flow). You can determine how results Checkmarx found by looking at the "scanSummary" section, and you can determine how many results CxFlow reported after applying filters by looking at the "flow-summary" section. Each vulnerability found will appear in the "xissues" list.

The boolean property `latest-committer-email` needs to be true in order to retrieve the latest committer email. By default, it is false. This property is only compatible with WEB mode.

The "XIssue" item looks like the following sample:

```
{
   // <String> that contains the vulnerability name
   "vulnerability": "Vulnerability Name",
   // <String> that contains one of the following values: 
   //    'To Verify'
   //    'Not Exploitable' 
   //    'Confirmed'
   //    'Urgent'
   //    'Proposed Not Exploitable'
   "vulnerabilityStatus": "TO VERIFY",
   // <String> that contains Cx calculated similarity ID
   "similarityId":"1051653785",
   // <String> that contains the CWE ID
   "cwe":"829",
   // <String> with detailed description of vulnerability
   "description": "Example...",
   // <String> that indicates the name of the programming language
   "language": "Objc",
   // <String> that contains one of the following values:
   //    'High'
   //    'Medium'
   //    'Low'
   //    'Info'
   "severity": "High",
   // <String> link back to issue in Checkmarx
   "link": "",
   // <String> The name of the value
   "filename": "",
   // <String> 
   "gitUrl": "",
   // <int> How many vulnerabilities are false positive.
   "falsePositiveCount": 0,
   // The details list is an associative array where vulnerability is found by looking up
   // up its line number.
   "details": {
      // Indicates instance of vulnerability found on line 33
      "33": {
          // <Boolean> true if marked as false positive
          "falsePositive": false,
          // <String> The code sample from line where vulnerability was found
          "codeSnippet": "...",
          // <String> user added comment
          "comment": ""
        },
      // Indicates instance of vulnerability found line 11
      "11": {
          "falsePositive": false,
          "codeSnippet": "...",
          "comment": ""
        }
   },
   // <Object> This allows you to track the source and sink locations for each 
   // instance of the vulnerability. NOTE: In some cases the vulnerability 
   // occurs more than once on a line and may appear like there are duplicate 
   // records. You may be able to use the "details" list if you're interested
   // in source-to-sink information.
   "additionalDetails":{
      // <String> contains back to Checkmarx where fix is described.
      "recommendedFix": "",
      // <String> describes related vulnerability categories.
      "categories": "OWASP Mobile Top 10 2016;M10-Extraneous Functionality",
      // <Array> the list of results found
      "results": [
         // A vulnerability instance
         {
            // <Object> Defines where the vulnerability ends up
            "sink": {
               // <String> the sink file
               "file": "",
               // <String> line where the vulnerability occurred
               "line":"33",
               // <String> column where the vulnerability occurred
               "column":"9",
               // <String> name of the object involved with the vulnerability
               "object":"passwordTextField"
            },
            // <String> indicates if the vulnerability is reoccurring 
            "state":"0",
            // <Object> Defines where the vulnerability started
            "source":{
               // <String> the source file
               "file":"",
               // <String> line where the vulnerability occurred
               "line":"33",
               // <String> column where the vulnerability occurred
               "column":"9",
               // <String> name of the object involved with the vulnerability
               "object":"passwordTextField"
            }
         }
      ]
   },
   // <Boolean> true if all instances are marked as false positive.
   "allFalsePositive": false
} 
```

## <a name="csv">CSV</a>
csv:
  file-name-format: "[PROJECT]-[TIME].csv"
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


The file system path and the file naming format are required.

**NOTE**: All of the file based outputs have a file-name-format attribute, which allows for dynamic naming substitution.  File name follows a substitution pattern with the following elements:

**[APP]** → Application

**[TEAM]** → Checkmarx Team ( \ is replaced with _ in the filename)

**[PROJECT]** → Checkmarx Project [PROJECT] → Checkmarx Project

**[NAMESPACE]** → Checkmarx Project [PROJECT] → Org/Group/Namespace for the repo (if available)

**[REPO]** → Checkmarx Project [PROJECT] → Repository name (if available)

**[BRANCH]** → Checkmarx Project [PROJECT] → Branch name (if available)

**[TIMESTAMP]** → Current timestamp (yyyyMMdd.HHmmss format)

## <a name="email">Email Notifications</a>
CxFlow works with SMTP and Sendgrid to notify users through e-mail. 

```yaml
cx-flow:
  contact: admin@yourdomain.com
  bug-tracker: Email
  mail:
    host: smtp.gmail.com
    port: 587
    username: xxx
    password: xxx
    notification: true # Required if using SendGrid
    sendgrid:
      api-token: your-sendgrid-token-here
```

`cx-flow.mail.notification` send two e-mail events: Scan Submitted and Scan Completed. The default is `false` (no e-mails are sent, even if all parameters are configured).

If `cx-flow.mail.notification` is set to `true`, by default the scan submitted and the scan completed (summary) e-mails are sent by default. You can disable each one of these events using `cx-flow.mail.enabled-notifications` sub-properties:

```yaml
cx-flow:
  contact: admin@yourdomain.com
  bug-tracker: Email
  mail:
    host: smtp.gmail.com
    port: 587
    username: xxx
    password: xxx
    notification: true # Required if using SendGrid
    enabled-notifications: # if `notification` is true, you can customize the events. The defaults are below.
      scan-submitted: false
      scan-summary: true
      scan-summary-with-empty-results: true
    sendgrid:
      api-token: your-sendgrid-token-here
```

When `cx-flow.mail.enabled-notifications.scan-summary-with-empty-results` is set to `false`, CxFlow checks for the total number of SAST **high and medium** results. If they are zero, the e-mail is not sent. 

If using SMTP, the following fields are required:

```yaml
cx-flow:
  bug-tracker: Email
  mail:
    host: smtp.gmail.com
    port: 587
    username: xxx
    password: xxx
    notification: true
```

If using Sendgrid, only the notification field and the API Token are required.

```yaml
cx-flow:
  mail:
    notification: true # Required if using SendGrid
    sendgrid:
      api-token: your-sendgrid-token-here
```

Sendgrid requires a trusted sender. This can be configured at `cx-flow.contact`. Otherwise, `donotreply@checkmarx.com` will be set as default.

```yaml
cx-flow:
  contact: emailthatsendgridtrusts@yourdomain.com
```

### E-mail Templates

CxFlow comes with two different e-mail templates, one for each event below:

- Scan submitted to Checkmarx
- Successfully completed scan, showing results. 

CxFlow uses [Thymeleaf](https://www.thymeleaf.org/) for templating. Templates can be found at `/src/main/resources/templates` folder.

If you wish to use your own templates, you can override the following parameters:

```yaml
cx-flow:
  mail:
    templates:
      scan-submitted: C:\your\folder\my-scan-submitted-notification.html
      scan-completed-successfully: C:\your\folder\custom-scan-report.html
```

The Scan completed successfully report receives the following objects:

- `repo_fullname`
- `repo`
- `link`
- `issues`
  - `issue.link`
  - `issue.severity`
  - `issue.vulnerability`
  - `issue.filename`

## <a name="pdf">PDF</a>
The PDF bug-tracker (defined as PDF) is useful, if you want to retrieve the latest scan results from Checkmarx per project, Team, or the entire instance. This is the original PDF report provided by Checkmarx. When using PDF with both CxSAST and CxSCA scanners enabled, two seprate reports will be generated, one for CxSAST report and one for CxSCA report.

CxSCA currently does not support `--batch` mode, but retrieving latest scan for a particular project (project mode) is still possible.


```
cxflow:
  #zip-exclude: .*\.PNG
  bug-tracker: PDF
  bug-tracker-impl:
    # - Azure
    # - Csv
    # - CxXml
    # - CxXml
    # - GitHub
    # - GitLab
    #  - GitLabDashboard
    # - GitLab
    # - Rally
    # - Json
     - PDF
    # -JIRA
    # - SonarQube
    # -GITHUBPULL
    # -BITBUCKETCOMMIT

pdf:
  file-name-format: checkmarx.PDF
  data-folder: C:\\tmp

```

The file system path as well as the file naming format is required.
From above example if SAST and SCA both enabled then file will get created by name ```SAST_checkmarx.pdf``` and  ```SCA_checkmarx.pdf```.

## <a name="none">NONE | WAIT</a>
If you want to trigger scans asynchronously, use **NONE**  
If you want to trigger scans, but wait for feedback | summary console output, use **WAIT | wait**  