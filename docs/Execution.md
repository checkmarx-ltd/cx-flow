* [WebHook](#webhook)
* [Docker](#docker)
* [Command Line](#command)
* [Parse](#parse)
* [Batch](#batch)

## <a name="webhook">WebHook</a>
Refer to [Webhook Registration](https://github.com/checkmarx-ltd/cx-flow/wiki/WebHook-Registration) for instructions on registering for WebHooks.
To launch the WebHook WebService, select the [relevant jar file](https://github.com/checkmarx-ltd/cx-flow/wiki/Building-CxFlow-from-the-Source) and run the following command:

`java -jar cx-flow-<ver>.jar`

The relevant configuration is determined by the **application.yml** file that resides in the same directory as the jar file, or if an explicit configuration is set to override as defined in the command line as follows:

`java -jar cx-flow-<ver>.jar --spring.config.location=/path/to/application.yml --web`

## <a name="docker">Docker</a>
The CxFlow docker images on Docker Hub [checkmarx/cx-flow](https://hub.docker.com/r/checkmarx/cx-flow) contain the latest and previous versions of CxFlow.

```
docker pull checkmarx/cx-flow
docker run -e JAVA_OPTS="Specify JVM options here" --env-file=.checkmarx --name=cx-flow --detach -p <host port>:8080 checkmarx/cx-flow
```

The env-file provides the necessary overrides during the bootstrap process - urls, credentials, etc - sample below.
In addition, you can use --env-file <(env)` on *Unix hosts to move the entire environment from the host into the Docker container.
```
BITBUCKET_TOKEN=<user>:<token>
BITBUCKET_URL=http://xxxxxx:7990
BITBUCKET_API_PATH=/rest/api/1.0/
BITBUCKET_WEBHOOK_TOKEN=XXXXXXX
CHECKMARX_BASE_URL=https://xxxxxxxx
CHECKMARX_CLIENT_SECRET=XXXXXXXXXX
CHECKMARX_PASSWORD=XXXXXXXX
CHECKMARX_USERNAME=XXXXXXXX
CHECKMARX_TEAM=\CxServer\SP\Checkmarx
GITHUB_TOKEN=XXXXXXXXXXXXXX
GITHUB_WEBHOOK_TOKEN=XXXXXXXX
GITLAB_TOKEN=XXXXXXXX
GITLAB_WEBHOOK_TOKEN=XXXXXXXX
JIRA_TOKEN=XXXXXXX
JIRA_USERNAME=XXXXXX
JIRA_URL=https://XXXXXXXXX
JIRA_PROJECT=SS
```

**Note**:  In order to highly customize the yaml configuration for CxFlow in a Docker environment, use this docker image as a base and add custom configuration.  Alternatively build from source (Docker files are found in the git repository).

## <a name="command">Command Line</a>
CxFlow can be integrated via command line using several ways. The table below lists command line arguments and flags to help drive the different execution flows and overrides.

| Option | Description |
|--------|-------------|
| `--spring.config.location` | Override the main application.yml/properties file for the application.  Defaults to the application.yml packaged within the jar |
| `--parse` | Indicates that a result XML file from Checkmarx is provided (`–f` is also mandatory).  No value provided (flag) |
| `--project` | Indicates that we would like to retrieve the latest scan results for a given team/project and provide feedback (defect / issue tracking). No value provided (flag) |
| `--batch` | Indicates that the entire instance or a given team is iterated through and the latest results are retrieved for each project and feedback is provided (defect/issue tracking) |
| `--cx-team` | Used to override the team that is used as a base team (optionally defined globally in the yaml configuration).  This team is used when creating a project in Source/Scan (zip) mode as well as the team to use when retrieving latest project results in project/batch modes (--project/--batch) | `--cx-project` | Used to create the project in Source/Scan (zip) mode and to indicate, for which project to retrieve the latest results in Project mode (`--project`) |
| `--namespace` | Repository group (GitLab)/organization (GitHub)/namespace (BitBucket). Used as higher level grouping of repositories.  Used along with repo-name and branch for tracking purposes (Jira only).  If these three components are not present, an application attribute must be passed (**--app**).  These values are stored in a tracking label within Jira.  This value is also stored in the body of the issue. |
| `--repo-name` | Name of the repository.  Used along with repo-name and branch for tracking purposes (Jira Only).  If these three components are not present, application attribute must be passed (**--app**).  These values are stored in a tracking label within Jira.  This value is also stored in the body of the issue. |
| `--branch` | Branch used along with repo-name and branch for tracking purposes (Jira only).  If these three components are not present, then an application attribute must be passed  (**--app**).  These values are stored in a Tracking label within Jira. This value is also stored in the body of the issue. |
| `--app` | Alternatively used for Tracking purposes.  This value is also stored in the body of the issue. |
| `--repo-url` | Required for issues tracking with GitHub Issues or GitLab Issues.  This value is also stored in the body of the issue. |
| `--f` | File to be processed.  This the output from Checkmarx CLI, Jenkins/Bamboo Plugin, etc. |
| `--exclude-files` | Files to be excluded when running --scan CLI execution |
| `--exclude-folders` | Folders to be excluded when running --scan CLI execution |
| `--config` | Optional: Configuration override file (JSON) |
| `--bbs` | Optional: Indicates that the repository is of the BitBucket Server type as BB Server follows a different URL file format |
| `--bb` | Optional: Indicates that the repository is of the BitBucket Cloud type as BB Cloud follows a different URL file format (also different from BB Server) |
| `--bug-tracker` | Optional: Used to override the globally configured bug tracker as defined by the base YAML configuration.  The name is case-sensitive and must match the exact bean name as specified in the --bug-tracker-impl list of available implementations. JIRA is the only option that is not on this list, but can be used as well |
| `--spring.config.location` | Path to application.yml. This file contains the global configuration for CxFlow.  It is only required, if the jar file and the application.yml file are not in the current working directory.  Refer to the [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) (section 24.3) |
| `--offline` | If this flag is raised, the Checkmarx instance is not contacted.  This means that no issue description is provided and Checkmarx custom fields cannot be used |
| `--blocksysexit` | Optional: Mainly for build/test purposes. Avoid `System.exit()` in the code and exit with java exception |
| `--alt-project` | Name of the project in ADO. This parameter is required in addition to cx-project parameter. |
| `--project-custom-field` | Specify a project-level custom field to be set if a project is created or the `checkmarx.settings-override` property is set. The custom field is specified as *name:value* (i.e., the field name cannot include a colon). This option may be specified multiple times to set multiple fields. |
| `--scan-custom-field` | Specify a scan-level custom field. The custom field is specified as *name:value* (i.e., the field name cannot include a colon). This option may be specified multiple times to set multiple fields. |
## <a name="parse">Parse</a>

```
java -jar cx-flow-<ver>.jar  \
--parse \
--namespace=checkmarx \
--repo-name=Riches.NET \
--repo-url=https://github.com/xxxx/xxxx.git \
--branch=main \
--app=ABC \
--f=Checkmarx/Reports/ScanReport.xml
```

## <a name="batch">Batch</a>
### Entire Instance
`java -jar cx-flow-<ver>.jar --batch`

### Specific Team
Example for Checkmarx v9.x:

`java -jar cx-flow-<ver>.jar --batch --cx-team="CxServer/SP/Checkmarx/development"`

Example for Checkmarx v8.x:

`java -jar cx-flow-<ver>.jar --batch --cx-team="CxServer\SP\Checkmarx/development"`

### Single Project

Example for Checkmarx v9.x:

```
java -jar cx-flow-<ver>.jar \
--project \
--cx-team="CxServer/SP/Checkmarx/Test" \
--cx-project="riches-main" \
--app=AppName
```

Example for Checkmarx v8.x:

```
java -jar cx-flow-<ver>.jar \
--project \
--cx-team="CxServer\SP\Checkmarx\Test" \
--cx-project="riches-main" \
--app=AppName
```

### Docker

```
docker pull checkmarx/cx-flow
docker run checkmarx/cx-flow <applicable parameters>
```
