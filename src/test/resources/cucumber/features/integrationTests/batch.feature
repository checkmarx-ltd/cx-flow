@Skip @BatchFeature @IntegrationTest
Feature: Check Integration tests Batch functionality - retrieve scans of existing project
## TODO Should be tested with the following environment types:
# | version |  environmentType |
# |   8.9   |      HTTP        |
# |   8.9   |      HTTPS       |
# |   9.0   |      HTTP        |
# |   9.0   |      HTTPS       |

  Background:
    Given running SAST environment


  Scenario Outline:  retrieve SAST results for a specific for specific project
    When team is: "<team>" and project is: "<cx-project>" and batch mode is <batchMode>
    Then output file is created and will contain the vulnerabilities number <number>
    Examples:
      | batchMode | cx-project     | team     | number    |
      | --project | CodeInjection1 | CxServer | 3         |
      | --project |                | CxServer | exception |

  Scenario Outline:  retrieve last scan of a project with many scans
    Given SAST env with project CodeInjection and scans 2
    And first scan had 3 results and second scan has 2 results
    When   batch mode is <batchMode>
    Then output file is created and will contain the vulnerabilities number <number>
    Examples:
      | batchMode |   number    |
      | --project |   2         |


  Scenario Outline:  retrieve SAST results for a all projects of a team
    Given SAST environment which contains project CodeInjection0 under team \CxServer
    And 3 projects CodeInjection1,CodeInjection2,CodeInjection3 under team \CxServer\MyTeam1
    And 3 projects CodeInjection4,CodeInjection5,CodeInjection6 under team \CxServer\MyTeam2
    But no other projects exist in SAST
    When input team is: "<teamIn>" and batch mode is <batchMode> and
    Then output file is created and will contain projects: the following project list "<projectList>" under "<teamOut>"
    Examples:
      | batchMode | teamIn            | teamOut           | projectList                                  |
      | --batch   | \CxServer\MyTeam1 | \CxServer\MyTeam1 | CodeInjection1,CodeInjection2,CodeInjection3 |
      | --batch   | \CxServer\MyTeam2 | \CxServer\MyTeam2 | CodeInjection4,CodeInjection5,CodeInjection6 |
      | --batch   | \CxServer         | \CxServer\MyTeam2 | CodeInjection0                               |
      | --project | \CxServer\MyTeam1 | \CxServer\MyTeam1 | CodeInjection1                               |

  Scenario Outline:  retrieve SAST results for all projects in SAST instance
    Given SAST environment which contains project CodeInjection0 under team \CxServer
    And 3 projects CodeInjection1,CodeInjection2 under team \CxServer\MyTeam1
    And 3 projects CodeInjection4,CodeInjection5 under team \CxServer\MyTeam2
    But no other projects exist in SAST
    When input team not supplied and batch mode is <batchMode>
    Then output file is created and will contain projects: the following project "<project>" under "<teamOut>"
    Examples:
      | batchMode | teamOut                             | project                             |
      | --batch   | \CxServer\MyTeam1                   | CodeInjection1                      |
      | --batch   | \CxServer\MyTeam1                   | CodeInjection2                      |
      | --batch   | \CxServer\MyTeam2                   | CodeInjection4                      |
      | --batch   | \CxServer\MyTeam2                   | CodeInjection5                      |
      | --batch   | \CxServer                           | CodeInjection0                      |
      | --project | exception - team is mandatory field | exception - team is mandatory field |




  Scenario Outline:  retrieve SAST results of different size into output channels: Json, Csv, Xml
    When team is: "<team>" and project is: "<project>"
    Then Output Object will be created for the appropriate bug tracker channel <channel> with with vulnerabilities number <number>
    Examples:
      | project                  | team     | channel | number |
      | ProjWitNoVulnerabilities | CxServer | Json    | 0      |
      | ProjWitNoVulnerabilities | CxServer | Csv     | 0      |
      | ProjWitNoVulnerabilities | CxServer | Xml     | 0      |

      | Android-java-2.7M        | CxServer | Json    | 100000 |
      | Android-java-2.7M        | CxServer | Csv     | 100000 |
      | Android-java-2.7M        | CxServer | Xml     | 100000 |



  Scenario Outline:  retieve SAST results with the following filters: --filter-severity, --filter-category, --filter-status, --filter-cwe
    When retrieving results for project CodeInjection with filter-severity is "<filter-severity>" and filter-category is "<filter-category>" and filter-cwe "<filter-cwe>" and filter-status "<filter-status>"
    Then output file will be create and will contain violations number <number> and
    Examples:
      | filter-severity | filter-category | filter-status | filter-cwe | number |
      | High            |                 |               |            | 1      |
      | Medium          |                 |               |            | 1      |
      |                 |                 |               |            | 3      |
      |                 | SQL_Injection   |               |            | 1      |
      |                 |                 |               | 79         | ?      |
      |                 |                 | Urgent        |            | ?      |
      |                 |                 | Confirmed     |            | ?      |