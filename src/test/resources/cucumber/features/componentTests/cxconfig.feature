@CxConfigFeature
Feature: CxFlow should read configuration from cx.config file in the root of repository

  @Thresholds 
  Scenario Outline: CxFlow should approve or fail GitHub pull request, depending on whether threshold is exceeded in cx.config
  GitHub notifies CxFlow that a pull request was created. CxFlow then executes a SAST scan.
    Given github branch is "<branch>" and threshods section is not set application.yml
    And SAST detects <high count> findings of "High" severity
    And <medium count> findings of "Medium" severity
    And <low count> findings of "Low" severity
    Then CxFlow "<approves or fails>" the pull request

    Examples:
      | branch | high count | medium count | low count | approves or fails |
       ## branch test1: Thresholds section contains:   High: 3  Medium: 8  Low: 15
      | test1  | 5          | 6            | 8         | fails             |
      | test1  | 2          | 10           | 8         | fails             |
      | test1  | 2          | 6            | 20        | fails             |
      | test1  | 3          | 8            | 15        | approves          |
      | test1  | 0          | 0            | 0         | approves          |
       ## branch test2: Thresholds section contains:    High: threshold not set  Medium: 8  Low: 15
      | test2  | 20         | 6            | 8         | approves          |
       ## branch test3: Thresholds section contains:    High: 3  Medium: threshold not set   Low: 15
      | test3  | 2          | 6            | 8         | approves          |
       ## branch test4: Thresholds section doesn't exist in cx.config in the root of the banch
       ## If the 'thresholds' section is omitted, CxFlow should fail a pull request if there is at least 1 finding.
      | test4  | 2          | 6            | 8         | fails             |
      | test4  | 0          | 0            | 0         | approves          |
       ## branch test5: cx.config doesn't exist in the root of the branch
       ## If the 'thresholds' section is omitted, CxFlow should fail a pull request if there is at least 1 finding.
      | test5  | 2          | 6            | 8         | fails             |
      | test5  | 0          | 0            | 0         | approves          |


  @Thresholds
  Scenario Outline: CxFlow should approve or fail GitHub pull request, depending on whether threshold is exceeded in cx.config
  GitHub notifies CxFlow that a pull request was created. CxFlow then executes a SAST scan.
    Given application.xml contains high thresholds "<app_high>" medium thresholds "<app_medium>" and low thresholds "<app_low>"
    And github branch is "<branch>" with cx.config
    And SAST detects <high count> findings of "High" severity
    And <medium count> findings of "Medium" severity
    And <low count> findings of "Low" severity
    Then CxFlow "<approves or fails>" the pull request and cx.config truncates the data in application.yml

    Examples:
      | branch | app_high | app_medium | app_low | high count | medium count | low count | approves or fails |
       ## branch test1: Thresholds section contains:   High: 3  Medium: 8  Low: 15
      | test1  | 6        | 7          | 10      | 5          | 6            | 8         | fails             |
      | test1  | 2        | 2          | 2       | 3          | 8            | 15        | approves          |
       ## branch test2: Thresholds section contains:    High: threshold not set  Medium: 8  Low: 15
      | test2  | 5        |            |         | 20         | 6            | 8         | approves          |
       ## branch test3: Thresholds section contains:    High: 3  Medium: threshold not set   Low: 15
      | test3  |          | 5          |         | 2          | 6            | 8         | approves          |
       ## branch test4: Thresholds section doesn't exist in cx.config in the root of the branch
      | test4  | 2        | 5          | 10      | 2          | 6            | 8         | fails             |
       ## branch test5: cx.config doesn't exist in the root of the branch
      | test5  | 2        | 5          | 10      | 2          | 6            | 8         | fails             |