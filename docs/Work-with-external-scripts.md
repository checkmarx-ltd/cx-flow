CxFlow is able to use inputs from external groovy scripts, according to runtime information and specific logic that is implemented in the script

for example, you can determine the Cx-project name that cxflow will trigger\create when initiating scan and use realtime information from the webhook payload, and edit it in the desired format in the script logic
for example you can add a static prefix to branch name and cxflow will use it when determine the cx-project name:


```groovy
String branch = request.getBranch();

String cxProject = "script-prefix-" + branch;
return cxProject;
```

* [Project script](#projectScript)
* [Team script](#teamScript)
* [Branch Script](#branchScript)
* [SAST scan comment script](#scanComment)
* [Use a Script to Filter Findings](#filterFindings)

### <a name="projectScript">Project script</a>
* CxFlow will use the string returned from the script execution to determine the cx-project name
* To enable this flow add the following property to cxflow configuration (you can use any file name): 

```yaml
checkmarx:
  project-script: ...\CheckProject.groovy
```

* Script input: [ScanRequest object](https://github.com/checkmarx-ltd/cx-flow/blob/develop/src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String



### <a name="teamScript">Team script</a>
* CxFlow will use the string returned from the script execution to determine the cx-team name
* To enable this flow add the following property to cxflow configuration (you can use any file name): 

```yaml
checkmarx:
  team-script: ...\CheckTeam.groovy
```

* Script input: [ScanRequest object](https://github.com/checkmarx-ltd/cx-flow/blob/develop/src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String



### <a name="branchScript">Branch Script</a>
* CxFlow will use the boolean value returned from the script execution to determine if scan should be 
* To enable this flow add the following property to cxflow configuration (you can use any file name): 

```yaml
checkmarx:
  branch-script: ...\CheckBranch.groovy
```

* Script input: 
  * [ScanRequest object](https://github.com/checkmarx-ltd/cx-flow/blob/develop/src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
  * List<String> branches

* Return value: String


### <a name="scanComment">SAST scan comment script</a>

* CxFlow will use the string returned from the script execution to determine the scan comment that is added to the scan initiated by cxflow
* To enable this flow add the following property to cxflow configuration (you can use any file name): 

```yaml
checkmarx:
  comment-script: ...\Checkcomment.groovy
```

* Script input: [ScanRequest object](https://github.com/checkmarx-ltd/cx-flow/blob/develop/src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String



### <a name="filterFindings">Use a Script to Filter Findings</a>

To filter findings, CxFlow uses configuration that looks like the following:

```yaml
cx-flow:
  filter-severity:
    - Critical
    - High
  filter-category:
  filter-cwe:
  filter-status:
    - New
    - Confirmed
```

Now it’s possible to provide a Groovy script in the configuration. The script returns a boolean value. The value indicates if a specific finding passes the filter. For example:

```yaml
cx-flow:
  filter-script: "finding.severity == 'HIGH' || (finding.severity == 'MEDIUM' && finding.status == 'URGENT')"
```
The new functionality was introduced to support customer’s request. it was requested more sophisticated filtering logic. For example: _“return findings of High severity; in addition, return findings of Medium severity with the ‘Urgent’ status”_. An example of a corresponding Groovy expression is specified above.

CxFlow passes a finding parameter to the script. The ```finding``` object represents each SAST finding that is being checked against the filter. The ```finding``` object currently has the following properties:

* category
* cwe
* severity
* status
* state

The ```finding``` object can be easily extended to include other properties. Note that the script should compare ```finding``` properties to uppercase string values.
An exception is thrown in the following cases:

1. Both “simple” filters and a scripted filter are specified in the config.
2. Filtering script doesn’t return a boolean value.
3. Filtering script has invalid syntax.
4. A runtime error happens during script execution (comparing to a non-existent property etc.)