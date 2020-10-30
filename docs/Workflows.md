[[/Images/workflow1.png|Detailed webhook diagram]]
[[/Images/workflow2.png|Simple webhook example]]

## WebHook WebService
Running CxFlow as a web service will enable registering WebHook Push and Pull/Merge events in GitHub, GitLab, Bitbucket (Cloud/Server) to drive scanning automation.

[[/Images/workflow3.png|Diagram of various SCM repositories flowing through CxFlow automation framework to bug tracking software]]
[[/Images/workflow4.png|Diagram of SCM events that trigger automation framework]]

### Workflow detailed steps:
* Webhook is registered at the namespace level (aka group, organization) or at the individual project/repository level within GitLab, GitHub, or Bitbucket using a shared key/token and pointing to the Automation Service
* Developer commits code (PUSH Request)
* WebHook submits a request to the Service along with commit details
* All developers identified within the commit(s) of a PUSH request are notified via email that the scan has been submitted (note: email can be disabled)
* Service determines if the branch is applicable to the service (see Branch details below)
* Service creates a new team (if multi-tenant mode is on) and a project for the particular organization/repository within Checkmarx.  If a project already exists with the same name, the existing project is used
* Project is set to use specific scanning rules (Preset)
* Repository details are updated in the project within Checkmarx
* Scan request is submitted for the project in Checkmarx
* Service monitors the state of the scan, and waits until it is finished
* Once scan is finished, a report is generated and retrieved by the Service
* Findings are filtered based on defined criteria (see Filter details below)
* Service sends an email notification to all committers that scan is complete. The email includes direct links to issues based on Filtering
* Service publishes findings to defined Bug Tracking tool
   * Issues are collapsed (multiple issues of the same type in the same file are updated within 1 ticket) - See Bug Tracking details below.
   * Tickets are closed if the issue is remediated on next iteration of scan.
   * Tickets are re-opened in the event an issue is reintroduced.
   * All references within a ticket must be addressed before the Ticket is closed.


## Command Line
### Parse
The Parse mode uses the Checkmarx Scan XML as input to drive the automation:
[[/Images/workflow5.png|Detailed diagram of CxFlow CLI implmentation]]
[[/Images/workflow6.png]]
[[/Images/workflow7.png]]

* The existing CI/CD pipeline executes the flow in Jenkins, Bamboo, GitLab etc. It pulls code, submits code to Checkmarx (via plugin, CLI, SDK) and generates XML results files.
* Automation executable ingests the XML.
* Findings are filtered based on defined criteria. Refer to the Filter details below.
* Automation executable publishes findings to the defined defect management tool.
   * Issues are collapsed (multiple issues of the same type in the same file are updated within one ticket). Refer to the Bug Tracking details below.
   * Tickets are closed, if the issue is remediated during the next iteration of the scan.
   * Tickets are re-opened, if an issue is reintroduced.
   * All references within a ticket must be addressed before the ticket is closed.

### Source/Zip
[[/Images/workflow8.png]]

* The existing CI/CD pipeline is executed
* CxFlow Service is executed with applicable parameters including path to the source
* CxFlow Service creates a new team (if multi-tenant mode is enabled) and a project for the particular organization/repository within Checkmarx.  If a project already exists with the same name, the existing project is used.
* The project is set to use specific scanning rules (Preset).
* Repository details are updated in the project within Checkmarx. The source folder is zipped and submitted to Checkmarx.
* The scan request is submitted for the project in Checkmarx.
* CxFlow monitors the state of the scan, and waits until it is completed.
* Once a scan is finished, a report is generated and retrieved by CxFlow Service
* Findings are filtered based on defined criteria (see Filter details below)
* CxFlow Service sends an email notification to all committers that the scan is complete in the event that the email includes direct links to issues based on Filtering.
* CxFlow Service publishes findings to the defined Bug Tracking tool.
   * Issues are collapsed (multiple issues of the same type in the same file are updated within 1 ticket). See Bug Tracking details below.
   * Tickets are closed if the issue is remediated during the next iteration of the scan.
   * Tickets are re-opened in the event that an issue is reintroduced.
   * All references within a ticket must be addressed before the ticket is closed.

### Batch
The batch mode execution is used to retrieve results of the latest scan(s) at Checkmarx and publish feedback/defects according to the bug-tracker specified. It is not used to drive scanning (or parse provided XML), but instead is 

#### By Instance
* CxFlow Service is executed, indicating that the Batch mode is used.
* CxFlow retrieves a list of all projects on the instance of Checkmarx.
* Generating a report is initiated for each project for the latest scan (if available).
* CxFlow ResultService threads out processing each project's results.
   * Each bug tracker is implementation specific
   * If file based (XML, CSV, JSON), a new file will be created per project.  The file name can be composed of Team, Project, and static content among other things
   * If Jira is used, the global Jira configuration is used for each project and the ability to drive the project, issuetype, custom field mappings and assignee can all be overridden by custom fields within Checkmarx.

#### By Team
Same as defined above for the Instance, except that it retrieves projects/scan results for a given team that is defined in the command line during execution.

#### By Project
* Generating a report is initiated for the project of the latest scan.
* CxFlow ResultService projects results as follows:
   * Each bug tracker is a specific implementation.
   * If file based (XML, CSV, JSON), a new file is created per project.  The file name can be composed of Team, Project, and static content among other things.
   * If Jira is used, the global Jira configuration is used for each project and the ability to drive the project, issuetype, custom field mappings, assignee can all be overridden by custom fields within Checkmarx.

