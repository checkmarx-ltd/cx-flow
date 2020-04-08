@Component
Feature: Test analytics for get results operation




  Scenario Outline: do get results operation for a known project, and validate the analytics created for the operation.
    When doing get results operation on a scan with <high> <medium> <low> <info> results
    Then we should see the expected number of tickets in analytics
    Examples:
      | high  | medium  | low   | info  |
      | 10    | 10      | 10    | 10    |
      | 10    | 0       | 0     | 0     |
      | 0     | 0       | 10    | 0     |


   Scenario Outline: do get results operation for a known project with filters, and validate the analytics created for the operation.
     When doing get results operation on a scan with <high> <medium> <low> <info> results and filter is "<filter>"
     Then we should see the expected number of tickets in analytics
     Examples:
        | high  | medium  | low   | info  | filter  |
        | 10    | 11      | 12    | 13    | HIGH    |
        | 10    | 11      | 12    | 13    | MEDIUM  |
        | 10    | 11      | 12    | 13    | HIGH    |
