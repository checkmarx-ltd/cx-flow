@SAST_CLI_SCAN @IntegrationTest
Feature: Cx-Flow CLI SAST Integration tests

  Background: running SAST scan
    Given repository is github-sast

  Scenario Outline: Testing break-build functionality
    When running with break-build on <issue-type>
    Then run should exit with exit code <exit-code-number>

    Examples:
      | issue-type                  | exit-code-number |
      | success                     | 0                |
      | missing-mandatory-parameter | 1                |
      | error-processing-request    | 10               |

  Scenario Outline: Testing cli filter functionality
    Given code has x High, y Medium and z low issues
    When running sast scan <filter>
    Then bugTracker contains <number of issue> issues

    Examples:
      | filter                 | number of issue |
      | no-filter              | x+y+z           |
      | filter-High-and-Medium | x+y             |
      | filter-only-Medium     | y               |
      | filter-invalid-cwe     | 0               |
