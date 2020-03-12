@GitHubIntegrationTests @IntegrationTest
Feature: parse and then publish processing given SAST XML results, findings should be open or update an issue in GitHub

  @GitHubCreateIssuesByFilter
  Scenario Outline: publish results with severity filters and verify GitHub issues are getting opened
    Given bug tracker is GitHub
    And setting filter severity as "<filter-severity>"
    When publishing "<input>"
    Then <number-of-issues> new issues should be opened according filters severity

    Examples:
      | input                                   | filter-severity | number-of-issues |
      | 5-findings-different-vuln-same-file.xml | High            | 1                |
      | 5-findings-different-vuln-same-file.xml | High,Medium     | 3                |
      | 5-findings-different-vuln-same-file.xml | Low             | 0                |

  @GitHubCreateIssuesFromDifferentFiles
  Scenario Outline: publish results from different files and check GitHub issues are getting opened
    Given bug tracker is GitHub
    When publishing results from "<input>"
    Then <number-of-issues> new issues should be opened

    Examples:
      | input                                            | number-of-issues |
      | many-findings-different-vuln-different-files.xml | 4                |
      | 1-finding-same-file.xml                          | 1                |

  @GitHubResolveIssueVulnerabilities
  Scenario: publish results and check GitHub issue is getting updated
    Given bug tracker is GitHub
    And for a given type, there is an opened issue with multiple vulnerabilities
    When resolving a vulnerability
    Then the issue's code lines should be update

  @GitHubCloseIssue
  Scenario: publish results and check GitHub issue is getting closed
    Given bug tracker is GitHub
    And for a given issue, with a given vulnerabilities
    When resolving all vulnerabilities for an issue
    Then the issues should be mark as closed




