@Integration
@PublishProcessing
@JiraIntegrationTests
Feature: parse, and then publish processing given SAST XML results, findings should open an issue in Jira.


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


    @Create_issue
  Scenario Outline: publish new issue to JIRA which contains only one vulnerability type in one source file
    Given target is JIRA
    And   results contains <Number_Of> findings with the same type in one source file
    When  publishing results to JIRA
    Then  verify <Total_Of> new issues got created
    And verify <Number_Of> findings in body
    And issue status will be present in the body  

    Examples:
      | Number_Of | Total_Of |
      | 0         | 0        |
      | 1         | 1        |
      | 2         | 1        |


    @Create_issue
  Scenario Outline: publish new issues to JIRA and filtered by a single severity - ("High" in this case).
                    Every finding is in different file, so for wach finding we should see an issue created in JIRA
    Given target is JIRA
    And   there are <Number_Of_Total> findings from which <Number_Of> results match the filter
    When  publishing results to JIRA
    Then  verify <Number_Of> new issues got created

    Examples:
      | Number_Of_Total | Number_Of |
      | 0               | 0         |
      | 1               | 1         |
      | 3               | 1         |
      | 10              | 5         |
      | 10              | 10        |


    @Create_issue 
  Scenario Outline: sanity of publishing new issues to JIRA, with filter that may contain multiple severities.
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


  @Update_issue
  Scenario: updating an existing JIRA issue during publish
    Given target is JIRA
    And there is an existing issue
    And SAST results contain 1 finding, with the same vulnerability type and filename
    When publishing results to JIRA
    Then JIRA still contains 1 issue
    And issue ID hasn't changed
    And issue's updated field is set to a more recent timestamp
    And issue has the same vulnerability type and filename

  @Update_issue @status
  Scenario: updating an existing JIRA issue during publish
    Given target is JIRA
    And there is an existing issue
    And there is status change in the issue
    And SAST results contain 1 finding, with the same vulnerability type and filename
    When publishing results to JIRA
    Then JIRA still contains 1 issue
    And issue ID hasn't changed
    And issue's updated field is set to a more recent timestamp
    And issue has the same vulnerability type and filename
    And the updated issue has the new status field in the body
    And the updated issue has a recommended fix link

  @Update_issue @NegativeTest
    # Change scenario name
  Scenario: publish updated issue to JIRA with different parameters
    # Note - to update an issue, the vulnerability & filename fields must match
    Given target is JIRA
    And   there is an existing issue
    When  publishing same issue with different parameters
    Then  original issues is updated both with 'last updated' value and with new body content


  @Close_issue
  Scenario: publish closed issue to JIRA
    Given target is JIRA
    And   there is an existing issue
    When  all issue's findings are false-positive
    Then  the issue should be closed



  @Close_issue
  Scenario: two issues exists in jira. SAST findings contains only one of them, and after publish one should be closed.
    Given target is JIRA
    And there are two existing issues
    And SAST result contains only one of the findings
    When publishing results to JIRA
    Then there should be one closed and one open issue


  @Integration @Negative_test @Error_Handling
  Scenario: Perform a GET REST call to unreachable JIRA environment
    Given target is JIRA
    And JIRA is configured with invalid URL
    When preparing a getIssues call to deliver
    Then the call execution should throw a JiraClientRunTimeException since JIRA is un-accessible


  @Skip @Integration @Negative_test @Error_Handling
  Scenario: Fail to publish tickets to JIRA environment
    Given target is JIRA
    And Cx-Flow is configured with invalid project key
    When preparing results to deliver
    Then the call execution should throw a JiraClientRunTimeException since an error occurred when published new tickets

  @Analytics @Jira_Analytics @Jira_Analytics_Open_Issue_Command_Line
  Scenario: Open a new ticket in Jira via command line and validate matching data in the analytics json file
    Given bug tracker is Jira
    When opening a new Jira issue via the command line
    Then a matching ticket creation data should be recorded in the analytics json file

  @Analytics @Jira_Analytics @Jira_Analytics_Update_Issue_Command_Line
  Scenario: Update a current ticket in Jira via command line and validate matching data in the analytics json file
    Given bug tracker is Jira
    When updating a new Jira issue via the command line
    Then a matching ticket updating data should be recorded in the analytics json file



