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
        Then bug tracker contains <number of issue> issues

        Examples:
            | filter                 | number of issue |
            | no-filter              | x+y+z           |
            # | filter-High-and-Medium | x+y             |
            # | filter-only-Medium     | y               |

    Scenario Outline: Publishing latest scan results
        # Normal flow. Make sure that only the latest scan is used.
        Given previous scan for a SCA project contains 2 findings
        And last scan for the project contains <latest count> findings
        When running CxFlow with `publish latest scan results` options
        Then bug tracker contains <latest count> issues
        Examples:
            | latest count |
            | 0            |
            | 1            |
            | 5            |

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