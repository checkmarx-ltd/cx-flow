@SCA_CLI_SCAN  @IntegrationTest
Feature: SCA support in CxFlow command-line
    Background: Only SCA vulnerability scanner is enabled in CxFlow

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

    Scenario Outline: Publishing latest scan results
        # Normal flow. Make sure that only the latest scan is used.
        Given bug tracker contains no issues
        And last scan for the corresponding SCA project contains "<latest count>" findings
        And previous scan for the project contains 2 findings
        When running CxFlow to publish latest scan results
        Then bug tracker contains <latest count> issues
        Examples:
            | latest count |
            | 0            |
            | 1            |
            | 3            |

    Scenario: Trying to publish latest scan results for a non-existent project
        # Make sure CxFlow doesn't crash in this case.
        Given bug tracker contains no issues
        And the "non-existent-test" project doesn't exist in SCA
        When running CxFlow to publish latest scan results for the "non-existent-test" project
        Then bug tracker still contains no issues
        And no exception is thrown

    Scenario: Trying to publish latest scan results for a project with no scans
        # Make sure CxFlow doesn't crash in this case either.
        Given bug tracker contains no issues
        And the "project-without-scans" project exists in SCA but doesn't have any scans
        When running CxFlow to publish latest scan results for the "project-without-scans" project
        Then bug tracker still contains no issues
        And no exception is thrown
