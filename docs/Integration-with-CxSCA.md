* [Configuration](#configuration)
* [Bug-Trackers](#bug)
* [Filters](#filters)
* [Thresholds](#thresholds)
* [Policy Management](#policyManagement)
* [Configuration As Code](#configurationascode)
* [SCA Scans From Command Line](#commandline)
* [SCA ZIP Folder Scan](#zipFolderScan)
* [SCA Project Team Assignment](#scaProjectTeamAssignment)

## <a name="configuration">Configuration</a>
CxSCA scans can be triggered based on WebHooks using CxFlow. 
For instructions on registering CxFlow to WebHook, refer to [WebHook Registration](
https://github.com/checkmarx-ltd/cx-flow/wiki/WebHook-Registration).

### Adding the CxSCA Configuration
Select the vulnerability scanner by adding the following property to the CxFlow section. You can choose to trigger a CxSAST scan a CxSCA scan or both. The example below has been set to trigger both CxSCA and CxSAST.
```
enabled-vulnerability-scanners:
    - sca
    - sast
```
[[/Images/SCA1.png|YML example demonstrating enabled vulnerability scanners]]

In addition, add one of the CxSCA sections with the following properties:

#######1
```
sca:
  appUrl: [https://sca.scacheckmarx.com](https://sca.scacheckmarx.com)
  apiUrl: [https://api.scacheckmarx.com](https://api.scacheckmarx.com)
  accessControlUrl: [https://platform.checkmarx.net](https://platform.checkmarx.net)
  tenant: your tenant name
  username: userxx
  password: pasxx
```

[[/Images/SCA2.png|YML SCA example]]

#######2
```
sca:
  appUrl: https://eu.sca.checkmarx.net
  apiUrl: https://eu.api-sca.checkmarx.net
  accessControlUrl: https://eu.platform.checkmarx.net
  tenant: your-tenant
  username: userxx
  password: passxx
```

[[/Images/SCA2A.png|YML SCA example]]

## <a name="bug">Bug-Trackers</a>
SCA integration supports tickets management with the following bug trackers :
* Jira
* GitLab
* Azure
* Github
<br/>The tickets format is the same for each of the bug trackers.

### Opening Tickets in Jira
CxFlow can open Jira tickets according to the CxSCA scan results.
Currently, CxFlow opens a limitation per vulnerability severity found in a package.

**+++ The section below is still under development +++**

CxFlow opens a separate ticket for each vulnerability type found in a package of a specific project.

The ticket is structured as follows:
```
<Severity> Risk Vulnerability:  <**vulnerabilityId**> in <**package name and version**> @ <**Repo.branch**>
```
CxFlow SCA scans Jira's tickets based on both **vulnerabilityId & PackageId**.

This means that each vulnerabilityId with a different packageId has its own ticket.

The following correlation exists between pull requests processed via GitHub and Jira:

[[/Images/SCA3.png|GitHub pull request summary example]]

* **Red section**: There are two vulnerabilities, each with the same Id, but each one has a different package. Therefore two matching tickets are expected on the Jira side.
* **Blue section**:There are four vulnerabilities, each with a different Id, but each one has the same package. Therefore four matching tickets are expected on the Jira side. 

Jira ticket example:
[[/Images/SCA4.png|Jira ticket example]]

## <a name="filters">Filters</a>
SCA filtering has 2 sections: filter-severity & filter-score:
<br/>
[[/Images/SCA5.png|Example of filter-severity and filter-score]]
* Filter Severity: is a list type and can have multiple values [high, medium, low] regardless the order and not case sensitive. When applying this filter the SCA results vulnerabilities will be sanitized according to the severities defined filter.
  * Special cases:
    * Filter not defined: in that case the severity filter won’t be applied and the returned results will contain all scan severities.
    * Unrecognized filter defined: in that case this filter value will be ignored.
* Filter Score: is a double value starting from ‘0.0’.
  * Special cases:
    * Negative value will be ignored.
    * Filter not defined: in that case the score severity won’t be applied and the returned results will contain all scan scores.
* Combined filters: In case both severity & score filter were defined, the returned results will contain only vulnerabilities which apply both conditions.
  * Backwards Compatibility: in case none of the filters were defined, the returned results won’t be sanitized and all scan’s results will be returned

## <a name="thresholds">Thresholds</a>
Cx-Flow supports 2 kind of thresholds: 
* Severity  Thresholds (count per severity): 
* Score Threshold 
<br/>When performing a scan, if at least one of the thresholds is violated, Cx-Flow will fail the pull request and it will be marked as failed. 

### Feature SCM Supportability 
The thresholds feature is supported in the following source-code managements: 
* ADO 
* GitHub 
* BitBucket  

### Configuration changes required – via application.yml 
Thresholds are configured in the application.yml within the sca section:
<br/>
[[/Images/SCA6.png|Severity and Score Threshold examples]]

### On the supported SCM sections: (e.g. GitHub section) 
* Define the block-merge with true value 
* Define the error-merge with true value 

### Threshold configuration – General 
Cx-Flow uses the thresholds to ease its (no) tolerance of findings.  
* Severity – this threshold tells Cx-Flow what is the maximum number of findings (per severity) allowed, before failing the pull request. 
* Score – this threshold tells Cx-Flow what is the maximum acceptable score (of all findings), before failing the pull request. 
<br/>Note:  
* if only one threshold type is defined, the other one will be ignored 
* that the thresholds are on findings and not on issues. 
* if no threshold is defined, cx-flow will fail the pull request if there is any finding. 
* thresholds are checked after the execution of filters. 
<br/>An example for pull request failure: 
<br/>
[[/Images/SCA7.png|Example of Pull Request Failure]]

## <a name="policyManagement">Policy Management</a>
SCA supports with policy management control.
Each policy can be customized by defining number of custom rules and conditions in which, if getting violated, can break a build.
On the creation process or after it, a policy can be defined with 'Break build' flag or not.

[[/Images/SCA-policy-creation.png|Example of Policy creation dashboard]]

When performing a scan, if a defined policy is getting violated, Cx-Flow will fail the pull request and it will be marked as failed.
* Violated policy occurs when at least one rule condition is getting violated AND when policy 'Break Build' flag in on.
* In case of a CLI scan which violated a policy: Cx-Flow will fail with exit code 10.
* If current scan violated any active SCA thresholds and also violated a policy, policy break build has the top priority.


## <a name="configurationascode">Configuration As Code</a>
CxFlow supports configuration as code for CxSAST and CxSCA scans.
<br/>Available overrides for SCA properties:
* additionalProperties:
  * vulnerabilityScanners
* sca:
  * appUrl
  * apiUrl
  * accessControlUrl
  * tenant
  * thresholdsSeverity
  * thresholdsScore
  * filterSeverity
  * filterScore
  * team (needs to be set with none empty value)
  
<br/>Example for SCA config file content:
```
{
    "additionalProperties": {
        "cxFlow": {
			"vulnerabilityScanners": ["sca", "sast"]
        }
    },
	"sca": {
		"appUrl": "sampleAppUrl",
		"apiUrl": "sampleApiUrl",
		"accessControlUrl": "sampleAccessControlUrl",
		"tenant": "sampleTenant",
		"thresholdsSeverity": {
			"HIGH": 10,
			"MEDIUM": 6,
			"LOW": 3
		},
		"thresholdsScore": 8.5,
		"filterSeverity": ["high", "medium", "low"],
		"filterScore": 7.5,
		"team": "/CxServer/MyTeam/SubTeam"
	}
}
```

## <a name="commandline">SCA Scans From Command Line</a>
### CxFlow can initiate SCA scans with command line mode
<br/>There are 2 options to add SCA scan to the cli run:
* Add scanner argument to the cli command:  --scanner=sca 
* Add sca to the active vulnerabilities scanner in cxflow app.yml: 
[[/Images/SCA8.png|Example of enables-vulnerbaility-scanners]]

### CxFlow can open security tickets upon SCA scan results 
In order to open SCA security tickets, set the bug tracker in cxflow app.yml file or in add the argument with your bug tracker type (for example: --bug-tracker=Jira) 

 
### CxFlow can init git scan or upload zip folder to scan by sca:
* git scan:
  * --scan  --enabled-vulnerability-scanners=sca --app=MyApp --cx-project=test --repo-url=my-repo-url --repo-name=my-repo --branch=master --github  
* local zip scan:
  * --scan --app=MyApp --cx-team="my-team" --cx-project="project" --f="/Users/myProjects/project"
* get latest scan results:
  * --project --app=MyApp --cx-team="my-team" --cx-project="project" ([use 'project' command](https://github.com/checkmarx-ltd/cx-flow/blob/develop/src/main/java/com/checkmarx/flow/dto/ScanRequest.java))


## <a name="zipFolderScan">SCA ZIP folder scan</a>
In order to change the default CxFlow SCA scan behaviour and to perform a SCA ZIP scan, the next configuration property should be added underneath the sca configuration section:
```
enabledZipScan: true
```
Additional configuration in SCA zip scan flow - Include source files
* Default value set to false, In order to change the default CxFlow SCA zip scan behaviour, the next configuration property should be added underneath the sca configuration section:
```
includeSources: true
```

## <a name="scaProjectTeamAssignment">SCA project team assignment</a>
SCA project team assignment with CxFlow is performing on the SCA project creation stage. In order to set a project team, the next configuration property should be added underneath the sca configuration section:
```
team: /team
```
Or within a tree hierarchy:
```
team: /MainTeam/SubTeam
```
* In order to declare a team within a tree hierarchy, make sure to use the forward slash ('/').
* Declaring not existing team or team path will be resulted with 400 BAD REQUEST error.