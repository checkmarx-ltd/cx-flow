* [Overview](#overview)
* [CxSAST v8.9 .yml Example File](#eightdotnine)
* [CxSAST v9.0 .yml Example File](#ninedotzero)

### <a name="overview">Overview</a>
This is a set of example yml file configurations used to execute CxFlow.
<br/>All files have configuration examples for the following

**Webhooks**
* Azure
* BitBucket
* GitHub
* GitLab

**Issue Tracking**
* Azure
* GitHub
* GitLab
* JIRA

**Note** The current examples use http communication. It is recommended that the webtokens be changed to use a more secure protocol.

### <a name="eightdotnine">CxSAST v8.9 .yml Example File</a>

```
server:
  port: ${PORT:8982}
logging:
  file: flow.log

cxflow:
  bug-tracker: JIRA
  #bug-tracker-impl:
    # - Azure
    # - Csv
    # - CxXml
    # - GitHub
    # - GitLab
    # - Rally
    # - Json
  branches:
  - master
  - main
  - dev\w+
  - release-\w+
  filter-severity:
  # - High
  filter-category:
  - SQL_Injection
  - Stored_XSS
  - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  # - Urgent
  # - Confirmed
  # - To Verify
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/

checkmarx:
  username: ###<cxsast_username>###
  password: ###<cxsast_password>###
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: http://localhost
  team: \CxServer\SP\Company
  url: ${checkmarx.base-url}/cxrestapi
  #scan-preset: Checkmarx Default
  #incremental: true
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl

azure:
  webhook-token: cxflow:12345
  token: ###<ADOtoken>###
  url: https://dev.azure.com/
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  #block-merge: true
  closed-status: Done
  open-status: "To Do"

bitbucket:
  webhook-token: 12345
  token: ###<bitbucketuser>###:###<bitbuckettoken>###
  url: https://api.bitbucket.org
  api-path: /2.0

github:
  webhook-token: 12345
  token: ###<githubtoken>###
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  #block-merge: true
  #error-merge: true
  #cx-summary: true

gitlab:
  webhook-token: 12345
  token: ###<gitlabtoken>###
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  #block-merge: true

jira:
  url: ###<jira url>###
  username: ###<jira user email>###
  token: ###<jira api token>###
  project: APPSEC
  issue-type: Bug
  label-prefix: < CUSTOM PREFIX NAME >
  priorities:
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: In Progress
  close-transition: Done
  open-status:
    - Backlog
    - Selected for Development
    - In Progress
  closed-status:
    - Done
  fields:
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: loc
      jira-field-name: LOC
      jira-field-type: label
      jira-default-value: XXXXX
```

### <a name="ninedotzero">CxSAST v9.0 .yml Example File</a>
```
server:
  port: ${PORT:8982}
logging:
  file: flow.log

cxflow:
  bug-tracker: JIRA
  #bug-tracker-impl:
    # - Azure
    # - Csv
    # - CxXml
    # - GitHub
    # - GitLab
    # - Rally
    # - Json
  branches:
  - master
  - main
  - dev\w+
  - release-\w+
  filter-severity:
  - High
  filter-category:
  # - SQL_Injection
  # - Stored_XSS
  # - Reflected_XSS_All_Clients
  filter-cwe:
  filter-status:
  # - Urgent
  # - Confirmed
  # - To Verify
  #mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  #wiki-url: https://custodela.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://checkmarx-demo.codebashing.com/courses/
  enabled-vulnerability-scanners:
  #  - sca
    - sast

checkmarx:
  version: 9.0
  username: ###<cxsast_username>###
  password: ###<cxsast_password>###
  client-id: resource_owner_client
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  scope: access_control_api sast_rest_api
  base-url: http://cx.local
  #multi-tenant: true
  configuration: Default Configuration
  #scan-preset: Checkmarx Defaul
  preserve-xml: true
  team: /CxServer/Checkmarx/CxFlow
  url: ${checkmarx.base-url}/cxrestapi
  #scan-preset: Checkmarx Default
  #incremental: true
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx
  sdk-url: ${checkmarx.base-url}/cxwebinterface/SDK/CxSDKWebService.asmx
  portal-wsdl: ${checkmarx.base-url}/Portal/CxWebService.asmx?wsdl
  sdk-wsdl: ${checkmarx.base-url}/SDK/CxSDKWebService.asmx?wsdl

azure:
  webhook-token: cxflow:12345
  token: ###<ADOtoken>###
  url: https://dev.azure.com/
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  #block-merge: true
  closed-status: Done
  open-status: "To Do"

bitbucket:
  webhook-token: 12345
  token: ###<bitbucketuser>###:###<bitbuckettoken>###
  url: https://api.bitbucket.org
  api-path: /2.0

github:
  webhook-token: 12345
  token: ###<githubtoken>###
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  #block-merge: true
  #error-merge: true
  #cx-summary: true

gitlab:
  webhook-token: 12345
  token: ###<gitlabtoken>###
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  #block-merge: true

jira:
  url: ###<jira url>###
  username: ###<jira user email>###
  token: ###<jira api token>###
  project: APPSEC
  issue-type: Bug
  label-prefix: < CUSTOM PREFIX NAME >
  priorities:
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: In Progress
  close-transition: Done
  open-status:
    - Backlog
    - Selected for Development
    - In Progress
  closed-status:
    - Done
  fields:
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: loc
      jira-field-name: LOC
      jira-field-type: label
      jira-default-value: XXXXX
```