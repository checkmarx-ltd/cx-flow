@AzureDevOps
@PublishProcessing
@Integration
@Skip
Feature: Parsing SAST report and publishing items to Azure DevOps
  Background: Issue tracker is Azure DevOps

  @Create_issue
  Scenario Outline: Creating a single issue after merging several findings
    Given SAST report contains <finding count> findings with the same vulnerability type and in the same file
    When publishing the report
    Then <issue count> issues are created
    Examples:
      | finding count | issue count |
      | 0             | 0           |
      | 1             | 1           |
      | 3             | 1           |

  @Create_issue
  Scenario Outline: Creating separate issues from several findings
    Given SAST report contains <number of> findings, each with a different vulnerability type
    When publishing the report
    Then <number of> issues are created
    Examples:
      | number of |
      | 0         |
      | 1         |
      | 3         |

  @Update_issue
  Scenario: Updating an existing issue
    Given Azure DevOps contains 1 open issue with vulnerability type T, filename F, description D1 and 'last updated' time U1
    And SAST report contains 1 finding with vulnerability type T, filename F, description D2, and not marked as false positive
    When publishing the report
    Then Azure DevOps contains 1 open issue with vulnerability type T, filename F, description D2 and 'last updated' time U2
    And U2 is greater than U1

  @Close_issue
  Scenario: Closing an existing issue if its finding no longer exists
    Given Azure DevOps contains 1 open issue with vulnerability type T and filename F
    And SAST report doesn't contain any findings with vulnerability type T and filename F
    When publishing the report
    Then Azure DevOps contains 1 closed issue with vulnerability type T and filename F

  @Close_issue
  Scenario: Closing an existing issue if all its findings are false positive
    Given Azure DevOps contains 1 open issue with vulnerability type T and filename F
    And SAST report contains 2 findings with vulnerability type T, filename F and marked as false positive
    When publishing the report
    Then Azure DevOps contains 1 closed issue with vulnerability type T and filename F

  @Close_issue
  Scenario: Closing only relevant issues
    # Closing an issue if it doesn't appear in CxFlow report.
    Given Azure DevOps contains 2 issues with filenames F1 and F2
    And SAST report contains 1 finding with filename F1, not marked as false positive
    When publishing the report
    Then Azure DevOps contains 1 open issue with filename F1
    And 1 closed issue with filename F2

  @NegativeTest
  @Error_Handling
  @UnReachableService
  Scenario: Azure DevOps is unreachable
    Given invalid Azure DevOps URL is provided in configuration
    When publishing a SAST report
    # TODO: which exception?
    Then CxFlow should throw an exception