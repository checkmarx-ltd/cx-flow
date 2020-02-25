@ThresholdsFeature
Feature: CxFlow should fail builds and pull requests if the number of findings with certain severity is above threshold

  Scenario Outline: CxFlow should approve or fail GitHub pull request, depending on whether threshold is exceeded
  GitHub notifies CxFlow that a pull request was created. CxFlow then executes a SAST scan.
    Given threshold for findings of "high" severity is "<high threshold>"
    And threshold for findings of "medium" severity is "<medium threshold>"
    And threshold for findings of "low" severity is "<low threshold>"
    When SAST detects <high count> findings of "high" severity
    And <medium count> findings of "medium" severity
    And <low count> findings of "low" severity
    Then CxFlow "<approves or fails>" the pull request

    Examples:
      | high threshold | medium threshold | low threshold | high count | medium count | low count | approves or fails |
      | 3              | 8                | 15            | 5          | 6            | 8         | fails             |
      | 3              | 8                | 15            | 2          | 10           | 8         | fails             |
      | 3              | 8                | 15            | 2          | 6            | 20        | fails             |
      | 3              | 8                | 15            | 3          | 8            | 15        | approves          |
      | 3              | 8                | 15            | 0          | 0            | 0         | approves          |
      | 0              | 0                | 0             | 0          | 0            | 1         | fails             |
      | <omitted>      | 8                | 15            | 20         | 6            | 8         | approves          |
      | 3              | <omitted>        | 15            | 2          | 6            | 8         | approves          |
      | 3              | 8                | <omitted>     | 2          | 6            | 10        | approves          |
      | <omitted>      | <omitted>        | <omitted>     | 2          | 6            | 10        | approves          |
      # <omitted> - threshold value is not specified in config, or specified with an empty value, or with a null value.
      # All of these cases will result in the same config object.

  @Skip
  Scenario: the whole 'thresholds' section is omitted

  @Skip
  Scenario: Invalid threshold values
  CxFlow should throw errors if an invalid value is specified for some of the thresholds
