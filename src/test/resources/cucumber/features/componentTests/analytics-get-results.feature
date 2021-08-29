@Component
Feature: Test analytics for get results operation



  @Skip
  Scenario Outline: do get results operation for a known project, and validate the analytics created for the operation.
    When doing get results operation on SAST scan with <high> <medium> <low> <info> results
    Then we should see the expected number of tickets in analytics for SAST
    Examples:
      | high  | medium  | low   | info  |
      | 10    | 10      | 10    | 10    |
      | 10    | 0       | 0     | 0     |
      | 0     | 0       | 10    | 0     |

  @Skip
  Scenario Outline: do get results operation for a known project, and validate the analytics created for the operation.
    When doing get results operation on SCA scan with <high> <medium> <low> results
    Then we should see the expected number of tickets in analytics for SCA
    Examples:
      | high | medium | low |
      | 10   | 0      | 0  |
      | 10   | 5      | 0  |
      | 0    | 0      | 10  |


  @Skip
   Scenario Outline: do get results operation for a known project with filters, and validate the analytics created for the operation.
     When doing get results operation on SAST scan with <high> <medium> <low> <info> results and filter is "<filter>"
     Then we should see the expected number of tickets in analytics for SAST
     Examples:
        | high  | medium  | low   | info  | filter  |
        | 10    | 11      | 12    | 13    | HIGH    |
        | 10    | 11      | 12    | 13    | MEDIUM  |
        | 10    | 11      | 12    | 13    | HIGH    |

     