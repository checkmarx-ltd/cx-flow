@ParseFeature @ComponentTest @SAST
Feature: Parsing SAST results
  SAST results in XML format (input) should be correctly converted to CxFlow JSON report (output)

  Scenario: Parsing SAST scan results with no findings
    Given input with no findings
    When parsing the input
    Then the generated CxFlow report contains no issues and the summary is empty


  Scenario Outline: Parsing SAST findings with a variety of vulnerability types and filenames
  Check CxFlow's ability to merge several SAST findings into a single CxFlow issue.
  Separate issues should be created for each combination of vulnerability type and filename in findings.
    Given input having the following findings: "<findings>"
    When parsing the input
    Then CxFlow report is generated with <number of issues> issues
    And each issue contains <number of details> results and the same number of details
    Examples:
      | findings                                                             | number of issues | number of details |
      | 2 findings with the same vulnerability type and in the same file     | 1                | 2                 |
      | 2 findings with the same vulnerability type and in different files   | 2                | 1                 |
      | 2 findings with different vulnerability types and in the same file   | 2                | 1                 |
      | 2 findings with different vulnerability types and in different files | 2                | 1                 |

  @NonDefaultConfiguration
  Scenario: Parsing SAST scan results with a variety of severities
  Findings should be correctly grouped by severity in report summary
    Given input has 4 findings with "Low" severity
    And input has 3 findings with "Medium" severity
    And input has 2 findings with "High" severity
    And each finding has a unique combination of vulnerability type + filename
    # The condition above is needed to prevent CxFlow from merging several findings into a single issue.
    # The merging functionality is covered in a separate test.
    When parsing the input with severity filter: Low, Medium, High
    Then CxFlow report summary contains a "Low" field with the value 4
    And CxFlow report summary contains a "Medium" field with the value 3
    And CxFlow report summary contains a "High" field with the value 2
    And CxFlow report summary contains only these 3 fields
    # Example how CxFlow issues are grouped by severity:
    #  "flow-summary": {
    #     "High": 2,
    #     "Medium": 3,
    #     "Low": 4
    #  }

  Scenario Outline: Parsing execution paths in SAST scan results
    CxFlow should only take the first and last <PathNode> elements from <Path> in SAST results.
    Given input with <number of findings> finding(s)
    And the execution path of each finding contains 5 nodes
    And each of the findings has a different filename
    When parsing the input
    Then CxFlow report is generated with <number of findings> issues
    And each issue contains 1 result
    And "source" object in each CxFlow result corresponds to the "first" execution path node in input
    And "sink" object in each CxFlow result corresponds to the "last" execution path node in input
    Examples:
      | number of findings |
      | 1                  |
      | 3                  |
        # Examples of data being compared:
        # From SAST:
        #  <PathNode>
        #    <FileName>DOS_Login.java</FileName>
        #    <Line>92</Line>
        #    <Column>37</Column>
        #    <Name>getRawParameter</Name>
        #     ...
        #  </PathNode>
        #
        # From CxFlow:
        #  "source": {
        #     "file": "DOS_Login.java",
        #     "line": "92",
        #     "column": "37",
        #     "object": "getRawParameter"
        #  }

  @NonDefaultConfiguration
  Scenario: Filtering findings from SAST scan results
    Given input containing 3 findings with different severities: Medium, High, Critical
    When parsing the input with severity filter: High, Critical
    Then CxFlow report is generated with 2 issues
    And issue severities are: High, Critical

  Scenario: Verify that generated reports match corresponding reference reports
    Given reference CxFlow reports are available for specific inputs
    When parsing each of these inputs
    Then the generated CxFlow report matches a corresponding reference report

  @NegativeTest
  Scenario Outline: Trying to run parsing with an invalid command line
    When running CxFlow with command line: "<command line>"
    Then CxFlow exits with exit code <exit code>
    Examples:
      | command line                                                                       | exit code |
      | --parse --offline --blocksysexit --app=MyApp                                       | 10        |
      | --parse --offline --blocksysexit --app=MyApp --f=m:\nonexistent\file-0192019593560 | 2         |
      | --parse --offline --blocksysexit --f=c:\dontCareIfFileExists                       | 1         |
