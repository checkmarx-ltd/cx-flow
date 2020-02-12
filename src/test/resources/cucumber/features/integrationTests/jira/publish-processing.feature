@Jira
@Integration
@PublishProcessing
Feature: parse, and then publish processing
  given SAST XML results, calling flowService, to parse and publish results. findings should open an issue in Jira.


  Scenario Outline: publish new issues to JIRA, one new issue is getting created per scan vulnerability type
    Given target is JIRA
    And   results contain <Number_Of> findings each having a different vulnerability type in one source file
    When  publishing results to JIRA
    Then  verify <Number_Of> new issues got created

    Examples:
      | Number_Of |
      | 0         |
      | 1         |
      | 3         |

  @Integration
  @Create_issue
  Scenario Outline: publish new issue to JIRA which contains only one vulnerability type
    Given target is JIRA
    And   results contains <Number_Of> findings with the same type
    When  publishing results to JIRA
    Then  verify <Total_Of> new issues got created
    And verify <Number_Of> findings in body

    Examples:
      | Number_Of | Total_Of |
      | 0         | 0        |
      | 1         | 1        |
      | 2         | 1        |

  @Integration
  @Create_issue
  Scenario Outline: publish new issues to JIRA and filtered by a single severity
    Given target is JIRA
    And   there are <Number_Of_Total> results from which <Number_Of> results match the filter
    When  publishing results to JIRA
    Then  verify <Number_Of> new issues got created

    Examples:
      | Number_Of_Total | Number_Of |
      | 0               | 0         |
      | 1               | 1         |
      | 3               | 1         |
      | 10              | 5         |
      | 10              | 10        |

  @Integration
  @Create_issue
  Scenario Outline: sanity of publishing new issues to JIRA
    Given target is JIRA
    And   filter-severity is <Filter_Severity>
    And   using sanity findings
    When  publishing results to JIRA
    Then  verify results contains <High_Vulnerabilities_Expected>, <Medium_Vulnerabilities_Expected>, <Low_Vulnerabilities_Expected>, <Info_Vulnerabilities_Expected> for severities <Filter_Severity>
    Examples:
      | Filter_Severity | High_Vulnerabilities_Expected | Medium_Vulnerabilities_Expected | Low_Vulnerabilities_Expected  | Info_Vulnerabilities_Expected  |
      | High            | 2                             | 0                                 | 0                           | 0                              |
      | High,Medium    | 2                             | 10                                | 0                           | 0                              |
      | High,Low       | 2                             | 0                                 | 19                          | 0                              |

  @Integration
  @Update_issue
  Scenario: updating an existing JIRA issue during publish
    Given target is JIRA
    And there is an existing issue
    And SAST results contain 1 finding
    When publishing results to JIRA
    Then JIRA still contains 1 issue
    And issue ID hasn't changed
    And issue's updated field is set to a more recent timestamp
    And issue has the same vulnerability type and filename

  @Integration
  @Update_issue @Negative_test
  Scenario: publish updated issue to JIRA with different parameters
    # Note - to update an issue, the vulnerability & filename fields must match
    Given target is JIRA
    And   there is an existing issue
    When  publishing same issue with different parameters
    Then  original issues is updated both with 'last updated' value and with new body content

    @Integration
    @Close_issue
    Scenario: publish closed issue to JIRA
      Given target is JIRA
      And   there is an existing issue
      When  all issue's findings are false-positive
      Then  the issue should be closed

  @Integration @Negative_test @Error_Handling @UnReachableService
  Scenario: Perform a GET REST call to unreachable JIRA environment
    Given target is JIRA
    And JIRA is configured with invalid URL
    When preparing a getIssues call to deliver
    Then the call execution should throw a JiraClientRunTimeException

  @Integration
  @Close_issue
  Scenario: two issues exists in jira. SAST findings contains only one of them, and after publish one should be closed.
    Given target is JIRA
    And there are two existing issues
    And SAST result contains only one of the findings
    When publishing results to JIRA
    Then there should be one closed and one open issue


