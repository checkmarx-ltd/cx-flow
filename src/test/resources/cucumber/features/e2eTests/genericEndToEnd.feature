@EndToEnd
@Integration
Feature: Cxflow generic end-2-end tests

  Scenario Outline: Check cxflow end-2-end <scan-engine> flow between <repository> webhook and <bug-tracker>
    Given CxFlow is running as a service
    And Scan engine is <scan-engine>
    And repository is <repository>
    And bug-tracker is <bug-tracker>
    And webhook is configured for push event
    When pushing a change
    Then bug-tracker issues are updated
    Examples:
      | scan-engine | repository | bug-tracker |
      | sast        | GitHub     | JIRA        |
      | sast        | ADO        | JIRA        |
      | sca         | GitHub     | JIRA        |

  Scenario Outline: Check cxflow pull-request end-2-end <scan-engine> of <repository>
    Given CxFlow is running as a service
    And repository is <repository>
    And Scan engine is <scan-engine>
    And webhook is configured for pull-request
    When creating pull-request
    Then pull-request is updated
    Examples:
      | repository | scan-engine |
      | GitHub     | sca         |
