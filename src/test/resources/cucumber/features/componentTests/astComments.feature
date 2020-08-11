@Component
Feature: Test analytics for get results operation




  Scenario Outline: do get results operation for a known project, and validate the analytics created for the operation.
    When repository is "<repo>" and scanner is "<scanner>"
    And doing get results operation on scan with <high> <medium> <low> <info> results
    Then we should see the expected number of results in comments
    Examples:
      | repo   | scanner | high | medium | low | info |
      | github | AST     | 10   | 10     | 10  | 10   |
      | github | AST     | 10   | 0      | 0   | 0    |
      | github | AST     | 0    | 0      | 10  | 0    |
      | github | AST     | 0    | 0      | 0   | 0    |
      | github | AST,SCA | 0    | 0      | 10  | 0    |
    

 

     