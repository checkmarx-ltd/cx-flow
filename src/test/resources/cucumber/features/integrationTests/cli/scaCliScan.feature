@SCA_CLI_SCAN  @IntegrationTest
Feature: SCA support in CxFlow command-line
    Background: Only SCA vulnerability scanner is enabled in CxFlow
        Bug tracker doesn't contain any issues at the beginning of each test run.

    Scenario: SCA CLI scan of a local directory
        Given source directory contains vulnerable files
        When running CxFlow with `scan local sources` options
        Then bug tracker contains some issues
        And no exception is thrown

    Scenario Outline: Testing break-build functionality
        When running a SCA scan with <expected-scenario> input
        Then run should exit with exit code <exit-code-number>

        Examples:
            | expected-scenario           | exit-code-number |
            | success                     | 0                |
            | break-build                 | 10               |
            | missing-mandatory-parameter | 1                |
            | missing-project             | 2                |
            | error-processing-request    | 10               |



    Scenario Outline: Testing cli filter functionality
        Given code has 6 High, 11 Medium and 1 low issues
        When running sca scan <filter>
        Then bug tracker contains <number of issue> issues

        Examples:
            | filter     | number of issue |
            | none       | 19              |
            | High       | 6               |
            | Medium,Low | 13              |
            | Low        | 2               |


    Scenario Outline: Publishing latest scan results
        # Normal flow. Make sure that only the latest scan is used.
        Given previous scan for a SCA project contains 2 findings
        And last scan for the project contains <latest count> findings
        When running CxFlow with `publish latest scan results` options
        Then bug tracker contains <latest count> issues
        Examples:
            | latest count |
            | 0            |
            | 5            |
            | 12           |

    Scenario: Trying to publish latest scan results for a non-existent project
        # Make sure CxFlow doesn't crash in this case.
        Given the "ci-non-existent-project-test" project doesn't exist in SCA
        When running CxFlow with `publish latest scan results` options
        Then bug tracker contains no issues
        And no exception is thrown


    Scenario: Trying to publish latest scan results for a project with no scans
        # Make sure CxFlow doesn't crash in this case either.
        Given the "ci-project-no-scans-test" project exists in SCA but doesn't have any scans
        When running CxFlow with `publish latest scan results` options
        Then bug tracker contains no issues
        And no exception is thrown


    Scenario Outline: While publishing latest scan results, CxFlow must respect SCA filters
        Given last scan for a project "ci-sca-cli-integration-tests" contains 50 High, 3 Medium and 1 Low-severity findings
        When run CxFlow with `publish latest scan results` options and <filters>
        Then bug tracker contains <expected issue count> issues
        Examples:
            | filters         | expected issue count |
            | Medium          | 3                    |
            | Medium,Low      | 3                    |
            | none            | 55                   |