# GitHub example

## Description

This example shows a way to use `Cx-Flow` with GitHub webHook.

## Prerequisite

- A github project.  
- This same project configured in your checkmarx.
- A user/password from a `checkmarx` user. **NOTE** To use some features (error merge, ...), the user language **MUST** be 'english'

## Setup
 
### GitHub apps

`Cx-Flow` will be used as a server. It may be convenient to not use a user-bound token. You can create a GitHub apps, and install it on some repository of your organization.

The GitHub apps will need the permissions:

- Checks (read & write)
- Contents (read & write)
- Issues (read & write)  (Not needed for now)
- Pull requests (read & write) 

The events the GitHub apps will listen are:

- Push
- Pull request

You need setup the webhook URL to the enpoint `Cx-Flow` will be available (eg: https://Cx-Flow.your.org). 

To setup `Cx-Flow`, you will need from this GitHub app the `app-id` and to generate an associated private key. 

### Cx-Flow

#### Private key

`Cx-Flow` is a java application. It is easier for Java to have GitHub private key in a standard format. With openssl tool, you can do that with:

```bash
$ openssl pkcs8 -topk8 -inform PEM -outform PEM -in /your/location/github-app.private-key.pem -out /your/location/private.pem -nocrypt 
```

#### Configuration file

Create a `application.yml` file on the disk. It will be referenced later as `/config/application.yml`.

A Simple template look like:

```yaml
server:
  port: 8080

# env LOG_PATH is also to set to /dev/null
logging:
  file: /dev/null

cx-flow:
  bug-tracker: GitHub
  bug-tracker-impl:
    - GitHub
  # Commit will trigger a scan only if from one of those branches or if a PR target one of those branches.
  branches:
    - develop
    - main
    - master
    - security
  filter-severity:
    - Medium
    - High
    # Checkmarx return severity in the user locale. If you are not using features that need 'english' don't forget to put the correct value.
    - Moyenne
    - Haute
  # Thresholds to set PR in error if error merge is enabled. User locale MUST be english !!!!
  thresholds:
    HIGH: 0
    MEDIUM: 2
  codebash-url: https://xxxxx.codebashing.com/courses/

checkmarx:
  version: 9.0
  username: your_checkmarx_user
  password: your_checkmarx_password
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: https://checkmarx.your.org
  url: https://checkmarx.your.org/CxRestAPI
  multi-tenant: false 
  incremental: true
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl
  portal-package: checkmarx.wsdl.portal

github:
  webhook-token: 123456789
  url: https://github.com
  api-url: https://api.github.com
  false-positive-label: false-positive
  error-merge: true
  block-merge: true
  app:
    id: 9876
    org: your_github_org
    # Secret key is the content of the GitHub app private key you converted in the previous step.
    secret-key: |
      -----BEGIN PRIVATE KEY-----
      MIIEvA...
      ABCDEF...
      123456==
      -----END PRIVATE KEY-----
```

#### Run

The `Cx-Flow` server can now be launched with:

```bash
$ java -jar /your/app/cx-flow.jar --spring.config.location=/config/application.yml --web
```

It will listen port 8080 by default.

### GitHub repository

If you installed the repository in the GitHub app configuration, you only need a configuration file at the root of your repository. (in the correct branches)

This file is named `cx.config` and look like:

```json
{
 "project": "project_name_in_checkmarx",
 "team": "/CxServer/SP/Company/Users",
 "sast": {
   "folderExcludes": "src/test,src/main/resources"
 }
}
```

## result

### On PR

Now you can create PR, which will be commented and checked like:

![PR page](github.png)

### On branches

On push on branches, issues will be created like:

![Issue page](github_issue.png) 