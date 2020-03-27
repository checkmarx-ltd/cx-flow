@Jira
@Integration
@JiraLoadTests
Feature: Load testing for JIRA

  Scenario Outline: publish SAST results that should create many issues in Jira and make sure that they are created in less than the time given
    Given SAST results with that should result in <Num_Of_Issues> JIRS issues
    When results are parsed and published <samples> times, and time is recorded for each sample
    Then <Percents_Threshold> percents of publish request should take less than <Primary_Duration_Threshold> seconds
    And the other tests should take less than <Secondary_Duration_Threshold> seconds

    Examples:
    | Num_Of_Issues  | Percents_Threshold  | Primary_Duration_Threshold  |   Secondary_Duration_Threshold  | samples |
    | 385            | 95                  | 230                         | 253                             | 1       |
    | 300            | 95                  | 180                         | 198                             | 1       |
    | 200            | 95                  | 120                         | 134                             | 1       |
    | 100            | 95                  | 60                          | 66                              | 1       |