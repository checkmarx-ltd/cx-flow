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