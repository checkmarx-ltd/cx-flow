When using CxFlow to initiate checkmarx scans (SAST or SCA), you can use CxFlow parse logic to control pull requests or build status.

* [Break build](#breakBuild)
* [Block pull request](#blockPullRequest)
  * [SCM configurations](#scmConfigurations)
* [Thresholds vs Basic filters](#thresholds)
* [Filter vulnerabilities by groovy script](#filterByGroovyScript)

## <a name="breakBuild">Break build</a>
This option is enabled when CxFlow runs as part of build process by command line. By default, CxFlow exits the operation with exit code: 0

In case of breaking the build according to policy, CxFlow will exit with exit code 10.

To enable the build status (break build) checks, add the following property to CxFlow configuration:
```yaml
cx-flow:
  break-build: true
```

## <a name="blockPullRequest">Block pull request</a>
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

#### <a name="scmConfigurations">SCM configurations</a>

in order to use CxFlow status checks to control vulnerable code and block developers actions, you need to configure it in the SCM branches settings:

<u>**Github**</u>:

Under settings->branches, mark checkmarx status check as required to pass before merging:
[[/Images/github_status_check.png|github status check]]

<u>**Azure**</u>:

Under Branch policies configuration, enable Checkmarx scan as ‘required’ (https://docs.microsoft.com/en-us/azure/devops/repos/git/pr-status-policy?view=azure-devops)

[[/Images/azure_branch_policies.png|azure branch policies]]

[[/Images/azure_enable_checkmarx_scan.png|enable checkmarx scan]]



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
2. If thresholds section exists, CxFlow expects thresholds configuration for each scanner (SAST and SCA)
3. For SCA thresholds, see this page: [SCA Thresholds](https://github.com/checkmarx-ltd/cx-flow/wiki/Integration-with-CxSCA#thresholds)


## <a name="filterByGroovyScript">Filter vulnerabilities by groovy script</a>

See [here](https://github.com/checkmarx-ltd/cx-flow/wiki/Work-with-external-scripts#use-a-script-to-filter-findings).

