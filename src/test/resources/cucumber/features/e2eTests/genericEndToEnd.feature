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


  @Skip
  Scenario Outline: Check cxflow pull-request end-2-end <scan-engine> of <repository>
    Given repository is <repository>
    And Scan engine is <scan-engine>
    And CxFlow is running as a service
    And webhook is configured for pull-request
    When creating pull-request
    Then pull-request is updated
    Examples:
    | repository  | scan-engine |
    | GitHub      | SCA         |
