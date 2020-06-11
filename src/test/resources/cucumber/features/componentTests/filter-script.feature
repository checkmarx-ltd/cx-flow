Feature: Using Groovy script to filter SAST findings

  Scenario Outline: Filtering SAST findings using a script
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And finding #1 has "High" severity, "ReOccured" status and "To Verify" state
    And finding #2 has "Medium" severity, "New" status and "Urgent" state
    And finding #3 has "Low" severity, "New" status and "To verify" state
    And no simple filters are defined
    And filter script is "<filter script>"
    When CxFlow generates issues from the findings
    Then CxFlow report is generated with issues corresponding to these findings: "<findings>"
    Examples:
      | filter script                                                                               | findings |
      | <not specified>                                                                             | 1,2,3    |
      | true                                                                                        | 1,2,3    |
      | false                                                                                       | <none>   |
      | finding.severity == 'HIGH' \|\| (finding.severity == 'MEDIUM' && finding.state == 'URGENT') | 1,2      |
      | finding.severity == 'HIGH' \|\| (finding.severity == 'MEDIUM' && finding.status == 'NEW')   | 1,2      |
      | finding.state != 'TO VERIFY'                                                                | 2        |
      | finding.status == 'NEW'                                                                     | 2,3      |
      | finding.status == 'REOCCURED'                                                               | 1        |

  @Skip
  @NegativeTest
  Scenario: Both scripted filter and simple filters are specified in config
    # Check that an error is thrown
  @Skip
  @NegativeTest
  Scenario: Invalid script syntax
    # Throw an exception with a meaningful message
  @Skip
  @NegativeTest
  Scenario: Script runtime error
    # E.g. script uses a non-existent variable
    # Throw an exception with a meaningful message
