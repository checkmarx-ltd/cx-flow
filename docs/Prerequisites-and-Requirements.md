> **Notice :** This is to inform you that the cxFlow will not be compatible to the old Java versions (version 8 and 11) beginning April 1 2024.
The customers need to upgrade the Java version in the CxFlow server to a version greater than or equal to Java 17 (less than 20)
After the upgrade, the customers need to use the CxFlow Java11.jar file 


The following applications are required:

| Software         | Version                                            | Notes                                                                                                                                                                                                                                                                                          |
|------------------|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Java Runtime** | 8, 11  ,17,18                                      | Specific builds exist for both Java 8 and 11, or higher version. CxFlow can run anywhere with Java 1.8/11+ Runtime available. If user is using higher versions of JAVA they should append **Djava.locale.providers=COMPAT,CLDR** in JVM arguments in order to avoid **DateTimeParseException** |
| **CxSAST**       | 8.8, 8.9, 9.x                                      | CxFlow uses Checkmarx's REST APIs, available for version 8.8 and higher                                                                                                                                                                                                                        |
| **Jira**         | 6.4, 7.x, 8.x, 9.x                                 | Jira Cloud and Software have been tested                                                                                                                                                                                                                                                       |
| **GitHub**       | Cloud and Enterprise supported versions            | Both WebHook and Issue integration                                                                                                                                                                                                                                                             |
| **GitLab**       | Cloud, Community and Enterprise supported versions | Both WebHook and Issue integration                                                                                                                                                                                                                                                             |
| **BitBucket**    | Cloud, Server (version 7.2 to 8.13 )               | WebHook                                                                                                                                                                                                                                                                                        |
| **Azure DevOps** | Cloud, Server 2019, TFS Server 2018                | Both WebHook and WorkItem integration                                                                                                                                                                                                                                                          |

## Additional Requirements
* The server requirements depend on your use case. The minimum requirements are: 2 core, 4GB RAM and 20GB disk space
* The CI/CD/Execution toolset must allow executing custom applications  `(jar/cli, docker) - For CLI execution: i.e. Jenkins, Bamboo, GitLab CI, Drone, CircleCI, TravisCI, etc
* The network architecture must support the following:
  * http/s access to Checkmarx
  * Access to the relevant defect management system, if applicable.
  * Connectivity from the repository to the CxFlow web service
* Internal CA root, intermediate and self-signed certificates must be available in the Java JRE truststore (cacerts). This applies to any integration component to ensure that there are no trust issues
* Self-signed certificates must be explicitly trusted by installing them into the Java JRE trustsore (cacerts)
* To enable automated scanning orchestration and project creation (WebHook Web Service integration), the source repository must be capable of supporting WebHooks, specifically:
  * GitLab
  * BitBucket Server/Cloud
  * GitHub
  * Azure DevOps
  * TFS
* Service account credentials and API tokens must be provisioned and made available with access to the relevant tools and services that are related to Defect Management, CI/CD tools, Source Repositories and Checkmarx.
  * Checkmarx will require the ability to create projects (required), , delete projects (optional), create team (optional, if multi-tenant is configuration is set)
  * Jira and other bug trackers will require the ability to create/close/transition issues according to defined configuration

### GitHub Personal Access Token
Create a token as follows:
1. Select your profile and then click **Settings** (upper right corner).
1. Click **Developer settings > Personal Access Tokens > Generate New Token**.
1. Assign a name an add a note to the token, **repo:status** and **public_repo** under the repo section

[[/Images/prereq1.png|GitHub token example]]

### Azure DevOps Access Token
The Azure Access Token that must be configured with CxFlow must meet the following requirements for Push and Pull Request events:
* Code (Read & write)
* Work Items (Read, write, & manage)

[[/Images/prereq2azure.PNG|Azure DevOps Access Token example]]

Tokens only have a life cycle of 365 days maximum so having a secret rotation cycle in place is very important in the long term.