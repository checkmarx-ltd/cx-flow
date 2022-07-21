* [Configuration](#configuration)
* [Bug-Trackers](#bug)
* [Filters](#filters)
* [Thresholds](#thresholds)
* [Policy Management](#policyManagement)
* [Configuration As Code](#configurationascode)
* [SCA Scans From Command Line](#commandline)
* [SCA ZIP Folder Scan](#zipFolderScan)
* [SCA Project Team Assignment](#scaProjectTeamAssignment)
* [SCA Scan Timeout](#scaScanTimeOut)
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

In addition, add a CxSCA section with the following properties:
```
sca:
  appUrl: https://sca.checkmarx.net
  apiUrl: https://api-sca.checkmarx.net
  accessControlUrl: https://platform.checkmarx.net
  tenant: your-tenant
  username: username
  password: xxxxx
  team: "/CxServer/MyTeam/SubTeam"
  include-sources: true
  exclude-files: "**/*.xml"
  manifests-include-pattern: "!**/*.xml, **/*.yml"
  fingerprints-include-pattern: "**/*.yml"
  preserve-xml: true
  filter-severity:
    - High
  filter-policy-violation: true
  //If User want to check for Direct Dependency specifically below tag can be used and default value is All. 
  filter-dependency-type: Direct
  //Based on threshold severity it will break build even for Direct Dependency.
  thresholds-Severity:
    HIGH: 1
    MEDIUM: 150
    LOW: 1
```

To use an European tenant:
=======
In addition, add one of the CxSCA sections with the following properties:

```
sca:
  appUrl: https://eu.sca.checkmarx.net
  apiUrl: https://eu.api-sca.checkmarx.net
  accessControlUrl: https://eu.platform.checkmarx.net
  tenant: your-tenant
  username: username
  password: xxxxx
  team: "/CxServer/MyTeam/SubTeam"
  include-sources: true 
  exclude-files: "**/*.xml"
  manifests-include-pattern: "!**/*.xml, **/*.yml"
  fingerprints-include-pattern: "**/*.yml"
  preserve-xml: true
  filter-severity:
    - High
  filter-policy-violation: true
```

## <a name="bug">Bug-Trackers</a>
SCA integration supports tickets management with the following bug trackers:
* Jira
* GitLab
* Azure
* GitHub
* CxXML

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

**Following Properties of SCA Results can now be mapped to jira issues:**
```
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

## <a name="filters">Filters</a>
SCA filtering has 2 sections: filter-severity, filter-score & filter-policy-violation:
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
* Policy Violation filters: SCA vulnerabilities will be picked by Cxflow on violates any policy.Add a filter-policy-violation to the sca block that only validates vulnerabilities that breach Policy Violations.
  * Backwards Compatibility: In case filters were not defined, the returned results won’t be sanitized and all scan’s results will be returned
```
  sca:
    filter-policy-violation: true
```


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
<br/> When a configuration as code property is set, it will only override the corresponded global configuration property. In case of a list property (e.g. 'filterSeverity'), the whole global corresponded list will be overridden.

## <a name="commandline">SCA Scans From Command Line</a>
### CxFlow can initiate SCA scans with command line mode
<br/>There are 2 options to add SCA scan to the cli run:
* Add scanner argument to the cli command:  --scanner=sca 
* Add sca to the active vulnerabilities scanner in CxFlow app.yml: 
[[/Images/SCA8.png|Example of enables-vulnerability-scanners]]

### CxFlow can open security tickets upon SCA scan results 
In order to open SCA security tickets, set the bug tracker in CxFlow app.yml file or in add the argument with your bug tracker type (for example: --bug-tracker=Jira) 
 
### CxFlow can init git scan or upload zip folder to scan by sca:
* git scan:
  * --scan  --enabled-vulnerability-scanners=sca --app=MyApp --cx-project=test --repo-url=my-repo-url --repo-name=my-repo --branch=main --github  
* local zip scan:
  * --scan --app=MyApp --cx-team="my-team" --cx-project="project" --f="/Users/myProjects/project"
* get latest scan results:
  * --project --app=MyApp --cx-team="my-team" --cx-project="project"
*EnabledZipScan:
  * CxFlow will locally clone the repository and zip it and send the zip file for scanning.
```
enabled-zip-scan: true
```
When `enabled-zip-scan` is set to `true` then cx-flow will first clone the repository locally, and then it will zip the repository and send it for scanning.

**Note:** When `enabled-zip-scan` is set to `true` when working with GitLab pipeline then `--gitlab` has to be passed in the CLI command, and in case of GitHub actions `--github` has to be passed in the CLI command.


Additional configuration in SCA zip scan flow - Include source files

* Default value set to false, In order to change the default CxFlow SCA zip scan behavior, the next configuration property should be added underneath the sca configuration section:

```
include-sources: true
```

* When includeSources is set to true cx-flow will consider all the files for scanning. If there is need to exclude files the **exclude-files** parameter is used. This parameter expects a regular expression for the files to be excluded. e.g ``` exclude-files: "**/*.xml"``` will exclude all the .xml files present in the source folder.


* When includeSources is set to false cx-flow will consider the manifest-files and calculate fingerprint for it. If there is a need to exclude files then in this case the **manifests-include-pattern** and the **fingerprints-include-pattern** is used. These parameters also requires regular expression. e.g ``` manifests-include-pattern: **/*.xml, !**/*.yml``` will include the all the xml file and exclude all the yml files.
 
**Note** The files to be excluded must begin with !. (Only applicable for manifests-include-pattern and fingerprints-include-pattern properties).

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

## <a name="scaScanTimeOut">SCA Scan Timeout</a>
In order  to set Scan TimeOut for SCA, the configuration property should be added underneath the sca configuration section:
```
 scan-timeout: 120