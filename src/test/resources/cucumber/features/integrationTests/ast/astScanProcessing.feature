@AstIntegrationTests @IntegrationTest
Feature: Cx-Flow SCA Integration permutation tests
  

  @ASTRemoteRepoScan
  Scenario Outline: scan multiple initiators
    Given scan initiator list is "<initiators>"
    And  GIT remote repository source
    When scan is finished
    Then the returned contain populated results for all initiators

    Examples:
      | initiators |
      | AST        |
      | AST,SCA    |
 