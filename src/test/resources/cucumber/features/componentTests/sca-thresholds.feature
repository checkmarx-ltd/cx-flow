@ThresholdsFeature
@CxSCA
@ComponentTest
Feature: CxFlow should fail builds and pull requests if scan exeeds threshold

  Scenario Outline: CxFlow should fail pull request if threshold-severity is exceeded
    testing only the severity filter
    Given the following thresholds-severitys:
      | threshold-name | threshold-for-high | threshold-for-medium | threshold-for-low |
      | N/A            | <omitted>          | <omitted>            | <omitted>         |
      | empty          | 0                  | 0                    | 0                 |
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
    When threshold-severity is cofigured to <threshold-to-use>
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

  Scenario Outline: CxFlow should fail pull request if exceedes threshold-score
    testing only score
    When max findings score is <isOver> threshold-score
    Then pull request should <pass-or-fail>

    Examples:
      | over-or-undr | pass-or-fail |
      | over         | fail         |
      | under        | pass         |
      | exact        | pass         |

  Scenario Outline: CxFlow should fail if a threshold is exceeded
    testing score and count
    When the folowing threashold/s <type> fails
    Then pull request should <pass-or-fail>

    Examples:
      | type  | pass-or-fail |
      | score | fail         |
      | count | fail         |
      | both  | fail         |
      | none  | pass         |