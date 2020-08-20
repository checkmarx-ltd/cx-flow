@SCA_CLI_SCAN  @IntegrationTest
Feature: SCA support in CxFlow command-line

    Scenario Outline: Testing break-build functionality
        When running a SCA scan with break-build on <issue-type>
        Then run should exit with exit code <exit-code-number>

        Examples:
            | issue-type                  | exit-code-number |
            | success                     | 0                |
            | missing-mandatory-parameter | 1                |
            | missing-project             | 2                |
            | error-processing-request    | 10               |

    Scenario Outline: Testing cli filter functionality
        Given code has x High, y Medium and z low issues
        When running sca scan <filter>
        Then bugTracker contains <number of issue> issues

        Examples:
            | filter                 | number of issue |
            | no-filter              | x+y+z           |
            # | filter-High-and-Medium | x+y             |
            # | filter-only-Medium     | y               |
