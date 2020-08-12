@CxConfigFeature @IntegrationTest
Feature: CxFlow should read configuration from cx.config file in the root of repository

  @Thresholds
  Scenario Outline: CxFlow should approve or fail GitHub pull request, depending on whether threshold is exceeded in cx.config
  GitHub notifies CxFlow that a pull request was created. CxFlow then executes a SAST scan.
    Given github branch is "<branch>" and thresholds section is not set application.yml
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
       ## branch test4: Thresholds section doesn't exist in cx.config in the root of the branch
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


  @Filters
  Scenario Outline: CxFlow will show results as per filters section cx.config
    Given application.xml contains filters section with filter type "<app_filter>"
    And github branch is "<branch>" with cx.config
    Then CxFlow will return results as per the filter in cx.config

    Examples:
      | branch | app_filter            | 
       ## branch test7: Filter section contains:  filter severity: High, Medium
      | test7  | severity,category     | 
       ## branch test8: Filter section contains:  filter cwe:    "79", "89"
      | test8  |                       |
      ## filter category:   "XSS_Reflected", "SQL_Injection"
      | test9  |                       |
      | test7  | severity,cwe          | 
       ## branch test8: Filter section contains:  filter cwe:    "79", "89"
      | test8  | severity,category     |
       ## branch test10: Filter section contains:
       ## filter cwe:    "79", "89"
       ## filter category:   "XSS_Reflected", "SQL_Injection"
      | test10 |                       |
       ## branch test9: Filter section contains:
       ## filter filter severity: High, Medium
       ## filter category:   "XSS_Reflected", "SQL_Injection"
      | test11 | severity,category,cwe |

  @Invalid
  Scenario Outline: CxFlow will fail when cx.config format is invalid
    Given github branch is "<branch>" with invalid cx.config
    Then CxFlow will ignore the cxconfig and take all the values from application.yml

    Examples:
      | branch |
      # branch12 contain cxconfig with invalid field
      | test12 |

  @Sca_cx_config
  Scenario: Cx-Flow will set sca configuration sections according to the cx.config file
    Given cx-flow has a scan request
    When target repo contains a configuration file
    Then cx-flow configurations properties are getting overridden with the following parameters:
      | vulnerabilityScanners |
      | thresholdsSeverity    |
      | thresholdsScore       |
      | filterSeverity        |
      | filterScore           |

  Scenario Outline: CxFlow should use config-as-code from a correct branch
    Given use-config-as-code-from-default-branch property in application.yml is set to "<use default>"
    And GitHub repo default branch is "master"
    When GitHub notifies CxFlow that a pull request was created for the "test1" branch
    Then CxFlow should get config-as-code from the "<branch>" branch
    Examples:
      | use default | branch |
      | true        | master |
      | false       | test1  |