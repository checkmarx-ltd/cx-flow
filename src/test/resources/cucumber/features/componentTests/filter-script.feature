Feature: Using Groovy script to filter SAST findings

  Scenario Outline: Filtering SAST findings using a script
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And finding #1 has "High" severity, "ReOccured" status and "To Verify" state
    And finding #2 has "Medium" severity, "New" status and "Urgent" state
    And finding #3 has "Low" severity, "New" status and "To verify" state
    And no simple filters are defined
    And filter script is "<filter script>"
    When CxFlow generates issues from findings
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

  @NegativeTest
  Scenario: Both scripted filter and simple filters are specified in config
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And status filter is set to "New"
    And filter script is "finding.severity == 'MEDIUM'"
    When CxFlow generates issues from findings
    Then CheckmarxRuntimeException is thrown
    And the exception message contains the text: "Simple filters and scripted filter cannot be used together"

  @NegativeTest
  Scenario Outline: Invalid script syntax
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And no simple filters are defined
    And filter script is "<invalid script>"
    When CxFlow generates issues from findings
    Then CheckmarxRuntimeException is thrown
    And the exception message contains the text: "make sure the script syntax is correct"
    Examples:
      | invalid script        |
      | (finding.severity     |
      | 12q3C = Z'            |
      | Bonnie Prince Charlie |

  @NegativeTest
  Scenario Outline: Script runtime error
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And no simple filters are defined
    And filter script is "<invalid script>"
    When CxFlow generates issues from findings
    Then CheckmarxRuntimeException is thrown
    And the exception message contains the text: "runtime error has occurred while executing the filter script"
    Examples:
      | invalid script               |
      | finding.creativity == 'HIGH' |
      | nonExistingVar > 23          |

  @NegativeTest
  Scenario: Script returns a non-boolean value
    Given SAST report containing 3 findings, each in a different file and with a different vulnerability type
    And no simple filters are defined
    And filter script is "return 302"
    When CxFlow generates issues from findings
    Then CheckmarxRuntimeException is thrown
    And the exception message contains the text: "Filtering script must return a boolean value"