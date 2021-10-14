* [Create ECR Repository](#ecr)
* [Download CxFlow](#download)
* [Configure CxFlow](#configure)
* [Build Docker Image](#build)
* [Create SSM Parameters/Secrets](#ssm)
* [Define ECS IAM Roles/Permissions](#iam)
* [Define ECS Task Definition](#task)
* [Create Service](#createservice)
* [CxFlow Security Groups](#security)
* [Monitoring](#monitoring)

## <a name="ecr">Create ECR Repository</a>
https://docs.aws.amazon.com/AmazonECR/latest/userguide/repository-create.html
<br/>The steps below assume a repository named cxflow

## <a name="download">Download CxFlow</a>
Grab the latest binary from the following:
<br/>https://github.com/checkmarx-ts/cx-flow/releases/latest
<br/>Download the jar file, for example: cx-flow-1.5.3.jar 
<br/. **Note** There is a JDK 11 version as well - cx-flow-11-1.5.3.jar

## <a name="configure">Configure CxFlow</a>
Configure CxFlow as per the custom deployment needs, see (Configuration)[https://github.com/checkmarx-ltd/cx-flow/wiki/Configuration]
<br/>The configuration should be completed using a file named application.yml.  Keep the JAR for CxFlow and the configuration (application.yml) file in the same directory
<br/>A base configuration can be found below for 8.9 and 9.0, save each file as application.yml
<br/>**Note** Default bug tracker is Azure in this example, but can be changed
<br/>**Note** Configuration file should be configured for specific use cases, scanning/branching criteria, result feedback, and/or result filtering policy.
<br/>8.9 Example application.yml:
```
server:
  port: ${PORT:8080}

logging:
  file:
    name: cx-flow.log

cx-flow:
  # Agreed upon shared API XXXXXXXXXXXXX
  tokentoken: xxxx
  bug-tracker: Azure
  bug-tracker-impl:
    - GitHub
    - Azure
  branches:
    - develop
    - main
    - security
  filter-severity:
    - High
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://cxa.codebashing.com/courses/
  auto-profile: true
  #branch-script: D:\\tmp\Branch.groovy
  #zip-exclude: .*.json$, bin\/.*
  #list-false-positives: true

checkmarx:
  username: xxx
  password: xxx
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  base-url: http://cx.local
  multi-tenant: true
  configuration: Default Configuration
  scan-preset: Checkmarx Default
  team: /CxServer/Checkmarx/CxFlow
  url: ${checkmarx.base-url}/cxrestapi
  preserve-xml: true
  incremental: true
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx

jira:
  url: https://xxxxx.atlassian.net
  username: xxxxxxxxxxxxx
  token: xxxx
  project: APPSEC
  http-timeout: 50000
  issue-type: Application Security Bug
  priorities:
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: Reopen Issue
  close-transition: Close Issue
  open-status:
    - Open
    - Reopened
    - In Progress
    - False-Positive
    - Assistance-Required
  closed-status:
    - Closed
    - Resolved
  fields:
    - type: result
      name: system-date
      skip-update: true
      offset: 60
      jira-field-name: Due Date #Due date (cloud)
      jira-field-type: text
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: severity
      jira-field-name: Severity
      jira-field-type: single-select
    - type: result
      name: loc
      jira-field-name: Line Numbers
      jira-field-type: label
    - type: static
      name: identified-by
      jira-field-name: Identified By
      jira-field-type: single-select
      jira-default-value: Automation
    - type: static
      name: dependencies
      jira-field-name: Dependencies
      jira-field-type: multi-select
      jira-default-value: Java, AngularJS
    - type: result
      name: not-exploitable
      jira-field-name: False Positive LOC
      jira-field-type: label

github:
  webhook-token: 1234
  token: xxx
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true
  error-merge: true
  cx-summary: true

gitlab:
  webhook-token: 1234
  token: xxx
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  cx-summary: true


bitbucket:
  webhook-token: 1234
  token: xxx
  url: https://api.bitbucket.org
  api-path: /2.0
  false-positive-label: false-positive
  cx-summary: true

azure:
  webhook-token: cxflow:1234
  token: xxxx
  url: https://dev.azure.com/XXXXXX
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  cx-summary: true
  block-merge: true
  error-merge: true
  closed-status: Closed
  open-status: Active

json:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].json"
  data-folder: "/tmp"

cx-xml:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
  data-folder: "/tmp"

csv:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].csv"
  data-folder: "/tmp"
  include-header: true
  fields:
    - header: Customer field (Application)
      name: application
      default-value: unknown
    - header: Primary URL
      name: static
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
      name: summary
      prefix: "*"
      postfix: "*"
    - header: Severity
      name: severity
    - header: recommendation
      name: recommendation
    - header: Similarity ID
      name: similarity-id
```
<br/>9.0 Example application.yml
```
server:
  port: ${PORT:8080}

logging:
  file:
    name: cx-flow.log

cx-flow:
  # Agreed upon shared API token
  token: xxxx
  bug-tracker: Azure
  bug-tracker-impl:
    - GitHub
    - Azure
  branches:
    - develop
    - main
    - security
  filter-severity:
    - High
  mitre-url: https://cwe.mitre.org/data/definitions/%s.html
  wiki-url: https://checkmarx.atlassian.net/wiki/spaces/AS/pages/79462432/Remediation+Guidance
  codebash-url: https://cxa.codebashing.com/courses/
  auto-profile: true
  #branch-script: D:\\tmp\Branch.groovy
  #zip-exclude: .*.json$, bin\/.*
  #list-false-positives: true

checkmarx:
  version: 9.0
  username: xxxxx
  password: xxxxx
  client-id: resource_owner_client
  client-secret: 014DF517-39D1-4453-B7B3-9930C563627C
  scope: access_control_api sast_rest_api
  base-url: http://cx.local
  multi-tenant: true
  configuration: Default Configuration
  scan-preset: Checkmarx Default
  team: /CxServer/Checkmarx/CxFlow
  url: ${checkmarx.base-url}/cxrestapi
  preserve-xml: true
  incremental: true
  #WSDL Config
  portal-url: ${checkmarx.base-url}/cxwebinterface/Portal/CxWebService.asmx

jira:
  url: https://xxxxx.atlassian.net
  username: xxxxxxxxxxxxx
  token: xxxx
  project: APPSEC
  http-timeout: 50000
  issue-type: Application Security Bug
  label-prefix: < CUSTOM PREFIX NAME >
  priorities:
    High: High
    Medium: Medium
    Low: Low
    Informational: Lowest
  open-transition: Reopen Issue
  close-transition: Close Issue
  open-status:
    - Open
    - Reopened
    - In Progress
    - False-Positive
    - Assistance-Required
  closed-status:
    - Closed
    - Resolved
  fields:
    - type: result
      name: system-date
      skip-update: true
      offset: 60
      jira-field-name: Due Date #Due date (cloud)
      jira-field-type: text
    - type: result
      name: application
      jira-field-name: Application
      jira-field-type: label
    - type: result
      name: category
      jira-field-name: Category
      jira-field-type: label
    - type: result
      name: cwe
      jira-field-name: CWEs
      jira-field-type: label
    - type: result
      name: severity
      jira-field-name: Severity
      jira-field-type: single-select
    - type: result
      name: loc
      jira-field-name: Line Numbers
      jira-field-type: label
    - type: static
      name: identified-by
      jira-field-name: Identified By
      jira-field-type: single-select
      jira-default-value: Automation
    - type: static
      name: dependencies
      jira-field-name: Dependencies
      jira-field-type: multi-select
      jira-default-value: Java, AngularJS
    - type: result
      name: not-exploitable
      jira-field-name: False Positive LOC
      jira-field-type: label

github:
  webhook-token: XXXXXXXXXXXXX
  token: xxx
  url: https://github.com
  api-url: https://api.github.com/repos/
  false-positive-label: false-positive
  block-merge: true
  error-merge: true
  cx-summary: true

gitlab:
  webhook-token: XXXXXXXXXXXXX
  token: xxx
  url: https://gitlab.com
  api-url: https://gitlab.com/api/v4/
  false-positive-label: false-positive
  cx-summary: true


bitbucket:
  webhook-token: XXXXXXXXXXXXX
  token: xxx
  url: https://api.bitbucket.org
  api-path: /2.0
  false-positive-label: false-positive
  cx-summary: true

azure:
  webhook-token: cxflow:XXXXXXXXXXXXX
  token: xxxx
  url: https://dev.azure.com/XXXXXX
  issue-type: issue
  api-version: 5.0
  false-positive-label: false-positive
  cx-summary: true
  block-merge: true
  error-merge: true
  closed-status: Closed
  open-status: Active

json:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].json"
  data-folder: "/tmp"

cx-xml:
  file-name-format: "[NAMESPACE]-[REPO]-[BRANCH]-[TIME].xml"
  data-folder: "/tmp"

csv:
  file-name-format: "[TEAM]-[PROJECT]-[TIME].csv"
  data-folder: "/tmp"
  include-header: true
  fields:
    - header: Customer field (Application)
      name: application
      default-value: unknown
    - header: Primary URL
      name: static
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
      name: summary
      prefix: "*"
      postfix: "*"
    - header: Severity
      name: severity
    - header: recommendation
      name: recommendation
    - header: Similarity ID
      name: similarity-id
```
## <a name="build">Build Docker Image</a>
Use the following as a base Docker file
```
FROM alpine:3.11
VOLUME /tmp
RUN apk add openjdk8-jre && apk update && apk upgrade
ADD cx-flow-1.5.3.jar //
ADD application.yml //
ENTRYPOINT ["java"]
CMD ["-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-jar", "/cx-flow-1.5.3.jar", "--spring.config.location=/application.yml", "--web"]
EXPOSE 8080:8080
```
Build the image, and tag it for ECR and push to the remote repository (created in previous <a name="ecr">step</a>)
```
$(aws ecr get-login --no-include-email --region us-east-1)
docker build -t cxflow .
docker tag cxflow XXXXXXXXXX.dkr.ecr.us-east-1.amazonaws.com/cxflow
docker push XXXXXXXXXX.dkr.ecr.us-east-1.amazonaws.com/cxflow
```
**Note** XXXXXXXXXX is the AWS account, and this example is using the us-east-1 region.  This image is by default tagged/pushed as latest unless specified

## <a name="ssm">Create SSM Parameters/Secrets</a>
There are several SSM parameters (mainly secure SSM parameters).  Feel free to give different naming patterns, but remember to reflect the changes in the ECS task definition as well as the IAM policy references.   These are the commands for using the AWS CLI and are leveraging the base KMS key.  Please refer to SSM documentation for variations.  Console configuration is also possible.  
```
#Checkmarx User
aws ssm put-parameter --name "/cxflow/checkmarx/username" --value "admin" --type SecureString

#Checkmarx Password
aws ssm put-parameter --name "/cxflow/checkmarx/password" --value "XXXXXX" --type SecureString

#It is worth noting that this below reference will result in what the url resolves.  Go into the console and update if required (validate this)
aws ssm put-parameter --name "/cxflow/checkmarx/url" --value "https://cx.local" --type String

#This token is associated with the default endpoint for driving a scan/results but that is not also associated with a 
#webhook event payload (which is a separate token as per below)
aws ssm put-parameter --name "/cxflow/token" --value "XXXXXX" --type SecureString

#Preshared secret between GitHub and CxFlow - used when registering the webhook for auth 
aws ssm put-parameter --name "/cxflow/github/webhook-token" --value "XXXXXX" --type SecureString

#GitHub Service account API token (Personal access token) used for driving git scanning in Checkmarx
#posting MD comments in PRs, Updating statuses, Creating GitHub issues (if applicable)
aws ssm put-parameter --name "/cxflow/github/token" --value "XXXXXX" --type SecureString

#Preshared secret between GitLab and CxFlow - used when registering the webhook for auth 
aws ssm put-parameter --name "/cxflow/gitlab/webhook-token" --value "XXXXXX" --type SecureString

#GitLab Service account API token used for driving git scanning in Checkmarx
#posting MD comments in MRs, Creating GitLab issues (if applicable)
aws ssm put-parameter --name "/cxflow/gitlab/token" --value "XXXXXX" --type SecureString

#Same pattern as above for GitHub/GitLab, instead for Azure DevOps and BitBucket
aws ssm put-parameter --name "/cxflow/azure/webhook-token" --value "<user>:XXXXXX" --type SecureString
aws ssm put-parameter --name "/cxflow/azure/token" --value "XXXXXX" --type SecureString
aws ssm put-parameter --name "/cxflow/bitbucket/webhook-token" --value "XXXXXX" --type SecureString
aws ssm put-parameter --name "/cxflow/bitbucket/token" --value "<use>:XXXXXX" --type SecureString
```
**Note**  Using SSM will avoid the use of any unencrypted credentials and will be automatically injected into the CxFlow process.

## <a name="iam">Define ECS IAM Roles/Permissions</a>
ECS will need appropriate access to IAM resources including SSM Parameters, KMS key, Cloud Watch log streams, ECR registry as well as must be able to be assumed.
<br/>The trust policy must be defined as the following to allow this:
```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```
**Note** The Role must be associated with ECS and Container Service Definition.
[[/Images/fargate1.png|AWS Screenshot]]
The role should contain the base managed policy of **AmazonECSTaskExecutionRolePolicy** to ensure proper access is given to base ECS resources from within task execution.
[[/Images/fargate2.png|Amazon ECS Task Execution Role Policy selected]]
Additional IAM policy is used to drive the above mentioned SSM parameters / KMS keys, Cloudwatch logs, etc.  This can be used as a starting point but should be adjusted or restricted further based on specific needs (i.e. log event groups/streams can be narrowed appropriately):
```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParametersByPath",
        "ssm:GetParameters",
        "kms:Decrypt"
      ],
      "Resource": [
        "arn:aws:ssm:us-east-1:<Acct ID>:parameter/cxflow/*",
        "arn:aws:kms:us-east-1:<Acct ID>:key/<KMS Key ID>"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
```
## <a name="task">Define ECS Task Definition</a>
The following ECS Task Definition can be used to define the CxFlow Task and inject the appropriate ENV variables linked from SSM Secure Parameters (note the secrets section - there are direct links to the ssm parameter paths as defined within the same region and the ENV variables that will be inject/expected in CxFlow) - also note the log definition that will send output to cloudwatch logs:
<br/>fargate.json
```
{
  "containerDefinitions": [{
    "portMappings": [{
        "hostPort": 8080,
        "containerPort": 8080,
        "protocol": "http"
      }],
      "essential": true,
      "name": "cxflow-container",
      "image": "275043232443.dkr.ecr.us-east-1.amazonaws.com/cxflow:latest",
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/app/cxflow",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "cx-"
        }
      },
      "secrets": [{
        "name": "CHECKMARX_USERNAME",
        "valueFrom": "/cxflow/checkmarx/username"
      },
      {
        "name": "CHECKMARX_PASSWORD",
        "valueFrom": "/cxflow/checkmarx/password"
      },
      {
        "name": "CHECKMARX_BASE_URL",
        "valueFrom": "/cxflow/checkmarx/url"
      },
      {
        "name": "CX_FLOW_TOKEN",
        "valueFrom": "/cxflow/token"
      },
      {
        "name": "GITHUB_TOKEN",
        "valueFrom": "/cxflow/github/token"
      },
      {
        "name": "GITHUB_WEBHOOK_TOKEN",
        "valueFrom": "/cxflow/github/webhook-token"
      },
      {
        "name": "AZURE_TOKEN",
        "valueFrom": "/cxflow/azure/token"
      },
      {
        "name": "AZURE_WEBHOOK_TOKEN",
        "valueFrom": "/cxflow/azure/webhook-token"
      },
      {
        "name": "GITLAB_TOKEN",
        "valueFrom": "/cxflow/gitlab/token"
      },
      {
        "name": "GITLAB_WEBHOOK_TOKEN",
        "valueFrom": "/cxflow/gitlab/webhook-token"
      },
      {
        "name": "BITBUCKET_TOKEN",
        "valueFrom": "/cxflow/bitbucket/token"
      },
      {
        "name": "BITBUCKET_WEBHOOK_TOKEN",
        "valueFrom": "/cxflow/bitbucket/webhook-token"
      }]
    }
  ],
  "networkMode": "awsvpc",
  "executionRoleArn": "arn:aws:iam::275043232443:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::275043232443:role/ecsTaskExecutionRole",
  "memory": "2048",
  "cpu": "512",
  "requiresCompatibilities": [
    "FARGATE"
  ],
  "family": "cxflow"
}

```

<br/>
The following command can be used to define the task definition within ECS (using AWS CLI):

```
aws ecs register-task-definition --cli-input-json file://fargate.json
```

## <a name="createservice">Create Service</a>
To create a service to run the defined task the following can be leveraged:

```
aws ecs create-service --cluster FieldDevelopment --service-name cxflow-service --task-definition cxflow:1 --desired-count 1 --launch-type "FARGATE" --network-configuration "awsvpcConfiguration={subnets=[subnet-XXXXXX],securityGroups=[sg-XXXXXX,sg-XXXXXXX], assignPublicIp=ENABLED}"
```

<br/>**Note** The --task-definition # (i.e. cxflow:1) is linked directly with the current version, which is applicable to the number of times the definition has been revised.  
<br/>**Note** This command should be tailored to the specific use case.  Specifically, network architecture must be considered (awsvpcConfiguration).  The above assigns a Public IP directly, which will not be appropriate for most (if not all) deployments.  This should consider any ALB, NGINX and other containers within the orhestration.

## <a name="security">CxFlow Security Groups</a>
<br/>CxFlow should NOT be exposed directly on the internet without considering the requirements for connectivity.  
<br/>GitHub, Azure DevOps, Bitbucket all have IP CIDR ranges that can be referenced for white-listing:
<br/>GitHub: https://help.github.com/en/github/authenticating-to-github/about-githubs-ip-addresses
<br/>Azure: https://docs.microsoft.com/en-us/azure/devops/organizations/security/allow-list-ip-url?view=azure-devops
<br/>BitBucket: https://confluence.atlassian.com/bitbucket/what-are-the-bitbucket-cloud-ip-addresses-i-should-use-to-configure-my-corporate-firewall-343343385.html
<br/>On-premise architecture and security controls should be reviewed and implemented on a per-customer basis.
<br/>**Note** GitLab does not have a defined CIDR range and must be considered with caution
<br/>**Note** CxFlow should be placed into a network zone / DMZ that does not have any network access beyond required connectivity points.

## <a name="monitoring">Monitoring</a>
Actuator endpoint can be used to monitor whether the service is up / online:
<br/>Example: http://cxflow.XXXXX:8080/actuator/health

```
{
"status": "UP"
}
```

<br/>**Note**  Actuator can be leveraged for many other monitoring purposes:
<br/>https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html