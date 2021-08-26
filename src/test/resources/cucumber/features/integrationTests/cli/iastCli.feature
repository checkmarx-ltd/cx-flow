@IastFeature @IntegrationTest
Feature: Check integration with iast - stop scan and create jira issue by CLI/Jenikns
And command line example: ”java -jar cx-flow-1.6.21.jar --spring.config.location=application.yml --iast --bug-tracker="jira" --assignee="email@mail.com" --scan-tag="cx-scan-tag-1" --jira.url=https://xxxx.atlassian.net --jira.username=email@mail.com --jira.token=token --jira.project=BCB --iast.url="http://localhost" --iast.manager-port=8380 --iast.username="username" --iast.password="password" --iast.update-token-seconds=150 --jira.issue-type="Task"”


  Scenario Outline: test cli runner
    Given mock services "<scanTag>" "<filter-severity>" "<thresholds severity>"
    Given mock CLI runner "<scanTag>" "<bug tracker>" "<params>"
    When running cli "<exit code>" "<scanTag>"
    Then check how many create issue "<create issue>" "<bug tracker>"

    Examples:
      | scanTag    | bug tracker | filter-severity      | thresholds severity              | exit code | create issue | params    |
      | cx-scan-1  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |           |
      | cx-scan-2  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            |           |
      | cx-scan-3  | jira        | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            |           |
      | cx-scan-4  | jira        | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            |           |
      | cx-scan-5  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            |           |
      | cx-scan-6  | jira        | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            |           |
      | cx-scan-7  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            |           |
      | cx-scan-8  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            |           |

      | cx-scan-9  | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            | --github  |
      | cx-scan-10 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            | --github  |
      | cx-scan-11 | CUSTOM      | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            | --github  |
      | cx-scan-12 | CUSTOM      | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            | --github  |
      | cx-scan-13 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            | --github  |
      | cx-scan-14 | CUSTOM      | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            | --github  |
      | cx-scan-15 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            | --github  |
      | cx-scan-16 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            | --github  |


      | cx-scan-17 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            | --gitlab  |
      | cx-scan-18 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            | --gitlab  |
      | cx-scan-19 | CUSTOM      | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            | --gitlab  |
      | cx-scan-20 | CUSTOM      | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            | --gitlab  |
      | cx-scan-21 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            | --gitlab  |
      | cx-scan-22 | CUSTOM      | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            | --gitlab  |
      | cx-scan-22 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            | --gitlab  |
      | cx-scan-23 | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            | --gitlab  |

      | cx-scan-1  | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            | --ado     |
      | cx-scan-2  | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 2            | --ado     |
      | cx-scan-2  | CUSTOM      | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 1            | --ado     |
      | cx-scan-2  | CUSTOM      | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0         | 0            | --ado     |
      # the following case: when at least one MEDIUM issue is found, the execution should be ended with error
      # (exit code = 10). All issues should be created (in this case two issues).
      | cx-scan-2  | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 2            | --ado     |
      # this next one fails as it as expected (exit code = 10) and just one issue is created, because the MEDIUM
      # vulnerability was found and, since that a MEDIUM is not eligible to create an issue (it is not between
      # filter-severity values), the execution is finished in error because of the thresholdsSeverity validation
      # (MEDIUM is defined as 1 on thresholds severity values).
      ### summing up: MEDIUM vulnerabilities does not create an issue, but when it is found the process FAIL (exit code = 10) ###
      | cx-scan-2  | CUSTOM      | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 10        | 1            | --ado     |
      | cx-scan-2  | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 10        | 2            | --ado     |
      | cx-scan-2  | CUSTOM      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 0         | 2            | --ado     |

  Scenario Outline: test web runner
    Given mock services "<scan-tag>" "<filter-severity>" "<thresholds-severity>"
    When I am using these parameters <project-id> <namespace> <assignee> <bug-tracker-project> <repo-name>
     And I do a request to bug tracker <bug-tracker> using this scan tag <scan-tag> and this request token <request-token>
    Then the status code should be equals to <status-code>
     And check how many create issue <create-issue> <bug-tracker>

    Examples:
      | scan-tag   | bug-tracker | filter-severity      | thresholds-severity              | create-issue | request-token | status-code | project-id | namespace | assignee         | bug-tracker-project | repo-name |
      # the firsts tests will check if all validations (required parameters and type validations) are working
      | cx-scan-1  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | INVALID       | 403         |            | ns        | email@server.com | btProject           | reponame  |
      | cx-scan-1  | " "         | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | xxxx          | 400         |            | ns        | email@server.com | btProject           | reponame  |
      | cx-scan-1  | INVALID     | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | xxxx          | 400         |            | ns        | email@server.com | btProject           | reponame  |
      | " "        | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | xxxx          | 400         |            | ns        | email@server.com | btProject           | reponame  |
      # bugtracker tests:
      | cx-scan-1  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 2            | xxxx          | 200         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-2  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 2            | xxxx          | 200         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-3  | jira        | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 1            | xxxx          | 200         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-4  | jira        | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | xxxx          | 200         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-5  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 2            | xxxx          | 500         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-6  | jira        | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 1            | xxxx          | 500         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-7  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 2            | xxxx          | 500         |            | ns        | email@server.com |                     | reponame  |
      | cx-scan-8  | jira        | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 2            | xxxx          | 200         |            | ns        | email@server.com |                     | reponame  |

      | cx-scan-9  | github      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 2            | xxxx          | 200         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-10 | github      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 2            | xxxx          | 200         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-11 | github      | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 1            | xxxx          | 200         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-12 | github      | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | xxxx          | 200         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-13 | github      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 2            | xxxx          | 500         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-14 | github      | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 1            | xxxx          | 500         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-15 | github      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 2            | xxxx          | 500         | 1234       | ns        | email@server.com |                     | reponame  |
      | cx-scan-16 | github      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 2            | xxxx          | 200         | 1234       | ns        | email@server.com |                     | reponame  |

      | cx-scan-17 | gitlab      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 2            | xxxx          | 200         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-18 | gitlab      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 2            | xxxx          | 200         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-19 | gitlab      | HIGH,MEDIUM          | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 1            | xxxx          | 200         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-20 | gitlab      | HIGH                 | HIGH=-1,MEDIUM=-1,LOW=-1,INFO=-1 | 0            | xxxx          | 200         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-21 | gitlab      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 2            | xxxx          | 500         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-22 | gitlab      | HIGH,LOW,INFO        | HIGH=-1,MEDIUM=1,LOW=-1,INFO=-1  | 1            | xxxx          | 500         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-22 | gitlab      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,MEDIUM=1,LOW=1,INFO=-1   | 2            | xxxx          | 500         | 1234       | ns        | email@server.com |                     |           |
      | cx-scan-23 | gitlab      | HIGH,MEDIUM,LOW,INFO | HIGH=-1,INFO=-1                  | 2            | xxxx          | 200         | 1234       | ns        | email@server.com |                     |           |

