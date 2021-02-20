* [What is CxFlow?](#whatiscxflow)
* [What are the main benefits of CxFLow](#benefits)
* [What are typical use cases?](#typical)
* [Is CxFlow supported by the product team?](#productteam)
* [How does Checkmarx support CxFlow implementations?](#implementations)
* [Does CxFlow have Checkmarx Licensing requirements?](#licensing)
* [What Integrations does CxFlow support?](#integrations)
* [What are the requirements for CxFlow?](#requirements)
* [When are Issues created from CxFlow into a Defect Tracking system?](#issues)
* [How can I demo CxFlow?](#demo)
* [Is CxFlow open source?](#opensource)
* [Has CxFlow been security tested and scanned for known vulnerabilities?](#securitytested)
* [If I have an issue / feature request item where can I report it?](#featurerequest)
* [How do I obtain the latest version of CxFlow?](#latest)
* [How do I get started with CxFlow?](#getstarted)
* [How does CxFlow work with multiple GitHub organizations or multiple JIRA projects?](#multiple)
* [Can a single yaml file be used to connect to multiple defect tracking systems?](#singleyaml)
* [How do you manage the project creation within CxSAST when running CxFlow in WebHook mode?](#manageprojectcreation)
* [If using global WebHooks, can specific projects be excluded from scanning?](#globalwebhooks)

## <a name="whatiscxflow">Q: What is CxFlow?</a>
CxFlow is a solution that enables creating projects automatically, scans orchestration and facilitates feedback channels in a closed loop mode.

## <a name="benefits">Q: What are the main benefits of CxFlow?</a>
CxFlow aids in incorporating Checkmarx scanning into DevOps/Release pipelines as early as possible. 

## <a name="typical">Q: What are typical use cases?</a>
Refer to [CxFlow Workflows](https://github.com/checkmarx-ltd/cx-flow/wiki/Workflows) for further information.

## <a name="productteam">Q: Is CxFlow supported by the product team?</a>
CxFlow is supported by the product team. Tickets can be opened via the regular workflow. SEG will decide to whom the ticket is routed, based on the production matrix and progress.

## <a name="implementations">Q: How does Checkmarx support CxFlow implementations?</a>
Checkmarx supports deploying and maintaining CxFlow by leveraging Checkmarx Professional Services for CxFlow iplementation, setup, and maintenance.

## <a name="licensing">Q: Does CxFlow have Checkmarx Licensing requirements?</a>
No. CxFlow is a tool developed interdependently from the Checkmarx product line and does not require any additions to existing customer licenses.

## <a name="integrations">Q: What Integrations does CxFlow support?</a>
The table below lists all the supported integrations, features and states the recommended versions.

| Software/Services | Features | CxFlow Version |
|-------------------|----------|----------------|
| **Jira** | Issue Tracking | >= 1.0.0 |
|      | Custom Bug Types |        |
|      | Custom Transitions in Workflows |        |
|      | Custom Fields                   |        |
| **GitHub** | WebHooks | >= 1.2.0 |
|        | Pull Requests Scanning and Decorating |       |
|        | Push Events |        |
|        | Native Issues Tracker |               |
| **GitLab** | WebHooks | >= 1.2.0 |
|        | Merge Requests Scanning and Decorating |       |
|        | Push Events |        |
|        | Native Issues Tracker |               |
| **Azure DevOps** | WebHooks | >= 1.3.0 |
|        | Merge Requests |       |
|        | Push Events |        |
|        | Pipelines |               |
|        | Work Items |               |
| **BitBucket** | WebHooks | >= 1.4.3 |
|               | Post Webhooks (plugin) | >= 3.14.18 |
|               | Merge Requests Scanning |         |
|               | Pull Events |          |
|               | Issue Tracker |         |
| **Rally** | Issue Tracking | >= 1.5.3 |

## <a name="requirements">Q: What are the requirements for CxFlow?</a>
Refer to [Pre-Requisites and Requirements](https://github.com/checkmarx-ltd/cx-flow/wiki/Prerequisites-and-Requirements) 

## <a name="issues">Q: When are Issues created from CxFlow into a Defect Tracking system?</a>
Issues are only created when a Push event into a protected branch occurs. When a Pull/Merge Request is created (and CxFlow scans the new code), the vulnerability information is displayed in the Pull/Merge Request comments and does NOT create issues in the defect tracking system.

## <a name="demo">Q: How can I demo CxFlow?</a>
Professional Services has created an easy-to-use CxFlow Demo Instance (sub-project of CxPsPowerHasks) script to assist with easy deployment and demonstration of CxFlow.

## <a name="opensource">Q: Is CxFlow open source?</a>
Yes. The code can be found [here](https://github.com/checkmarx-ts/cx-flow). Connect to preview 
<br>**Note**: CxFlow is a technical solution. Although it is open source and available for anyone to deploy, Checkmarx recommends and supports installations assisted by Checkmarx Professional Services

## <a name="securitytested">Q: Has CxFlow been security tested and scanned for known vulnerabilities?</a>
Yes. CxFlow has undergone multiple test runs at several stages with various testing tools.
For additional information, contact your Checkmarx Representative.

## <a name="featurerequest">Q: If I have an issue / feature request item where can I report it?</a>
CxFlow feature requests and issues should be reported like any other product feature request. CxFlow is available just like any other Checkmarx component.

## <a name="latest">Q: How do I obtain the latest version of CxFlow?</a>
You can find the current release on the [GitHub releases page](https://github.com/checkmarx-ts/cx-flow/releases).

## <a name="getstarted">Q: How do I get started with CxFlow?</a>
Please see the [Labs](https://github.com/checkmarx-ltd/cx-flow/wiki/Labs) page or the quick start on the home page.

## <a name="multiple">Q: How does CxFlow work with multiple GitHub organizations or multiple JIRA projects?</a>
Overrides can be used at the WebHook level and config as code can be added to the individual repos.
<br/>[CxFlow Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration)
<br/>[Config As Code](https://github.com/checkmarx-ltd/cx-flow/wiki/Config-As-Code)

## <a name="singleyaml">Q: Can a single yaml file be used to connect to multiple defect tracking systems?</a>
Yes - with the limitation of one Jira instance.

## <a name="manageprojectcreation">Q: How do you manage the project creation within CxSAST when running CxFlow in WebHook mode?</a>
Overrides can be used to assign the same name to multiple projects.  Alternatively, a groovy script can be used to help decide on project names and if it should be scanned. Refer also to [CxFlow Configuration](https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration)

## <a name="globalwebhooks">Q: If using global WebHooks, can specific projects be excluded from scanning?</a>
Yes, this can be performed with overrides & [Config As Code](https://github.com/checkmarx-ltd/cx-flow/wiki/Config-As-Code)