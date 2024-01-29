When using CxFlow to initiate checkmarx scans (CxSAST or CxSCA), you can use CxFlow parse logic to control pull requests or build status.

* [Break build](#breakbuild)
* [Block pull request](#blockpullrequest)
* [SCM configurations](#scmconfigurations)
* [Thresholds vs Basic filters](#thresholds)
* [Filter vulnerabilities by groovy script](#filterbygroovyscript)

## <a name="breakbuild">Break build</a>
This option is enabled when CxFlow runs as part of build process by command line. By default, CxFlow exits the operation with exit code: 0

In case of breaking the build according to policy, CxFlow will exit with exit code 10.

To enable the build status (break build) checks, add the following property to CxFlow configuration:
```yaml
cx-flow:
  break-build: true
```

## <a name="blockpullrequest">Block pull request</a>
To enable pull request status checks, add the following properties, under the repository configuration:
```yaml
block-merge: true
error-merge: true
```

For example, if you use GitHub SCM, add these properties under github section:
```yaml
github:
  block-merge: true
  error-merge: true
```

#### <a name="scmconfigurations">SCM configurations</a>

In order to use CxFlow status checks to control vulnerable code and block developers actions, you need to configure it in the SCM branches settings:

<u>**GitHub**</u>:

Under settings->branches, mark checkmarx status check as required to pass before merging:
[[/Images/github_status_check.png|github status check]]

<u>**Azure**</u>:

Under Branch policies configuration, enable Checkmarx scan as ‘required’ (https://docs.microsoft.com/en-us/azure/devops/repos/git/pr-status-policy?view=azure-devops)

[[/Images/azure_branch_policies.png|azure branch policies]]

[[/Images/azure_enable_checkmarx_scan.png|enable checkmarx scan]]

<u>**Bitbucket Cloud**</u>:

User needs to define a pilicy in which approval of checkmarx user is mandatory then it will block merge.


#### <a name="note">Note</a>

<u>**GitLab**</u>:

CxFlow does not support blocking pull request in GitLab. If **block-merge: true and error-merge: true** then CxFlow will post status of vulnerability as comment but it will not block PR.

<u>**AWS Code build (Buildspec)**</u>:

If the build is not breaking because the pipeline is not able to get the exit code, the user can add the following script to catch the exit code from the Cx-Flow logs.
```****
export EXIT_CODE=$(grep 'Finished with exit code:' cx-flow.log | tail -1 |sed 's/.*: //')

echo $EXIT_CODE
```

## <a name="thresholds">Thresholds vs Basic filters</a>

By default, CxFlow uses the basic filter configuration to make a ‘break decision’.

For example, if you configure filter:  ```Filter-severity: HIGH``` - CxFlow results will contain only High severity findings. if ```number-of-results > 0```,  CxFlow will fail the build/PR. If  ```number-of-results = 0``` build/PR is approved.

Now you can add concrete thresholds to control the validation logic:
```yaml
cx-flow:
  thresholds:
    high: 10
    medium: 10
    low: 10
```

[[/Images/cxflow_thresholds.png|thresholds screenshot]]

Thresholds can be used to break build or to block and fail pull requests.

The threshold values (numbers) refer to the numbers of the total findings, and not to the number of issues.

Threshold section is optional. In case of not defining it, if a certain scan has any findings, CxFlow will mark the pull request as failed. Otherwise, the pull request will be approved.

Threshold properties (high, medium and low) are also optional. In case of omitting, comparing with null or with an empty value, threshold check for these properties won’t be performed.

<u>In the following examples, ‘medium’ severity threshold won’t be applied:</u>

Thresholds:
* High: 1
* Medium:
* Low: 3

Thresholds:
* High: 1
* Low: 3

Thresholds:
* High: 1
* Medium: null
* Low: 3

In terms of prioritization - Thresholds are checked after the execution of filters.

#### *Notes:*

1. If thresholds section exists, break-build is always true
2. For SCA thresholds, see this page: [SCA Thresholds](https://github.com/checkmarx-ltd/cx-flow/wiki/CxSCA-Integration#thresholds)
3. If thresholds section exists and user want to disable break-build functionality. Then add below tag in file

```yaml
cxflow:
  disable-break-build: true
```

## <a name="directdependency">SCA : Direct dependency </a>
User can filter direct dependency vulnerabilities present in project. User need to add below code in YML file or pass it as command line parameter under SCA section.

```yaml
sca:
  filter-out-InDirectdependency: true
```
Default value of filter-dependency-type is **All**.

## <a name="directdependency">SCA : Dev & Test dependency</a>
User can filter out dev dependency & Test vulnerabilities present in project. User need to add below code in YML file or pass it as command line parameter under SCA section.

```yaml
sca:
  filter-Out-Devdependency: true
```
Default value of filter-Out-Devdependency: **false** .



## <a name="filterbygroovyscript">Filter vulnerabilities by groovy script</a>
See [here](https://github.com/checkmarx-ltd/cx-flow/wiki/Work-with-external-scripts#use-a-script-to-filter-findings).

