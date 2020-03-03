@Skip @PublishFeature @ComponentTest
Feature: Check Component tests publish functionality

  Background:
    Given Mocked SAST or OSA results Object

  Scenario Outline:  retrieve OSA results from input results object and publish to an appropriate bug tracker object
    Given OSA projectName is <projectName>
    When OSALibraries path is is: "<OSALibrariesFile>" and OSAVulnerailibies path is: "<OSAVulnerailibiesFile>"
    Then obejct file is created in channel: "<channel>" and will contain the OSALibraries number <OSALibrariesNum> and OSAVulnerailibies number <OSAVulnerailibiesNum>
    Examples:
      | projectName    |  channel         |    OSALibrariesFile       |  OSAVulnerailibiesFile     | OSAVulnerailibiesNum | OSALibrariesNum |
      | OSAVulsLibs    |  Jira            |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  GitHub          |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  Gitlab          |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  BitBucketServer |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  BitBucketCloud  |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  XML             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  Azure           |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  CSV             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  email           |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  WEB             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    num          |
      | OSAVulsLibs    |  PullMergeMarkdown  |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow  |            num       |    num          |


      | OSAVulsOnly    |  Jira            |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  GitHub          |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  Gitlab          |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  BitBucketServer |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  BitBucketCloud  |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  XML             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  Azure           |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  CSV             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  email           |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  WEB             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSAVulsOnly    |  PullMergeMarkdown  |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow  |            num       |    0          |

      | OSALibsOnly    |  Jira            |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  GitHub          |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  Gitlab          |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  BitBucketServer |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  BitBucketCloud  |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  XML             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  Azure           |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  CSV             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  email           |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  WEB             |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow     |            num       |    0          |
      | OSALibsOnly    |  PullMergeMarkdown  |    C:\\CxReports\\cxflow  |  C:\\CxReports\\cxflow  |            num       |    0          |

  Scenario Outline:  retrieve SAST results from input results object and publish to an appropriate bug tracker object
    When team is: "<team>" and project is: "<project>"
    Then Output Object will be created for the appropriate bug tracker channel <channel> with with vulnerabilities number <number>
    Examples:
      | project         | team      |  channel              | number |
      | CodeInjection1  | CxServer  |   Jira                |  3   |
      | CodeInjection1  | CxServer  |   GitHub              |  3   |
      | CodeInjection1  | CxServer  |   Gitlab              |  3   |
      | CodeInjection1  | CxServer  |   BitBucketServer     |  3   |
      | CodeInjection1  | CxServer  |   BitBucketCloud      |  3   |
      | CodeInjection1  | CxServer  |   XML                 |  3   |
      | CodeInjection1  | CxServer  |   Azure               |  3   |
      | CodeInjection1  | CxServer  |   CSV                 |  3   |
      | CodeInjection1  | CxServer  |   email               |  3   |
      | CodeInjection1  | CxServer  |   WEB                 |  3   |
      | CodeInjection1  | CxServer  |   PullMergeMarkdown   |  3   |


      | ProjWitNoVulnerabilities    | CxServer  |   Json                |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   Jira                |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   GitHub              |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   Gitlab              |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   BitBucketServer     |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   BitBucketCloud      |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   XML                 |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   Azure               |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   CSV                 |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   email               |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   WEB                 |  0   |
      | ProjWitNoVulnerabilities    | CxServer  |   PullMergeMarkdown   |  0   |

  Scenario Outline:  retrieve Big SAST results to a bug tracker
    When team is: "<team>" and project is: "<project>"
    Then Output Object will be created for the appropriate bug tracker channel <channel> with with vulnerabilities number <number>
    Examples:
      | project                       | team      |  channel              | number |
      | ProjWitManyVulnerabilities    | CxServer  |   Json                |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   Jira                |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   GitHub              |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   Gitlab              |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   BitBucketServer     |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   BitBucketCloud      |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   XML                 |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   Azure               |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   CSV                 |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   email               |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   WEB                 |  ?   |
      | ProjWitManyVulnerabilities    | CxServer  |   PullMergeMarkdown   |  ?   |