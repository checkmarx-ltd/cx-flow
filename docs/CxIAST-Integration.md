* [Overview](#Overview)
* [Configuration](#configuration)
* [Filters](#filters)
* [Thresholds](#thresholds)
* [Configure Bug Trackers](#configureBugTrackers)
* [Bug Trackers](#bugTrackers)
* [CLI Example](#cliExample)

## <a name="Overview">Overview</a>
CxIAST can be integrated within a CI/CD pipeline using CxFlow.
CxIAST scans start automatically when an application with an attached CxIAST agent is being tested.
To attach a CxIAST agent, refer to [CxIAST documentation](https://checkmarx.atlassian.net/wiki/spaces/CCD/pages/727417048/Configuring+the+AUT+Environment).

CxIAST scans can be stopped in two ways:
1. Manually stopped via the CxIAST manager.
2. By terminating the running application.

Once tests are completed, CxFlow is used to stop the running CxIAST scan.
CxFlow will then collect the results from CxIAST, analyze them, and open tasks/tickets in the configured [Bug Tracker](#bugTrackers).
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
  filter-severity:
    - HIGH
    - MEDIUM
    - LOW
    - INFO
  thresholds-severity:
    HIGH: 1
    MEDIUM: 3
    LOW: 10
    INFO: -1
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
To allow severities, add the relevant severity to the `filter-severity` section in the `iast` section in the configuration file.  
To ignore a severity, remove or comment that severity from the configuration file.

## <a name="thresholds">Thresholds</a>

CxFlow returns a status the CI pipeline when called.  
To control this, a threshold can be configured per vulnerability severity.  
Each severity threshold is determined by the allowed vulnerability count with that severity.  
To remove a threshold from a severity, set the relevant severity to `-1`. In the example below, the threshold has been removed
from **info**. Thresholds are configured in the `thresholds-severity` section in the `iast` section.  
When triggered in CLI mode, CxFlow returns status code `10`, if a threshold has been exceeded.  
When triggered in web mode, the `/iast/stop-scan-and-create-jira-issue/{scanTag}`
or `/iast/stop-scan-and-create-github-issue/{scanTag}` returns HTTP status 412.

## <a name="configureBugTrackers">Configure Bug Trackers</a>

For automatic create issue in Bug tracker need to configure CX-Flow for work with your bug tracker.

## <a name="bugTrackers">Bug Trackers</a>

At present, CxFlow only supports Jira, Github and Gitlab issue as a bug tracker when used with CxIAST.

### Jira

Refer to [Jira Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#jira) for
instructions on configuring CxFlow to work with Jira.

#### Required parameters for create jira ticket in application.yml file:

* `jira.url=https://xxxx.atlassian.net`
* `jira.username=email`
* `jira.token=token-xxx`
* `jira.project=project-key`

### Github

Refer to [Github issue Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#github) for
instructions on configuring CxFlow to work with Github issues.

#### Required parameters for create github ticket in CLI mode:

* `github.token=token-xxxx`

### Gitlab

Refer to [Gitlab issue Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#gitlab) for
instructions on configuring CxFlow to work with Gitlab issues.

#### Required parameters for create gitlab ticket in CLI mode:

* `gitlab.token=token-xxxx`

### Opening Tickets in Jira

CxFlow can open Jira tickets according to the CxIAST scan results.  
At present, CxFlow opens a separate Jira ticket for every new vulnerability of any severity discovered by CxIAST.

The ticket is structured as follows:

- The **title** field is set to `<CxIAST Vulnerability name> @ <Triggering API URL>`.
- The **priority** field is set based on the CxIAST vulnerability severity.
- The **assignee** field is set based on the `--assignee` argument that was passed to CxFlow, or based on the configured Jira
  username. Refer to [Jira Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Bug-Trackers-and-Feedback-Channels#jira)
  for additional information.
- The **description** field contains a link to the vulnerability in CxIAST Manager, scan tag, branch, repository name and severity
  of vulnerability.

#### Required parameters for create jira ticket in CLI mode:

* iast
* scan-tag=tag
* bug-tracker=jira

An example for a Jira ticket is available here:  
[[/Images/iast_jira_issue.png|Jira ticket example]]

### Opening Github Issues

CxFlow can open Github issues according to the CxIAST scan results. At present, CxFlow opens a separate github issue for every new
vulnerability of any severity discovered by CxIAST.

The ticket is structured as follows:

- The **title** field is set to `<CxIAST Vulnerability name> @ <Triggering API URL>`.
- The **assignee** field is set based on the `--assignee` argument that was passed to CxFlow.
- The **description** field contains a link to the vulnerability in CxIAST Manager, scan tag, branch, repository name and severity

#### Required parameters for create github ticket in CLI mode:

* iast
* scan-tag=tag
* bug-tracker=githubissue
* `repo-name=repository-name`
* `namespace=checkmarx-ltd`

An example for a Jira ticket is available here:  
[[/Images/iast_github_issue.png|Github issue example]]

### Opening Gitlab Issues

CxFlow can open Gitlab issues according to the CxIAST scan results. At present, CxFlow opens a separate gitlab issue for every new
vulnerability of any severity discovered by CxIAST.

The ticket is structured as follows:

- The **title** field is set to `<CxIAST Vulnerability name> @ <Triggering API URL>`.
- The **assignee** field is set based on the `--assignee` argument that was passed to CxFlow.
- The **description** field contains a link to the vulnerability in CxIAST Manager, scan tag, branch, repository name and severity

#### Required parameters for create github ticket in CLI mode:

* iast
* scan-tag=tag
* bug-tracker=gitlabissue
* `gitlab.token=token-xxxx`
* `project-id=xxxxxx`

An example for a Jira ticket is available here:  
[[/Images/iast_gitlab_issue.png|Gitlab issue example]]

## <a name="cliExample">CLI Example</a>

### Example opening Tickets in Jira

```
java -jar cx-flow.jar 
--spring.config.location=application.yml
--iast
--bug-tracker="jira"
--assignee="email@mail.com"
--scan-tag="scanTag"
--repo-name="checkmarx-ltd/cx-flow"
--branch="develop"
```

You can also pass the jira parameters that you want to set in application.yml for example:

```
java -jar cx-flow.jar 
--spring.config.location=application.yml
--iast
--bug-tracker="jira"
--assignee="email@mail.com"
--scan-tag="scanTag"
--repo-name="checkmarx-ltd/cx-flow"
--branch="develop"

--jira.username=email@mail.com
--jira.token=token-xxxx
--iast.username="user"
--iast.password="pass"
--jira.issue-type="Bug"
...
```

[[/Images/iast_jira_issue.png|Jira ticket example]]

### Example opening Tickets in Github issue

```
java -jar cx-flow.jar 
--spring.config.location=application.yml
--iast
--bug-tracker="githubissue"
--assignee="email@mail.com"
--scan-tag="scanTag"
--namespace="checkmarx-ltd"
--repo-name="cx-flow"
--branch="develop"

--github.token=token-xxxx
...
```

[[/Images/iast_github_issue.png|Github issue example]]

### Example opening Tickets in Gitlab issue

```
java -jar cx-flow.jar 
--spring.config.location=application.yml
--iast
--bug-tracker="githubissue"
--assignee="email@mail.com"
--scan-tag="cx-scan-20"
--namespace="checkmarx-ltd"
--repo-name="cx-flow"
--branch="develop"

--gitlab.token=token-xxxx

...
```

[[/Images/iast_gitlab_issue.png|Gitlab issue example]]

