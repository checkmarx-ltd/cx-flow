@Jira
@Integration
@JiraAutoConfig
@JiraIntegrationTests
Feature: When there are no closed or open statuses for Jira in YML, CxFlow should auto config it from Jira

  Scenario: There are no Jira closed and open statuses defined in yml. CxFlow Should get those definitions from Jira.
    #Given There is no Jira closed and open statuses defined in yml
    Then we should have open and closed statuses in jira properties bean

