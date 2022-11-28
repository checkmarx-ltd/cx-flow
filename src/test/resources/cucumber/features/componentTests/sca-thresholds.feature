@ThresholdsFeature
@CxSCA
@ComponentTest
Feature: CxFlow should fail builds and pull requests if scan exeeds threshold

  Scenario Outline: CxFlow should fail pull request if threshold-severity is exceeded
    testing only the severity filter
    Given the following thresholds-severities:
      | threshold-name | threshold-for-high | threshold-for-medium | threshold-for-low |
      | N/A            | <omitted>          | <omitted>            | <omitted>         |
      | strict         | 0                  | 0                    | 0                 |
      | normal         | 5                  | 10                   | 15                |
      | partial        | <omitted>          | 10                   | <omitted>         |
    And the following scan findings:
      | name        | high        | medium       | low          |
      | exact       | 5           | 10           | 15           |
      | high-over   | more-than-5 | less-than-10 | less-than-15 |
      | medium-over | less-than-5 | more-than-10 | less-than-15 |
      | low-over    | less-than-5 | less-than-10 | more-than-15 |
      | only-low    | 0           | 0            | more-than-0  |
      | no-findings | 0           | 0            | 0            |
    When threshold-severity is configured to <threshold-to-use>
    And scan finding is <scan-findings>
    Then pull request should <pass-or-fail>

    Examples:
      | threshold-to-use | scan-findings | pass-or-fail |
      | normal           | exact         | pass         |
      | normal           | high-over     | fail         |
      | normal           | medium-over   | fail         |
      | normal           | low-over      | fail         |
      | normal           | no-findings   | pass         |
      | strict           | only-low      | fail         |
      | N/A              | only-low      | fail         |
      | N/A              | no-findings   | pass         |
      | partial          | high-over     | pass         |

  Scenario Outline: CxFlow should fail pull request if exceeded threshold-score
    testing only score
    When max findings score is <over-or-under> threshold-score
    Then pull request should <pass-or-fail>

    Examples:
      | over-or-under | pass-or-fail |
      | over         | fail         |
      | under        | pass         |
      | exact        | pass         |

  Scenario Outline: CxFlow should fail if a threshold is exceeded
    testing score and count (both are defined)
    When the following thresholds fails on <type>
    Then pull request should <pass-or-fail>

    Examples:
      | type  | pass-or-fail |
      | score | fail         |
      | count | fail         |
      | both  | fail         |
      | none  | pass         |


  Scenario Outline: CxFlow should approve or fail cli build operation, depending on whether threshold is exceeded
    Given SCA thresholds section <exist> in cxflow configuration
    And SCA thresholds <exceeded> by scan findings
    And cx-flow.break-build property is set to <break>
    When cxflow called with get-latest-sca-project-results cli command
    Then cxflow SCA should exit with the correct <exit code>

    Examples:
      | exist  | exceeded   | break    | exit code |
      | true   | true       | true     | 10        |
      | false  | false      | true     | 0         |
      | false  | false      | false    | 0         |
      | false  | false      | true     | 0         |
      | true   | false      | true     | 0         |
      | true   | false      | true     | 0         |
      | true   | true       | false    | 10        |