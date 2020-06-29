@ThresholdsFeature
@CxSCA
@ComponentTest
Feature: CxFlow should fail builds and pull requests if scan exeeds threshold

  Background: Definition of thresholds and findings
    Given the following thresholds:
      | threshold-name | threshold-for-high | threshold-for-medium | threshold-for-low |
      | N/A            | <omitted>          | <omitted>            | <omitted>         |
      | empty          | 0                  | 0                    | 0                 |
      | normal         | 7.0                | 8.0                  | 9.0               |
      | partial        | <omitted>          | 8.0                  | <omitted>         |
    And the following scan findings:
      | name        | max-score-high | max-score-medium | max-score-low |
      | exact       | 7.0            | 8.0              | 9.0           |
      | high-over   | 7.0-8.0        | under-8.0        | under-9.0     |
      | medium-over | under-7.0      | 8.0-9.0          | under-9.0     |
      | low-over    | under-7.0      | under-8.0        | over-9.0      |
      | only-low    | 0              | 0                | over-0        |
      | no-findings | 0              | 0                | 0             |

  Scenario Outline: CxFlow should fail pull request if threshold is exceeded
    PR-Webhook trigger notifies CxFlow that a pull request was created. CxFlow then executes a CxSCA scan.
    When threshold is cofigured to <threshold-to-use>
    And scan finding is <scan-findings>; using CxSca
    Then pull request should <pass-or-fail>

    Examples:
      | threshold-to-use | scan-findings | pass-or-fail |
      | normal           | exact         | pass         |
      | normal           | high-over     | fail         |
      | normal           | medium-over   | fail         |
      | normal           | low-over      | fail         |
      | normal           | no-findings   | pass         |
      | empty            | only-low      | fail         |
      | N/A              | only-low      | fail         |
      | N/A              | no-findings   | pass         |
      | partial          | high-over     | pass         |
