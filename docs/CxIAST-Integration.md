* [Suggested Flow](#suggestedFlow)
* [Configuration](#configuration)
* [Filters](#filters)
* [Thresholds](#thresholds)
* [Bug Trackers](#bugTrackers)
* [CLI Example](#cliExample)

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
  # ssl-certificate-file-path: "/tmp/iast/certificate.cer"
  manager-port: 8380
  username: xxxx
  password: xxxx
  update-token-seconds: 250  # CxAccessControl token timeout
```
**Note:** To allow connection to CxIAST server using a self-signed certificate, uncomment `ssl-certificate-file-path`.  
A certificate file for your CxIAST server can be found in an extracted CxIAST agent folder.  
Alternatively, to get the certificate from a running CxIAST instance, you could use openssl.  
On linux, for example:
```
# If you access CxIAST UI at https://my-iast.com:443
openssl s_client -showcerts -servername my-iast.com -connect my-iast.com:443 < /dev/null > iast.cer
```
 
## <a name="filters">Filters</a>
CxFlow may filter CxIAST vulnerabilities according to the vulnerability severity before creating bug tracker tickets.  
To allow severities, add the relevant severity to the `iast` section in the configuration file:
```
iast:
  filter-severity:
    - HIGH
    - MEDIUM
    - LOW
    - INFO    
```
To ignore a severity, remove or comment that severity from the configuration file.

## <a name="thresholds">Thresholds</a>
CxFlow returns a status the CI pipeline when called.  
To control this, a threshold can be configured per vulnerability severity.  
Each severity threshold is determined by the allowed vulnerability count with that severity.  
To remove a threshold from a severity, set the relevant severity to `-1`. In the example below, the threshold has been removed from **info**. 
Thresholds are configured in the `iast` section:
```
iast:
  thresholds-severity:
    HIGH: 1
    MEDIUM: 3
    LOW: 10
    INFO: -1
```
When triggered in CLI mode, CxFlow returns status code `10`, if a threshold has been exceeded.  
When triggered in web mode, the `/iast/stop-scan-and-create-jira-issue/{scanTag}` returns HTTP status 412.

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
- The **description** field contains a link to the vulnerability in CxIAST Manager, scan tag, branch and repository name.

An example for a Jira ticket is available here:  
[[/Images/IAST2.png|Jira ticket example]]

## <a name="cliExample">CLI Example</a>

```
java -jar cx-flow-1.6.18.jar 
--spring.config.location=application.yml
--iast
--bug-tracker="jira"
--assignee="email@mail.com"
--scan-tag="cx-scan-20"
--repo-name="checkmarx-ltd/cx-flow"
--branch="develop"
```

You can also pass the parameters that you want to set in application.yml
for example:

```
java -jar cx-flow-1.6.18.jar 
--spring.config.location=application.yml
--iast
--bug-tracker="jira"
--assignee="email@mail.com"
--scan-tag="cx-scan-20"
--repo-name="checkmarx-ltd/cx-flow"
--branch="develop"

--jira.username=email@mail.com
--jira.token=token-xxxx
--iast.username="user"
--iast.password="pass"
--jira.issue-type="Bug"
...
```

