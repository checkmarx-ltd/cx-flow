* [Understanding the Data](#data)
* [Jira](#jira)
  * [Priorities](#priorities)
  * [Transitions](#transitions)
  * [Fields](#fields)
  * [Assigning tickets to a user](#assigningTickets)
* [Custom Bug trackers](#custom)
* [Azure DevOps Work Items](#azure)
* [GitLab Issues](#gitlab)
* [GitLab Security Dashbaord](#dashboard)
* [GitHub Issues](#github)
* [Rally Issues](#rally)
* [Service Now](#service)
* [CxXML](#cxxml)
* [Json](#json)
* [CSV](#csv)
* [NONE|WAIT](#none)

##  <a name="data">Understanding the Data</a>
Checkmarx results are processed according to the following [scheme](https://checkmarx.atlassian.net/wiki/download/attachments/1276543114/cx.xsd?version=1&modificationDate=1557257271078&cacheVersion=1&api=v2).

The breakdown of the XML includes the following:

**Query→ Result→ Path**

Issues are filtered based on the criteria found in the main configuration of CxFlow along with any particular overrides (severity, category, cwe, status).  From the XML structure, the **Source** identifier is the main tracking element.  The **Vulnerability+File** path is the key, and as additional line references are found for the same key, it is appended to the same issue reference.  See the development section for details on the ScanResults/Issue object structure.

The best fix location would be a more appropriate value to track, which is currently unavailable.

## <a name="jira">Jira</a>
Jira has the most complex configuration use case as it supports a variety of custom fields, custom workflows and custom transitions.

```
jira:
   url: https://xxxx.atlassian.net
   username: xxxx
   token: xxxx
   project: SS
   issue-type: Application Security Bug
   priorities:
      High: High
      Medium: Medium
      Low: Low
      Informational: Lowest
   pen-transition: In Review
   close-transition: Done
   open-status:
      - To Do
      - In Progress
      - In Review
   closed-status:
      - Done
   fields:
#    - type: cx #[ cx | static | result ]
#      name: Platform # cx custom field name | cwe, category, severity, application, *project*, repo-name, branch, repo-url, namespace, recommendations, loc, site, issueLink, filename, language
#      jira-field-name: Application
#      jira-field-type: label #[ security | text | label | single-select | multi-select ]
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
```

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

### <a name="fields"> Fields</a>
* **type**
  * **static**: Used for static values (specifically requires a jira-default-value to be provided)
  * **cx**: Used to map specific Checkmarx Custom Field values
  * **result**: Used to map known values from Checkmarx results or repository/scan request details.  Refer to the Result values below.
* **name**: If cx reflects the type, it is the name of the custom field within Checkmarx
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

* **jira-field-name** - Custom field name in Jira (readable name, not Custom field name)
* **jira-field-type** - Type of custom field in Jira:
  * _label_ (if using static or cx values, the CSV format is used and broken into multiple labels)
  * _text_ (applies to many custom field types: url, text box, text, etc
  * _multi-select_ (the CSV format is used and broken into multiple select values)
  * _single-select_
  * _security_ (used for issue security levels)
* **jira-default-value** - Static value if no value can be determined for the respective field (Optional)

### <a name="assigningTickets">Assigning tickets to a user</a>
Jira tickets can be assigned to a user when they are created. This can be achieved in the following way.

* As a webhook url parameter - The url parameter 'assignee' can be appended to the url in the webhook configuration and a user's email address to whom the tickets should be assigned, is provided as the value of the parameter.

  E.g - http​&#65279;://a7674e6a169f.ngrok.io?assignee=someUsersEmail@&#65279;xyz.com

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
* CxXML - Only available for SAST 8.x|9.x
* Csv
* Json
* GitHub
* GitLab
* Azure
* GitLabDashboard

## <a name="azure">Azure DevOps WorkItems</a>
Azure DevOps workitems only supports an issue body/description.  Custom/template field values are not available at present.  The available issue-type values are built/tested around issue and impediment (Scrum)
[[/Images/bug1.png|Screenshot of Azure Devop work item]]

## <a name="gitlab">GitLab Issues</a>
GitLab Issues leverages the same configuration as specified for WebHook listeners → API token (**token**) and valid urls are required

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

## <a name="dashboard">GitLab Security Dashboard</a>

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
```
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
The XML bug-tracker (defined as CxXml) is useful, if you want to retrieve the latest scan results per project (batch mode) from Checkmarx per project, Team, or the entire instance.  This is the original XML report provided by Checkmarx.  

**Note**: The Checkmarx config block must specify `preserve-xml` as `true` for this feedback type.  *Only available for SAST 8.x|9.x*
```
checkmarx:
   ...
   ...
     preserve-xml: true
cx-xml:
   file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
   data-folder: "C:\\tmp"
```

The file system path as well as the file naming format is required.

## <a name="json">Json</a>
The JSON bug-tracker (defined as Json), is useful if you would like to retrieve all of the latest scan results per project (batch mode) from Checkmarx per project, Team, or entire instance. The CxFlow JSON configuration block requires you to specify the path where reports are created and file name format to when creating reports, example:

```
json:
   file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
   data-folder: "C:\\tmp
```

The report contents will be a JSON representation of the ScanResults object, which includes issues based on the filtering specified in the main config block (cx-flow). You can determine how results Checkmarx found by looking at the "scanSummary" section, and you can determine how many results CxFlow reported after applying filters by looking at the "flow-summary" section. Each vulnerability found will appear in the "xissues" list.

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
```
checkmarx:
  ...
  ...
    preserve-xml: true
 
cx-xml:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
  data-folder: "C:\\tmp"
```
The file system path and the file naming format are required.

**NOTE**: All of the file based outputs have a file-name-format attribute, which allows for dynamic naming substitution.  File name follows a substitution pattern with the following elements:

**[APP]** → Application

**[TEAM]** → Checkmarx Team ( \ is replaced with _ in the filename)

**[PROJECT]** → Checkmarx Project [PROJECT] → Checkmarx Project

**[NAMESPACE]** → Checkmarx Project [PROJECT] → Org/Group/Namespace for the repo (if available)

**[REPO]** → Checkmarx Project [PROJECT] → Repository name (if available)

**[BRANCH]** → Checkmarx Project [PROJECT] → Branch name (if available)

**[TIMESTAMP]** → Current timestamp (yyyyMMdd.HHmmss format)


## <a name="none">NONE | WAIT</a>
If you want to trigger scans asynchronously, use **NONE**  
If you want to trigger scans, but wait for feedback | summary console output, use **WAIT | wait**  