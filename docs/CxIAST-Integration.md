* [Suggested Flow](#suggestedFlow)
* [Configuration](#configuration)
* [Filters](#filters)
* [Thresholds](#thresholds)
* [Bug Trackers](#bugTrackers)

## <a name="suggestedFlow">Suggested Flow</a>
CxIAST can be integrated within a CI/CD pipeline using CxFlow.  
A CxIAST scan starts automatically when an application with an attached CxIAST agent is being tested.  
CxIAST scans can be stopped in two ways:
1. Manually stopped via the CxIAST manager.
2. By terminating the running application.

After scans have been completed, CxFlow is used to stop the running CxIAST scan.  
CxFlow collects the results from CxIAST, analyzes them, and opens tasks/tickets in the configured bug tracker.  
To make sure the correct scan is stopped by CxFlow, CxIAST scan tags are used:  
[[/Images/IAST1.png|IAST flow example]]

## <a name="configuration">Configuration</a>
The configuration must be added to **application.yml** or passed as command line arguments.  
To view an example, refer to [Main Properties](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration#main).

### Adding the CxIAST Configuration
To allow communication between CxFlow and CxIAST, the following `iast` section must be added to **application.yml**:
```
iast:
  url: http://xxxxx.iast.net
  manager-port: 8380
  username: xxxx
  password: xxxx
  update-token-seconds: 150  # CxAccessControl token timeout
```
 
## <a name="filters">Filters - to be implemented</a>
CxFlow may filter CxIAST vulnerabilities according to the vulnerability severity before creating bug tracker tickets.  
To allow severities, add the relevant severity to the `iast` section in the configuration file:
```
iast:
  filter-severity:
    - high
    - medium
    - low
    - info    
```
To ignore a severity, remove that severity from the configuration file.

## <a name="thresholds">Thresholds - to be implemented</a>
CxFlow returns a status the the CI pipeline when called.  
To control this, a threshold can be configured per vulnerability severity.  
Each severity threshold is determined by the allowed vulnerability count with that severity.  
To remove a threshold from a severity, set the relevant severity to `-1`. In the example below, the trhreshold has been removed from **info**. 
Thresholds are configured in the `iast` section:
```
iast:
  thresholds-severity:
    high: 1
    medium: 3
    low: 10
    info: -1
```
When triggered in CLI mode, CxFlow returns status code `10`, if a threshold has been exceeded.  
When triggered in web mode, the `/iast/stop-scan-and-create-jira-issue/{scanTag}` returns `Threshold exceeded: <threshold information>` in the HTTP body.

## <a name="bugTrackers">Bug Trackers</a>
At present, CxFlow only supports Jira as a bug tracker when used with CxIAST.  
Refer to [Jira Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#jira) for instructions on configuring CxFlow to work with Jira.

### Opening Tickets in Jira
CxFlow can open Jira tickets according to the CxIAST scan results.  
At present, CxFlow opens a separate Jira ticket for every new vulnerability of any severity discovered by CxIAST.

The ticket is structured as follows:
- The **title** field is set to `<CxIAST Vulnerability name>:  <Triggering API URL>`.
- The **priority** field is set based on the CxIAST vulnerability severity.
- The **assignee** field is set based on the `--assignee` argument that was passed to CxFlow, or based on the configured Jira username. Refer to [Jira Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#jira) for additional information.
- The **description** field contains a link to the vulnerability in CxIAST Manager.
- The **labels** field contains the CxIAST scan tag.

An example for a Jira ticket is available here:  
[[/Images/IAST2.png|Jira ticket example]]