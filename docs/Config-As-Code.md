# Config as Code
The presence of a cx.config file in the root of the source repository is used to drive/override project/scanning configuration within CxFlow
### **Note: Currently implemented for GitHub, GitLab, Bitbucket Server, Bitbucket Cloud and Azure DevOps, for WebHook execution, and for local source scanning in batch mode.**

* [Current Overrides](#current)
* [Automated Code Profiling](#automatedcodeprofiling)
* [Automated Profile Configuration](#automatedprofileconfiguration)
* [CxProfile Config](#cxprofileconfig)

## <a name="current">Current Overrides Available</a>
Example Config As Code:
```
{
  "version": 1.0,
  "project": "XYZ-${repo}-${branch}",
  "team": "/a/b/c",
  "sast": {
    "preset": "",
    "engineConfiguration": "",
    "incremental": "true|false",
    "forceScan": "true",
    "fileExcludes": "",
    "folderExcludes": ""
  },
  "additionalProperties": {
    "cxFlow": {
      "application": "test app",
      "branches": ["develop", "main", "master"],
      "emails": ["xxxx@checkmarx.com"],
      "bugTracker": "JIRA|GitLab|GitHub|Azure",
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
            "type": "cx",
            "name": "xxx",
            "jira_field_name": "xxxx",
            "jira_field_type": "text",
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
        "status": ["Confirmed", "New"]
      }
    }
  }
}
```

## <a name="automatedcodeprofiling">Automated Code Profiling</a>

[[/Images/automatedWorkflow1.png|Automated code profiling workflow diagram]]
[[/Images/automatedWorkflow2.png|Automated code profiling swim lane diagram]]

* Registration of an Organizational WebHook across all Organizations pointed to CxFlow
    * Registration of the webhook will be for push events
* CxFlow will be configured to process events associated with branches deemed important/protected across the enterprise
    * master
    * main
    * develop
    * release*
    * etc (TBD)
* Upon receiving an event, CxFlow will:
    * Check local cache to see if the given repository has been previously profiled, regardless of branch
    * If yes, use the already established preset according to the cached value
        * Cache will leverage a Checkmarx custom field stored within the project
    * If no, CxFlow will profile the application by using the Contents GitHub API
        * Global exclusions will be applied to the profiling (paths/naming patterns)
        * File type, number of references, percentage of code base (reflected from post exclusions) will be mapped
        * CxFlow will iterate through a rule set that will attempt to match the fingerprint of the source code
            * Rules will be evaluated in order provided in the configuration file, once a match is found it will stop checking further
            * There will be a default / catchall rule for those not matching a finger print
        * The based on the fingerprint rule matching, an associated Checkmarx preset will be assigned
        * This information will be save to the local CxFlow cache.  Note:  If multiple instances of CxFlow are load balanced, the cache will only be available to the local instance doing the processing.  This design can be enhanced if required.
    * A scan request for the repository will be initiated with the scan preset that has been assigned in the previous step(s)
        * Scans will be attempted as incremental with the following rules:
            * A full scan was conducted within the last 7 days
            * A scan was conducted within the last 5 scans
        * Global file exclusion pattern(s) will be applied for every scan according to the CxFlow configuration
    * Optionally Result feedback can be configured 
        * CxFlow will generate the XML report
        * Results will be filtered
        * Results will be published according to the configured feedback channel(s)

## <a name="automatedprofileconfiguration">Automated Profile Configuration</a>
Default file is CxProfile.json unless provided as an override in the configuration yaml for CxFlow

The configuration is evaluated in order found within the file, and upon meeting the first match based on the criteria, the preset is selected.  
<br/>The default entry should be last, otherwise it will be selected as soon as it is reached.
Key | Description
---------|---------
name|Identifier for the profile.  If Default is the name, the rules are not evaluated and the preset is selected as this entry is reached.
preset|The Checkmarx scan preset that is associated with the profile
files|List of regular expressions that match file/path patterns.  All references in the list must be found for a match
weight|List of weighting criteria based on language percentages that must all match for the profile to be selected
weight→type|This is the associated language name based on the information from the repository (i.e. Java, C#, ASP, HTML, CSS)
weight→weight|The minimum percentage of code required for a match

All criteria must be met (file matches, weighting matches) to match and select the given profile

## <a name="cxprofileconfig">CxProfile Config</a>
```
[{
  "name:": "C# Web",
  "preset": "Checkmarx Default",
  "files": ["Web.config",".*web\\.xml"],
  "weight": [
    {"type":  "Java", "weight": 1},
    {"type":  "C#", "weight": 1},
    {"type":  "HTML", "weight": 1},
    {"type":  "Kotlin", "weight": 1}
  ]
},
  {
    "name:": "All",
    "preset": "All",
    "weight": [
      {"type":  "Java", "weight": 50},
      {"type":  "CSS", "weight": 88}
    ]
  },
  {
    "name:": "Java Web",
    "preset": "Checkmarx Express",
    "files": ["buildspec.yml"],
    "weight": [
      {"type":  "Java", "weight": 20},
      {"type":  "CSS", "weight": 1},
      {"type":  "HTML", "weight": 1}
    ]
  },
  {
    "name:": "Java Only",
    "preset": "Checkmarx Express",
    "weight": [
      {"type":  "Java", "weight": 50}
    ]
  },
  {
    "name:": "C#",
    "preset": "All",
    "weight": [
      {"type":  "C#", "weight": 20},
      {"type":  "ASP", "weight": 20}
    ]
  },
  {
    "name:": "DEFAULT",
    "preset": "Checkmarx Default"
  }]
```
