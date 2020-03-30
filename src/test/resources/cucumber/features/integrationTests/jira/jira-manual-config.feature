@Jira
@Integration
@JiraManualConfig
@JiraIntegrationTests
Feature: When there are no closed or open statuses for Jira in YML, CxFlow should auto config it from Jira

  Scenario: There are values for jira closed and open statuses in yml, and so they should be used
    Then we should use te values from yml

