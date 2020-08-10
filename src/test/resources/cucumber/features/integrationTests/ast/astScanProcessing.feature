@AstIntegrationTests @IntegrationTest
Feature: Cx-Flow AST Integration permutation tests
  

  @ASTRemoteRepoScan 
  Scenario Outline: scan multiple initiators
    Given scan initiator list is "<initiators>"
    Then the returned contain populated results for all initiators
    And sca finding count will be "<sca_findings>" and ast findings count "<ast_findings>" will be accordingly

    Examples:
      | initiators | sca_findings | ast_findings |
      | SCA        | 13           | 0            |
      | AST        | 0            | 11           |
      | AST,SCA    | 13           | 11           |

  @ASTRemoteRepoScan
  Scenario Outline: check error message on expired AST token
    Given AST scan with expired token
    Then expired token expected error will be returned "<message>"
    Examples:
      | message      |
      | Unauthorized |
      

  @ASTRemoteRepoScan
  Scenario Outline: check error message when AST is down
    Given AST scan is initiated when AST is not available
    Then unavailable AST server expected error will be returned "<message>"
    Examples:
      | message |
      | The resource you are looking for might have been removed, had its name changed, or is temporarily unavailable    |

  @AST_JIRA_issue_creation
  Scenario: Publish AST results and check JIRA tickets are getting created
    Given scan initiator is AST
    And bug tracker is JIRA
    When publishing new known unfiltered AST results
    Then new JIRA tickets should be open respectively