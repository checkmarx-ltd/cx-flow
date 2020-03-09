@GitHubIntegrationTests @IntegrationTest
Feature: parse and then publish processing given SAST XML results, findings should be open or update an issue in GitHub

  @GitHubCreateIssuesByFilter
  Scenario Outline: publish results with severity filters and check GitHub issues are getting open
    Given target is GitHub
    When setting filter severity as "<filter-severity>"
    And publishing results with filter severity for input "<input>"
    Then <number-of-issues> new issues should be open according filters severity

    Examples:
      | input                                   | filter-severity | number-of-issues |
      | 5-findings-different-vuln-same-file.xml | High            | 1                |
      | 5-findings-different-vuln-same-file.xml | High,Medium     | 3                |
      | 5-findings-different-vuln-same-file.xml | Low             | 0                |

  @GitHubCreateIssuesFromDifferentFiles
  Scenario Outline: publish results from different files and check GitHub issues are getting open
    Given target is GitHub
    When publishing results from different files for input "<input>"
    Then <number-of-issues> new issues should be open

    Examples:
      | input                                            | number-of-issues |
      | many-findings-different-vuln-different-files.xml | 4                |
      | 1-finding-same-file.xml                          | 1                |

  @GitHubResolveIssueVulnerabilities
  Scenario: publish results and check GitHub issue is getting updated
    Given target is GitHub
    And for a given type, there is an open issue with multiple vulnerabilities
    When resolving a vulnerability
    Then the issue's code lines should be update

  @GitHubCloseIssue
  Scenario: publish results and check GitHub issue is getting closed
    Given target is GitHub
    And for a given issue, with a given vulnerabilities
    When resolving the issue's all vulnerabilities
    Then the issues should mark as closed




