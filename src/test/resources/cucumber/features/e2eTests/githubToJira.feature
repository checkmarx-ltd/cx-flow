@EndToEnd
@Integration
Feature: Cxflow end-2-end tests

  Scenario: Check cxflow end-2-end SAST flow between GitHub webhook and JIRA
    Given source is GitHub
    And target is Jira
    And CxFlow is running as a service
    And webhook is configured for push event
    When pushing a change
    Then target issues are updated