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
      | HIGH          | 7.5   | 6                        |
      | High, medium  | 6.3   | 9                        |
      | high, invalid | 8.7   | 2                        |
      |               | 6.4   | 8                        |
      | medium        | 0.0   | 11                       |
      |               | 0.0   | 19                       |
      | low           | 0.0   | 2                        |
      |               | -0.3  | 19                       |


  @SCA_Issues_Creation 
  Scenario: Publish SCA results and check ADO tickets are getting created
    Given scan initiator is SCA
    And bug tracker is Azure
    When publishing new known unfiltered SCA results with 8 findings including 2 high and 6 medium vulnerabilities
    Then new 8 tickets should be created

  @SCA_Resolve_Issue 
  Scenario: Resolve an open vulnerability and check status updates to closed
    Given scanner is SCA
    And bug tracker is Jira
    And bug tracker contains open vulnerabilities
    When resolving on of the vulnerabilities
    Then resolved vulnerability status should getting update to closed

  @SCA_Zip_Scan 
  Scenario: Publish SCA results by zip folder
    Given scanner is SCA
    And enabledZipScan property is set with true
    When initiating a new scan
    Then returned scan high and medium results are bigger than zero

  @SCA_Policy_Management
  Scenario: Assign new violated policy to a project scan and validate that isPolicyViolated flag in SCA results is positive
    Given scanner is SCA
    And a violated policy is assigned to an existing project
    When initiating a new scan
    Then isPolicyViolated flag in SCA results should be positive
