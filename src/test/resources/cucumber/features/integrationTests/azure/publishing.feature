@AzureDevOps
@PublishProcessing
@Integration
Feature: Parsing SAST report and publishing items to Azure DevOps

  Background: Issue tracker is Azure DevOps

  @Create_issue
  Scenario Outline: Creating a single issue after merging several findings
    Given Azure DevOps doesn't contain any issues
    And SAST report contains <finding count> findings with the same vulnerability type and in the same file, and not marked as false positive
    When publishing the report
    Then Azure DevOps contains <issue count> issues
    Examples:
      | finding count | issue count |
      | 0             | 0           |
      | 1             | 1           |
      | 2             | 1           |

  @Create_issue
  Scenario: Creating separate issues from several findings
    # There is no need to check the cases with 0 or 1 findings: they are already covered in the previous scenario.
    Given Azure DevOps doesn't contain any issues
    And SAST report contains 2 findings, each with a different vulnerability type and filename, and not marked as false positive
    When publishing the report
    Then Azure DevOps contains 2 issues

  @Create_issue
  @NegativeTest
  Scenario: Don't create a new issue if all findings are false positive
    Given Azure DevOps doesn't contain any issues
    And SAST report contains 3 findings, all marked as false positive
    When publishing the report
    Then Azure DevOps doesn't contain any issues

  @Update_issue
  Scenario Outline: Updating an existing issue
    Given Azure DevOps initially contains 1 open issue with title: "<title>" and description containing link: "<link1>"
    And SAST report contains 1 finding with vulnerability type "<vulnerability>", filename "<filename>", link "<link2>", and not marked as false positive
    When publishing the report
    Then Azure DevOps contains 1 open issue with title: "<title>" and description containing link: "<link2>"

    Examples:
      | title                                                  | vulnerability             | filename       | link1              | link2                                                                       |
      | CX Reflected_XSS_All_Clients @ DOS_Login.java [master] | Reflected_XSS_All_Clients | DOS_Login.java | http://initial.url | http://CX-FLOW-CLEAN/CxWebClient/ViewerMain.aspx?scanid=1000026&projectid=6 |

  @Close_issue
  Scenario Outline: Closing an existing issue if its finding no longer exists in SAST report
    Given Azure DevOps initially contains 1 open issue with title: "<title>"
    And SAST report contains 1 finding with vulnerability type "Reflected_XSS_All_Clients" and filename "DOS_Login.java", not marked as false positive
    When publishing the report
    Then Azure DevOps contains 2 issues
    And an issue with the title "<title>" is in "Closed" state
    Examples:
      | title                                        |
      | CX Mega_Vulnerability @ Gotcha.java [master] |


  @Close_issue
  Scenario Outline: Closing an existing issue if all its findings are false positive
    Given Azure DevOps initially contains 1 open issue with title: "<title>"
    And SAST report contains 2 findings with vulnerability type "<vulnerability>", filename "<filename>" and marked as false positive
    When publishing the report
    Then Azure DevOps contains 1 issue with the title "<title>" and "Closed" state
    Examples:
      | title                                     | vulnerability | filename       |
      | CX SQL_Injection @ TestFile.java [master] | SQL_Injection | DOS_Login.java |

  @Close_issue
  Scenario Outline: Closing only relevant issues
  Make sure CxFlow only closes issues that do not appear in SAST report. Issues that do appear in SAST report
  should be left open.
    Given Azure DevOps initially contains 2 open issues with titles "<title1>" and "<title2>"
    And SAST report contains 1 finding with vulnerability type "<vulnerability1>" and filename "<filename1>", not marked as false positive
    When publishing the report
    Then Azure DevOps contains 2 issues
    And one of the issues has the title: "<title1>" and is in "Active" state
    And the other issue has the title: "<title2>" and is in "Closed" state
    Examples:
      | title1                                                 | title2                                    | vulnerability1            | filename1      |
      | CX Reflected_XSS_All_Clients @ DOS_Login.java [master] | CX SQL_Injection @ TestFile.java [master] | Reflected_XSS_All_Clients | DOS_Login.java |

  @Skip
  Scenario: Reopening closed issues
  CxFlow should reopen previously closed issue if a corresponding finding is present in SAST report.

  @Skip
  @NegativeTest
  @Error_Handling
  @UnReachableService
  Scenario: Azure DevOps is unreachable
    Given invalid Azure DevOps URL is provided in configuration
    When publishing a SAST report
    Then CxFlow should throw "org.springframework.web.client.ResourceAccessException"