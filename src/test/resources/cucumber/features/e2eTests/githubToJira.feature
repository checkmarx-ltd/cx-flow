Feature: Cxflow end-2-end tests

  Scenario: Check cxflow end-2-end SAST flow between GitHub webhook and JIRA
    Given source is Githb
    And target is Jira
    And CxFlow is running as a service
    And webhook is configured for push event
    When pushing a change
    Then scan is triggered on SAST
    And target issues are updated according to the scan results