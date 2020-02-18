@Skip @ParseFeature @IntegrationTest
Feature: Check Component tests Parse functionality - parse report file which is result of a scan by CLI/Jenikns
And command line example: java -jar cx-flow-1.5.2.jar -–parse –f="C:\CliReport.xml”
And java -jar cx-flow-1.5.2.jar –parse –-f="C:\OSAVulnerailibiesFile.json”  --lib-file="C:\OSAVulnerailibiesFile.json”

  Background:
    Given SAST running env

## TODO Should be tested with the following environment type:
# | version |  environmentType |
# |   8.9   |      HTTP        |
# |   8.9   |      HTTPS       |
# |   9.0   |      HTTP        |
# |   9.0   |      HTTPS       |

  Scenario Outline:  parse OSA results to an output file (OSA Vulnerabilities, Osa Libraries)
    When project is: “<project>"
    And OSALibrariesFile (--lib-file) = OSALibrariesFile and OSAVulnerailibies (--f) = OSAVulnerailibiesFile
    Then output file is created that will contain the OSALibraries number <OSALibrariesNum> and OSAVulnerailibies number <OSAVulnerailibiesNum>
    Examples:
      | project     | team     |  OSAVulnerailibiesNum | OSALibrariesNum |
      | OSAVulsLibs | CxServer |  num                  | num             |
      | OSAVulsOnly | CxServer |  num                  | 0               |
      | OSALibsOnly | CxServer |  0                    | num             |

  Scenario Outline:  parse SAST results to output file
    When team is: "<team>" and project is: "<project>" and preset is "<preset>"
    Then output file is created and will contain the vulnerabilities number <number>
    Examples:
      | project        | team     | preset            | number |
      | CodeInjection1 | CxServer | Checkmarx Default | 3      |
      | CodeInjection1 | CxServer | ?                 | ?      |

  Scenario Outline:  parse SAST while applying filters: filter-severity , filter-category , filter-status , filter-cwe
    When filter-severity is "<filter-severity>" and filter-category is "<filter-category>" and filter-cwe "<filter-cwe>" and filter-status "<filter-status>"
    Then output file is created  and will contain violations number <number> and
    Examples:
       | filter-severity | filter-category | filter-status | filter-cwe | number |
       | High            |                 |               |            | 1      |
       | Medium          |                 |               |            | 1      |
       |                 |                 |               |            | 3      |
       |                 | SQL_Injection   |               |            | 1      |
       |                 |                 |               | 79         | ?      |
       |                 |                 | Urgent        |            | ?      |
       |                 |                 | Confirmed     |            | ?      |

  Scenario Outline:  parse SAST results with custom fields and description - online/offline mode
    Given SAST environment with custom fields defined
    When  working in mode "<mode>"
    Then output file is created and will contain issue description "<issueDescription>" and custom fields "<customFields>"
    Examples:
      | mode      | issueDescription | customFields |
      #the default mode is online - description will not be empty, and customFields will not be empty if exist in SAST
      |           | notEmpty    | notEmpty     |
      #while working in offline mode description and customFields will be empty
      | --offline |             |              |
