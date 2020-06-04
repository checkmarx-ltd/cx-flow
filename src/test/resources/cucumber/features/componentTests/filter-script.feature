Feature: Using Groovy script to filter SAST findings

  Scenario Outline: Filtering SAST findings using a script
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And finding #1 has "High" severity, "ReOccured" status and "To Verify" state
    And finding #2 has "Medium" severity, "New" status and "Urgent" state
    And finding #3 has "Low" severity, "New" status and "To verify" state
    When CxFlow generates issues from the findings using "<filter script>"
    Then CxFlow report is generated with issues corresponding to these findings: "<findings>"
    Examples:
      | filter script   | findings |
      | <not specified> | 1,2,3    |
      | true            | 1,2,3    |
      | false           | <none>   |


  @NegativeTest
  Scenario: Both scripted filter and simple filters are specified in config
      # Check that an error is thrown