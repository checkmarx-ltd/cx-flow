@EndToEnd
@Integration
Feature: Cxflow generic end-2-end tests
  Scenario Outline: Check cxflow end-2-end SAST flow between <repository> webhook and <bug-tracker>
    Given repository is <repository>
    And bug-tracker is <bug-tracker>
    And CxFlow is running as a service
    And webhook is configured for push event
    When pushing a change
    Then bug-tracker issues are updated
    Examples:
    | repository  | bug-tracker |
    | GitHub      | JIRA        |
    | ADO         | JIRA        |