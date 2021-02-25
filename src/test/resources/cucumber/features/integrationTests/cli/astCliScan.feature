@AST_CLI_SCAN  @IntegrationTest
Feature: AST support in CxFlow command-line

    @Skip
    Scenario Outline: AST CLI scan of a local directory
        Given scanner is <scanner>
        And source directory contains vulnerable files
        When running CxFlow with `scan local sources` option and <projectName>
        Then bug tracker contains <issueNumber> issues
        And no exception is thrown
        Examples:
            | scanner | issueNumber | projectName      |
            | AST     | 38          | ast-cli-test     |
            | AST,SCA | 43          | ast-cli-with-sca |

    @Skip
    Scenario Outline: Running a AST scan with break-build enabled and command line arguments
        When running a AST scan with break-build on <issue-type>
        Then run should exit with exit code <exit-code-number>

        Examples:
            | issue-type                  | exit-code-number |
            | missing-mandatory-parameter | 1                |
            | missing-project             | 2                |

    @Skip
    Scenario Outline: AST CLI scan of github repo
        Given scanner is <scanner>
        And  repository is github
        Then bug tracker contains <number of issue> issues

        Examples:
            | scanner | number of issue |
            | AST     | 12               |
            | AST,SCA | 24              |

