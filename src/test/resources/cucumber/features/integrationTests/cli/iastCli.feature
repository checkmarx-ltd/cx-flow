@IastFeature @IntegrationTest
Feature: Check integration with iast - stop scan and create jira issue by CLI/Jenikns
And command line example: ”java -jar cx-flow-1.6.18.jar --spring.config.location=application.yml --iast --bug-tracker="jira" --assignee="email@mail.com" --scan-tag="cx-scan-tag-1" --jira.url=https://xxxx.atlassian.net --jira.username=email@mail.com --jira.token=token --jira.project=BCB --iast.url="http://localhost" --iast.manager-port=8380 --iast.username="username" --iast.password="password" --iast.update-token-seconds=150 --jira.issue-type="Task"”



  Scenario Outline: Get data from IAST and create Jira issues
    Given mock services "<scanTag>" "<filter-severity>"
    When running iast cli "<scanTag>"
    Then check how many create issue "<create jira issue>"

    Examples:
      | scanTag     | create jira issue | filter-severity         |
      | cx-scan-1   | 2                 | HIGH,MEDIUM,LOW,INFO    |
      | cx-scan-2   | 2                 | HIGH,MEDIUM,LOW,INFO    |
      | cx-scan-2   | 1                 | HIGH,MEDIUM             |
      | cx-scan-2   | 0                 | HIGH                    |
