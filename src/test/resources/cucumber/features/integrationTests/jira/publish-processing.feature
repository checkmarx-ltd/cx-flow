@Jira @Integration
Feature: Check integration tests of publish processing

  @Create_issue
  Scenario Outline: publish new issues to JIRA, one new issue is getting created per scan vulnerability type
    Given target is JIRA
    And   results contains <Number_of> vulnerabilities each having a different vulnerability type
    When  publishing results to JIRA
    Then  verify <Number_Of> new issues got created

    Examples:
      | Number_Of |
      | 0         |
      | 1         |
      | 3         |
      | ALL       |


  @Create_issue
  Scenario Outline: publish new issue to JIRA which contains only one vulnerability type
    Given target is JIRA
    And   results contains <Number_Of> vulnerabilities with the same type
    When  publishing results to JIRA
    Then  verify results contains <Total_Of> new issue with <Number_Of> vulnerabilities in body

    Examples:
      | Number_Of | Total_Of |
      | 0         | 0        |
      | 1         | 1        |
      | 3         | 1        |


  @Create_issue
  Scenario Outline: publish new issues to JIRA and filtered by a single severity
    Given target is JIRA
    And   there are <Number_Of_Total> results from which <Number_Of> results match the filter
    When  publishing results to JIRA
    Then  there are <Number_Of> new vulnerabilities

    Examples:
      | Number_Of_Total | Number_Of |
      | 0               | 0         |
      | 1               | 1         |
      | 3               | 1         |
      | 10              | 5         |
      | 10              | 10        |


  @Create_issue
  Scenario Outline: sanity of publishing new issues to JIRA
    Given target is JIRA
    And   filter-severity is <Filter_Severity>
    And   results contains "<High_Vulnerabilities_In_Scan>" of <"High_Vulnerabilities_Types">
    And   results contains "<Medium_Vulnerabilities_In_Scan>" of <"Medium_Vulnerabilities_Types">
    And   results contains "<Low_Vulnerabilities_In_Scan>" of <"Low_Vulnerabilities_Types">
    And   results contains "<Info_Vulnerabilities_In_Scan>" of <"Info_Vulnerabilities_Types">
    When  publishing results to JIRA
    Then  verify results contains "<High_Vulnerabilities_Expected>" of <"High_Vulnerabilities_Types">
    And   verify results contains "<Medium_Vulnerabilities_Expected>" of <"Medium_Vulnerabilities_Types">
    And   verify results contains "<Low_Vulnerabilities_Expected>" of <"Low_Vulnerabilities_Types">
    And   verify results contains "<Info_Vulnerabilities_Expected>" of <"Info_Vulnerabilities_Types">

    Examples:
      | Filter_Severity | High_Vulnerabilities_In_Scan | "High_Vulnerabilities_Types" | Medium_Vulnerabilities_In_Scan | "Medium_Vulnerabilities_Types" | Low_Vulnerabilities_In_Scan | "Low_Vulnerabilities_Types" | Info_Vulnerabilities_In_Scan | "Info_Vulnerabilities_Types" | High_Vulnerabilities_Expected | Medium_Vulnerabilities_Expected | Low_Vulnerabilities_Expected | Info_Vulnerabilities_Expected |
      | [High]          | 10                           | 2                            | 9                              | 3                              | 4                           | 2                           | 20                           | 1                            | 10                            | 0                               | 0                            | 0                             |
      | [High,Medium]   | 10                           | 2                            | 9                              | 3                              | 4                           | 2                           | 20                           | 1                            | 10                            | 9                               | 0                            | 0                             |
      | [High,Low]      | 10                           | 2                            | 9                              | 3                              | 4                           | 2                           | 20                           | 1                            | 10                            | 0                               | 4                            | 0                             |


  @Update_issue
  Scenario: publish updated issue to JIRA
    # Note - to update an issue, the vulnerability & filename fields must match
    Given target is JIRA
    And   there is an existing issue
    When  publishing same issue with different parameters
    Then  original issues is updated both with 'last updated' value and with new body content


  @Update_issue @Negative_test
  Scenario: publish updated issue to JIRA with missing parameters
    # Note - to update an issue, the vulnerability & filename fields must match
    Given target is JIRA
    And   there is an existing issue
    When  publishing same issue with missing parameters
    Then  original issues is updated both with 'last updated' value and with new body content

    @Close_issue
    Scenario: publish closed issue to JIRA
      Given target is JIRA
      And   there is an existing issue
      When  all issue's vulnerabilities are false-positive
      Then  the issue should be closed
