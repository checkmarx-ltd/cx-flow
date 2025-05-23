CxFlow is able to use inputs from external groovy scripts, according to runtime information and specific logic that is implemented in the script

For example, you can determine the Checkmarx project name that CxFlow will trigger and/or create when initiating scan and use realtime information from the webhook payload, and edit it in the desired format in the script logic.

Here is an example of how you can use a static prefix, branch name and commit hash to determine Checkmarx project name:

```groovy
String cxProject = "script-prefix-" + request.getBranch() + "-" + request.getHash() 
return cxProject
```
The resulting project name will look like this: `script-prefix-master-fa907029c049b781f961e452a375d606402102a6`.
For more information about the `getHash()` property, see the `hash` field documentation in [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java).

* [Project script](#projectscript)
* [Team script](#teamscript)
* [Branch Script](#branchscript)
* [SAST scan comment script](#scancomment)
* [Use a Script to Filter Findings](#filterfindings)
* [JIRA project key script](#jiraprojectkeyscript)
* * [Branch Name script](#branchnamescript)
* * [Default Branch Name script](#defaultbranchnamescript)

### <a name="projectscript">Project script</a>
* CxFlow will use the string returned from the script execution to determine the Checkmarx project name
* To enable this flow add the following property to CxFlow configuration (you can use any file name): 

```yaml
checkmarx:
  project-script: ...\CheckProject.groovy
```

* Script input: [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String



### <a name="teamscript">Team script</a>
* CxFlow will use the string returned from the script execution to determine the cx-team name
* To enable this flow add the following property to cxflow configuration (you can use any file name): 

```yaml
checkmarx:
  team-script: ...\CheckTeam.groovy
```

* Script input: [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String



### <a name="branchscript">Branch Script</a>
* CxFlow will use the boolean value returned from the script execution to determine if scan should be 
* To enable this flow add the following property to cxflow configuration (you can use any file name): 

```yaml
cx-flow:
  branch-script: ...\CheckBranch.groovy
```

* Script input: 
  * [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
  * List<String> branches

* Return value: String


### <a name="branchnamescript">Branch Name Script</a>
* CxFlow will change the name of branch according to provided groovy script.
* To enable this flow add the following property to cxflow configuration (you can use any file name):

```yaml
cx-flow:
  branchScript: ...\branch.groovy
```

* Script input:
  * [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
  * List<String> branches

* Return value: String

### <a name="defaultbranchnamescript">Default branch name Script</a>
* CxFlow will change the name of default branch according to provided groovy script.
* To enable this flow add the following property to cxflow configuration (you can use any file name):

```yaml
cx-flow:
  defaultBranchScript: ...\default.groovy
```

* Script input:
  * [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
  * List<String> branches

* Return value: String

### <a name="scancomment">SAST scan comment script</a>

* CxFlow will use the string returned from the script execution to determine the scan comment that is added to the scan initiated by CxFlow.
* CxFlow searches comment-script for local files only.
* To enable this flow add the following property to CxFlow configuration (you can use any file name): 

```yaml
cx-flow:
  comment-script: ...\ScanComment.groovy
```

* Script input: [ScanRequest object](https://raw.githubusercontent.com/checkmarx-ltd/cx-flow/develop/src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String
* Script example: [ScanComment.groovy](https://raw.githubusercontent.com/checkmarx-ltd/cx-flow/develop/src/main/resources/samples/ScanComment.groovy)

### <a name="filterfindings">Use a Script to Filter Findings</a>

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

### <a name="jiraprojectkeyscript">JIRA project key script</a>

* CxFlow will use the string returned from the script execution to determine the JIRA project key which is added to the bug tracker and used by CxFlow to issue tickets in it
* To enable this flow add the following property to CxFlow configuration (you can use any file name): 

```yaml
jira:
  project-key-script: ...\CheckProjectKey.groovy
```

* Script input: [ScanRequest object](../src/main/java/com/checkmarx/flow/dto/ScanRequest.java)
* Return value: String
* Script example: [JiraProjectKey.groovy](../src/main/resources/samples/JiraProjectKey.groovy)
