@ThresholdsFeature
Feature: CxFlow should fail builds and pull requests if the number of findings with certain severity is above threshold

  Scenario Outline: CxFlow should approve or fail GitHub pull request, depending on whether threshold is exceeded
    Given threshold for findings of "high" severity is "<high threshold>"
    And threshold for findings of "medium" severity is "<medium threshold>"
    And threshold for findings of "low" severity is "<low threshold>"
    When GitHub notifies CxFlow that a pull request was created
    And <high findings> of "high" severity are found
    And <medium findings> of "medium" severity are found
    And <low findings> of "low" severity are found
    Then CxFlow "<approves or fails>" the pull request

    Examples:
      | high threshold | medium threshold | low threshold | high findings | medium findings | low findings | approves or fails |
      | 3              | 8                | 15            | 5             | 6               | 8            | fails             |
      | 3              | 8                | 15            | 2             | 10              | 8            | fails             |
      | 3              | 8                | 15            | 2             | 6               | 20           | fails             |
      | 3              | 8                | 15            | 3             | 8               | 15           | approves          |
      | 0              | 0                | 0             | 0             | 0               | 1            | fails             |
      | <missing>      | 8                | 15            | 20            | 6               | 8            | approves          |
      | 3              | <null>           | 15            | 2             | 6               | 8            | approves          |
      | 3              | 8                | <empty>       | 2             | 6               | 10           | approves          |
      # <missing> - threshold value is not specified in config
      # <empty> - threshold value is an empty string

  @Skip
  Scenario: Invalid threshold values
    CxFlow should throw errors if an invalid value is specified for some of the thresholds
