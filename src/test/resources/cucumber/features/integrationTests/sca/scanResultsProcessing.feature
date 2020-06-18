@ScaIntegrationTests @IntegrationTest
Feature: Cx-Flow SCA Integration permutation tests

  @SCARemoteRepoScan
  Scenario Outline: create different remote repository scans types and check the returned results
    Given scan initiator is SCA
    And scan is configured as a "<visibility>" GIT remote repository source
    When scan is finished
    Then the returned results are not null

    Examples:
      | visibility |
      | public     |
      | private    |

  @SCAAnalytics
  Scenario Outline: SCA scan generates a scan report entry is created in Json Logger
    Given scan initiator is SCA
    And scan is configured as a "<visibility>" GIT remote repository source
    When scan is finished
    Then the returned results are not null
    And SCA scan report entry is created in Json Logger

    Examples:
      | visibility |
      | public     |

  @SCAAnalytics
  Scenario Outline: A failed SCA scan generates an entry with error message in Json Logger
    Given scan initiator is SCA
    And scan is configured using invalid GIT remote repository source "<repository>"
    Then SCA scan report entry is created in Json Logger with corresponding error message

    Examples:
      | repository              |
      | http://some_invalid_url |

  @SCA_Filtering
  Scenario Outline: Apply filter severity and filter score on SCA results
    Given scan initiator is SCA
    And SCA filter severity is enabled with "<severities>" filter
    And Sca filter score is enabled with <score> filter
    When SCA runs a new scan on Filters-Tests-Repo which contains 8 vulnerabilities results
    Then the expected number of sanitized vulnerabilities are <expected_vulnerabilities>

    Examples:
      | severities    | score | expected_vulnerabilities |
      | HIGH          | 7.5   | 2                        |
      | High, medium  | 5.6   | 3                        |
      | high, invalid | 9.2   | 1                        |
      |               | 5.6   | 3                        |
      | medium        | 0.0   | 6                        |
      |               | 0.0   | 8                        |
      | low           | 0.0   | 0                        |
      |               | -0.3  | 8                        |
