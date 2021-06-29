@IastFeature @IntegrationTest
Feature: Check integration with iast - stop scan and create jira issue by CLI/Jenikns
And command line example: ”java -jar cx-flow-1.6.21.jar --spring.config.location=application.yml --iast --bug-tracker="jira" --assignee="email@mail.com" --scan-tag="cx-scan-tag-1" --jira.url=https://xxxx.atlassian.net --jira.username=email@mail.com --jira.token=token --jira.project=BCB --iast.url="http://localhost" --iast.manager-port=8380 --iast.username="username" --iast.password="password" --iast.update-token-seconds=150 --jira.issue-type="Task"”


  Scenario Outline: test cli runner
    Given mock services "<scanTag>" "<filter-severity>" "<thresholds severity>"
    Given mock CLI runner "<scanTag>" "<bug tracker>"
    When running cli "<exit code>"
    Then check how many create issue "<create issue>" "<bug tracker>"

    Examples:
      | scanTag   | bug tracker | filter-severity      | thresholds severity              | exit code | create issue |
      | cx-scan-1 | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |
      | cx-scan-2 | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |
      | cx-scan-2 | jira        | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            |
      | cx-scan-2 | jira        | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            |
      | cx-scan-2 | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            |
      | cx-scan-2 | jira        | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            |
      | cx-scan-2 | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            |
      | cx-scan-2 | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            |

      | cx-scan-1 | githubissue | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |
      | cx-scan-2 | githubissue | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |
      | cx-scan-2 | githubissue | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            |
      | cx-scan-2 | githubissue | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            |
      | cx-scan-2 | githubissue | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            |
      | cx-scan-2 | githubissue | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            |
      | cx-scan-2 | githubissue | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            |
      | cx-scan-2 | githubissue | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            |

      | cx-scan-1 | azureissue  | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |
      | cx-scan-2 | azureissue  | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |
      | cx-scan-2 | azureissue  | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            |
      | cx-scan-2 | azureissue  | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            |
      # TODO: all tests with "exit code != 0" fails:
      # the following case: when the first MEDIUM issue is found, the execution is stopped. The process fail as it as
      # expected, but the number of created issues is equals to ONE, and the expected is TWO
      | cx-scan-2 | azureissue  | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            |
      # this next one fails as it as expected (exit code = 10) and not create any issue, because the MEDIUM vulnerability
      # was found and, since that a MEDIUM is not eligible to create an issue (it is not between filter-severity values),
      # the execution is stopped because of the thresholdsSeverity validation (MEDIUM is defined as 1 on thresholds severity values).
      ### summing up: MEDIUM vulnerabilities does not create an issue, but when it is found the process FAIL (exit code = 10) ###
      | cx-scan-2 | azureissue  | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            |
      | cx-scan-2 | azureissue  | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            |
      | cx-scan-2 | azureissue  | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            |