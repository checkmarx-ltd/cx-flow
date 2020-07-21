@SAST_CLI @IntegrationTest
Feature: Cx-Flow CLI Sast Integration tests

  @SAST_CLI_SCAN
  Scenario Outline: Run sast scan with cli command
    Given repository is github
    When running CxFlow scan with command line: "<command line>"
    Then CxFlow exits with exit code <exit code>
    And each bugTracker contains <number of issue> issues
    Examples:
      | command line                                                                                                                                                                                        | exit code | number of issue |
      | --scan  --severity=High --severity=Medium                      | 0         | 6    |
      | --scan  --severity=High --severity=Medium  --break-build=true  | 10        | 6    |
      | --scan  --cwe=1         --break-build=true                     | 0         | 0    |
      | --scan  --severity=High --break-build=false                    | 0         | 2    |
      | --scan  --severity=High                                        | 0         | 2    |


