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