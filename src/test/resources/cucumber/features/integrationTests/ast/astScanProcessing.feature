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
 