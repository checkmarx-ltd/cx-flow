@SCA_CLI_SCAN  @IntegrationTest
Feature: Cx-Flow CLI SCA Integration tests

  @SCA_CLI_SCAN
  Scenario Outline: Run sast scan with cli command
    Given repository is github-sca
    When running CxFlow scan with command line: "<command line>"
    Then CxFlow exits with exit code <exit code>
    And bugTracker contains <number of issue> issues
    Examples:
      | command line                                                                 | exit code | number of issue  |
      | --scan  --scanner=sca --severity=High --severity=Medium                      | x         | y                |
      | --scan  --scanner=sca --severity=High --severity=Medium  --break-build=true  | x         | y                |
      | --scan  --scanner=sca --CVE=1         --break-build=true                     | x         | y                |
      | --scan  --scanner=sca --severity=High --break-build=false                    | x         | y                |
      | --scan  --scanner=sca --severity=High                                        | x         | y                |


