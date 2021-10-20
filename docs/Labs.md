# Labs

## <a name="quickstart">Quick Start</a>

There are two ways to run CxFlow: 

- Command-line task runner
- Server

At the same folder of CxFlow JAR file, add an `application.yml` file with the following:

```yaml
server:
  port: 8080

logging:
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{15}){cyan}  [%clr(%X{cx}){blue}] %clr(:){faint} %replace(%m){'([\\|])','\\$1'}%n%wEx"
    file: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{15}){cyan}  [%clr(%X{cx}){blue}] %clr(:){faint} %replace(%m){'([\\|])','\\$1'}%n%wEx"
  file:
    name: cx-flow.log

cx-flow:
  token: xxxx
  bug-tracker: Csv
  bug-tracker-impl:
    - CxXml
    - Json
    - JIRA
    - GitLab
    - GitHub
    - Csv
    - Azure
    - Rally
  branches:
    - develop
    - master
    - security
  filter-severity:
    - High
  filter-category:
  filter-cwe:
  filter-status:
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html

checkmarx:
  version: 9.0
  username: your-cxsast-user
  password: your-cxsast-password
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: http://yout-cxsast-instance
  client-id: resource_owner_client
  scope: access_control_api sast_rest_api
  configuration: Default Configuration
  scan-preset: Checkmarx Default
  team: \CxServer
  url: ${checkmarx.base-url}/cxrestapi
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx

csv:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].csv"
  data-folder: "/tmp/cxflow"
  include-header: true
  fields:
    - header: Application
      name: application
      default-value: unknown
    - header: severity
      name: severity
    - header: Vulnerability ID
      name: summary
      prefix: "[APP]:"
    - header: file
      name: filename
    - header: Vulnerability ID
      name: summary
    - header: Vulnerability Name
      name: category
    - header: Category ID
      name: cwe
    - header: Description
      name: description
    - header: Severity
      name: severity
    - header: recommendation
      name: recommendation
    - header: Similarity ID
      name: similarity-id
```

### Running CxFlow as a Command-line task runner

For this example, we will use a well-known public GitHub repository, WebGoat. 

WebGoat is a Java application with intentionally implemented vulnerabilities. It is used for educational purposes. 

```bash
java -jar ./cx-flow.jar \
    --spring.config.location=./application.yml \
    --project \
    --cx-team="CxServer" \
    --cx-project="WebGoat" \
    --namespace=OWASP \
    --repo-name=WebGoat \
    --repo-url=https://github.com/WebGoat/WebGoat.git \
    --branch=master \
    --app=WebGoat
```

### Running CxFlow as a server

This mode is used to listen to Webhooks and RESTful requests.

The command to run in server mode is just the JAR execution, with no additional parameters.

```bash
java -jar ./cx-flow.jar
```