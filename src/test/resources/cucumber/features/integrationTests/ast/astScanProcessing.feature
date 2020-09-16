@AstIntegrationTests @IntegrationTest
Feature: Cx-Flow AST Integration permutation tests


  @ASTRemoteRepoScan
  Scenario Outline: using multiple vulnerability scanners
    Given enabled vulnerability scanners are "<scanners>"
    Then scan results contain populated results for all scanners
    And sca finding count will be "<sca_findings>" and ast findings count "<ast_findings>" will be accordingly

    Examples:
    # Cannot rely on an exact number of findings, because it may change after backend version updates.
      | scanners | sca_findings | ast_findings |
      | SCA      | > 0          | 0            |
      | AST      | 0            | > 0          |
      | AST,SCA  | > 0          | > 0          |

  @ASTRemoteRepoScan @InvalidCredentials
  Scenario Outline: Trying to scan with invalid credentials
    When CxFlow tries to start AST scan with the "<client id>" and "<client secret>" credentials
    Then an error will be thrown with the message containing "<message>"
    Examples:
    # <valid-xxx> values are replaced with actual values from the test config.
      | client id          | client secret   | message                           |
      | <valid-client-id>  | my-wrong-secret | unauthorized                      |
      | my-wrong-client-id | <valid-secret>  | unauthorized                      |
      | <valid-client-id>  | <empty>         | AST client secret wasn't provided |
      | <empty>            | <valid-secret>  | AST client ID wasn't provided     |


  @ASTRemoteRepoScan
  Scenario Outline: check error message when AST is down
    Given AST scan is initiated when AST is not available
    Then unavailable AST server expected error will be returned "<message>"
    Examples:
      | message                                                                                                       |
      | The resource you are looking for might have been removed, had its name changed, or is temporarily unavailable |

  @AST_JIRA_issue_creation
  Scenario: Publish AST results and check JIRA tickets are getting created
    Given scan initiator is AST
    And bug tracker is JIRA
    When publishing new known unfiltered AST results
    Then new JIRA tickets should be open respectively