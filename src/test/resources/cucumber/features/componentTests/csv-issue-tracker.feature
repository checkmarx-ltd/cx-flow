@CsvIssueTracker @Skip
Feature: Csv issue tracker flow

  Scenario Outline: Csv issue tracker
    Given Sast results having the following findings: <findings>
    When publish findings using Csv issue tracker
    Then Csv result generated with <number of issues> issue(s)
    Examples:
      | findings                                                             | number of issues |
      | 2 findings with the same vulnerability type and in the same file     | 1                |
      | 2 findings with the same vulnerability type and in different files   | 2                |
      | 2 findings with different vulnerability types and in the same file   | 2                |
      | 2 findings with different vulnerability types and in different files | 2                |