@AstIntegrationTests @IntegrationTest
Feature: Cx-Flow AST Integration permutation tests
  

  @ASTRemoteRepoScan
  Scenario Outline: scan multiple initiators
    Given scan initiator list is "<initiators>"
    Then the returned contain populated results for all initiators

    Examples:
      | initiators |
      | SCA        |
      | AST        |
      | AST,SCA    |
 