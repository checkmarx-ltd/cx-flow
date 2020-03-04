@ThresholdsFeature
Feature: CxFlow should fail builds and pull requests if the number of findings with certain severity is above threshold

  Scenario Outline: CxFlow should approve or fail GitHub pull request, depending on whether threshold is exceeded
  GitHub notifies CxFlow that a pull request was created. CxFlow then executes a SAST scan.
    Given threshold for findings of "High" severity is "<high threshold>"
    And threshold for findings of "Medium" severity is "<medium threshold>"
    And threshold for findings of "Low" severity is "<low threshold>"
    And no severity filter is specified
    When SAST detects <high count> findings of "High" severity
    And <medium count> findings of "Medium" severity
    And <low count> findings of "Low" severity
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
      | <omitted>      | <omitted>        | <omitted>     | 0          | 6            | 10        | fails             |
      | <omitted>      | <omitted>        | <omitted>     | 0          | 0            | 0         | approves          |
      # <omitted> - threshold value is not specified in config, or specified with an empty value, or with a null value.
      # All of these cases will result in the same config object.


  Scenario Outline: Thresholds section is omitted
  If the 'thresholds' section is omitted, CxFlow should fail a pull request if there is at least 1 finding.
    Given the whole 'thresholds' section is omitted from config
    And no severity filter is specified
    When SAST detects <high count> findings of "High" severity
    And <medium count> findings of "Medium" severity
    And <low count> findings of "Low" severity
    Then CxFlow "<approves or fails>" the pull request

    Examples:
      | high count | medium count | low count | approves or fails |
      | 0          | 0            | 0         | approves          |
      | 2          | 0            | 0         | fails             |
      | 0          | 4            | 0         | fails             |
      | 0          | 0            | 1         | fails             |
      | 2          | 4            | 7         | fails             |

  Scenario: Combining filters with threshold checks
  CxFlow should check thresholds after the execution of filters.
    Given threshold for findings of "High" severity is "3"
    And threshold for findings of "Medium" severity is "5"
    And threshold for findings of "Low" severity is "10"
    And severity filter is set to "High"
    When SAST detects 2 findings of "High" severity
    And 14 findings of "Medium" severity
    And 23 findings of "Low" severity
    Then CxFlow "approves" the pull request