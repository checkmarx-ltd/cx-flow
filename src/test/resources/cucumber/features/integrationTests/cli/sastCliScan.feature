@SAST_CLI_SCAN @IntegrationTest
Feature: Cx-Flow CLI SAST Integration tests

  Background: running SAST scan
    Given repository is github-sast

  Scenario Outline: Testing cli filter functionality
    Given code has x High, y Medium and z low issues
    When running sast scan <filter>
    Then bugTracker contains <number of issue> issues
    And cxflow should exit with exit code: <return code>
    
    Examples:
      | filter                 | number of issue | return code | 
      | no-filter              | x+y+z           | 10          | 
      | no-filter              | x+y+z           | 10          | 
      | filter-High-and-Medium | x+y             | 10          | 
      | filter-High-and-Medium | x+y             | 10          | 
      | filter-only-Medium     | y               | 10          | 
      | filter-invalid-cwe     | 0               | 0           | 

  Scenario Outline: Testing break-build functionality
    When running with break-build on <issue-type>
    Then cxflow should exit with exit code: <exit-code-number>

    Examples:
      | issue-type                  | exit-code-number |
      | success                     | 0                |
      | missing-mandatory-parameter | 1                |
      | error-processing-request    | 10               |
    
    
  Scenario: Testing SAST scans with bugTracker None
    Given bug tracker is set to 'None'
    When running cxflow to execute SAST scan
    Then cxflow should not wait for scan results
    And cxflow should exit with exit code: 0

  Scenario Outline: Testing cli filter functionality
    Given code has x High, y Medium and z low issues
    When running sast scan <filter>
    Then bugTracker contains <number of issue> issues
    And cxflow should exit with exit code: <return code>
    And and enabledZipScan is set to "<enabledZipScan>"

    Examples:
      | filter    | number of issue | return code | enabledZipScan |
      | no-filter | x+y+z           | 10          | false          |
      | no-filter | x+y+z           | 10          | not set        |
      | no-filter | x+y+z           | 10          | true           |
