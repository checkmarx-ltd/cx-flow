@ScanFeature @IntegrationTest
Feature: Check Integration tests command line functionality - scan. Example:
And java -jar cx-flow-1.5.2.jar --scan --github --cx-project="CodeInjection" --repo-url="https://github.com/username/Inj.git"  --namespace="MyNamespace" --app="MyApp" --branch="master" --spring.config.location="C:\MyProjects\cx-flow-runtime\application.yml"
And java -jar cx-flow-1.5.2.jar --scan --f="C:\Users\orlyk\Desktop\projects_to_scan\Code_Injection\Code_Injection" --cx-team="CxServer" --cx-project="CodeInjection1" --app="ABC" --cx-team="/CxServer" --spring.config.location="C:\MyProjects\cx-flow-runtime\application.yml"


## TODO Should be tested with the following environment types:
# | version |  environmentType |
# |   8.9   |      HTTP        |
# |   8.9   |      HTTPS       |
# |   9.0   |      HTTP        |
# |   9.0   |      HTTPS       |

#  Scenario Outline: empty
#    Given github repository which contains project CodeInjection
#    When nothing "<nothing>"
#    Then do nothing
#    Examples:
#    |nothing|
#    |nothing| 
  
    
  
  Scenario Outline:   test which project name will be used or created for scan with parameters: cx-project,branch,repo-name,namespace,app,multi-tenant=false. Using github as a respoiroty.
    Given github repository which contains project CodeInjection
    When project is: "<cx-project>" and branch="<branch>"
    And namespace is: "<namespace>" and application is "<app>"
    And repo-name is "<repo-name>" and --repo-url is supplied and --github flag is supplied
    And team in application.yml is \CxServer\SP
    And multi-tenant=false
    Then The request sent to SAST reporitory will contain scan result with project name="<OutProjectName>" and team "<teamOut>"

    Examples:
      | cx-project    | branch | repo-name  | namespace   | app   | OutProjectName                | teamOut      |
      | CodeInjection | master |            |             | MyApp | CodeInjection                 | \CxServer\SP |
      | CodeInjection | master |            | MyNamespace | MyApp | CodeInjection                 | \CxServer\SP |
      |               | master | MyRepoName |             | MyApp | MyApp                         | \CxServer\SP |
      |               | master |            | MyNamespace | MyApp | MyApp                         | \CxServer\SP |
      |               | master | MyRepoName | MyNamespace |       | MyNamespace-MyRepoName-master | \CxServer\SP |


  Scenario Outline:  test team name for different scan parameters: cx-project,branch,repo-name,namespace,app,multi-tenant=true. Using github as a respoiroty.
    Given github repository which contains project CodeInjection
    When project is: "<cx-project>" and branch="<branch>"
    And namespace is: "<namespace>" and application is "<app>"
    And repo-name is "<repo-name>" and --repo-url is supplied and --github flag is supplied
    And team in application.yml is \CxServer\SP
    And multi-tenant=true
    Then The request sent to SAST reporitory will contain scan result with project name="<OutProjectName>" and team "<teamOut>"

    Examples:
      | cx-project    | branch | repo-name  | namespace   | app   | teamOut                  | OutProjectName    |
      | CodeInjection | master |            |             | MyApp | \CxServer\SP             | CodeInjection     |
      | CodeInjection | master |            | MyNamespace | MyApp | \CxServer\SP\MyNamespace | CodeInjection     |
      |               | master | MyRepoName |             | MyApp | \CxServer\SP             | MyRepoName-master |
      |               | master | MyRepoName |             |       | \CxServer\SP             | MyRepoName-master |


  @Skip @File
  Scenario Outline:  test which project name will be used or created for scan with parameters: cx-project,branch,repo-name,namespace,app,multi-tenant. Using file system as a respoiroty.
    Given scan of a file
    When project is: "<cx-project>" and source is supplied as a file (--f)
    And team is set to CxServer in application.yml and team parameter is "<team>"
    And namespace is: "<namespace>" and application is "<app>" and multi-tenant="<multi-tenant>"
    Then The request sent to SAST will contain scan result with project name="<OutProjectName>"

    Examples:
      | team             | cx-project    | namespace   | app   | multi-tenant | OutProjectName |
      |                  | CodeInjection |             |       | true         | CodeInjection  |
      | \CxServer        | CodeInjection |             |       | true         | CodeInjection  |
      | \CxServer\MyTeam | CodeInjection |             |       | true         | CodeInjection  |
      |                  | CodeInjection | MyNamespace | MyApp | true         | CodeInjection  |
      |                  | CodeInjection | MyNamespace |       | false        | CodeInjection  |

  @Skip @File
  Scenario Outline:  test which project name will be used or created for scan with parameters: cx-project,branch,repo-name,namespace,app,multi-tenant. Using file system as a respoiroty.
    Given scan of a file
    When project is: "<cx-project>" and source is supplied as a file (--f)
    And team is set to CxServer in application.yml and team parameter is "<team>"
    And namespace is: "<namespace>" and application is "<app>" and multi-tenant="<multi-tenant>"
    Then SAST will contain scan result under team "<teamOut>"

    Examples:
      | team             | cx-project    | namespace   | app   | multi-tenant | teamOut                                              |
      |                  | CodeInjection |             |       | true         | \CxServer                                            |
      | \CxServer        | CodeInjection |             |       | true         | \CxServer                                            |
      | \CxServer\MyTeam | CodeInjection |             |       | true         | \CxServer\MyTeam                                     |
      |                  | CodeInjection | MyNamespace | MyApp | true         | \CxServer\MyNamespace                                |
      |                  | CodeInjection | MyNamespace |       | false        | exception - team \CxServer\MyNamespace doesn't exist |
    
    
  Scenario Outline: test scan with different vulnerabilities numbers and severities
    Given there is a SAST environment configured and running
    When  running a scan for repository "<repo_url>"
    Then  SAST output will contain high severity number <high> and medium severity number <medium> and low severity number <low>
    
    Examples:
      | repo_url                                                       | high | medium | low |
      | https://github.com/cxflowtestuser/Code_Injection.git           | 0    | 1      | 1   |
      | https://github.com/cxflowtestuser/VB_3845.git                  | 2    | 3      | 0   |
      | https://github.com/cxflowtestuser/amplify-multienv-example.git | 0    | 0      | 1   |

  
  Scenario Outline:  retrieve SAST results with the following filters: --filter-severity, --filter-category, --filter-status, --filter-cwe
    Given there is a SAST environment configured and running
    And running a scan for repository "<repoUrl>"
    And filter-severity is "<filter-severity>" and filter-category is "<filter-category>" and filter-cwe "<filter-cwe>" and filter-status "<filter-status>"
    Then output file will contain vulnerabilities <number>
    Examples:
      | repoUrl                                              | filter-severity | filter-category                             | filter-status    | filter-cwe | number |
      | https://github.com/cxflowtestuser/VB_3845.git        | High            |                                             |                  |            | 2      |
      | https://github.com/cxflowtestuser/VB_3845.git        | Medium          |                                             |                  |            | 3      |
      | https://github.com/cxflowtestuser/VB_3845.git        | High,Medium     |                                             |                  |            | 5      |
      | https://github.com/cxflowtestuser/VB_3845.git        |                 |                                             |                  |            | 5      |
      | https://github.com/cxflowtestuser/VB_3845.git        |                 | SQL_Injection                               |                  | 89         | 2      |
      | https://github.com/cxflowtestuser/Code_Injection.git | Medium          | CGI_Reflected_XSS_All_Clients               |                  |            | 1      |
      | https://github.com/cxflowtestuser/Code_Injection.git | High            | CGI_Reflected_XSS_All_Clients               |                  | 79         | 0      |
      | https://github.com/cxflowtestuser/Code_Injection.git |                 |                                             |                  | 89         | 0      |
      | https://github.com/cxflowtestuser/Code_Injection.git | Medium          | CGI_Reflected_XSS_All_Clients,SQL_Injection |                  | 79         | 1      |
      | https://github.com/cxflowtestuser/VB_3845.git        |                 |                                             | Urgent           |            | 0      |
      ## TODO following 2 tests on filter-status are failing. Need to fix 
      #| https://github.com/cxflowtestuser/VB_3845.git        | High            |                                             | To Verify,Urgent |            | 2      |
      #| https://github.com/cxflowtestuser/VB_3845.git        |                 | CGI_Reflected_XSS_All_Clients               | To Verify        | 79         | 1      |
      ## 79 = XSS_Reflected , 89 = SQL_Injection
      | https://github.com/cxflowtestuser/BookStoreJava      |                 |                                             |                  | 79,89      | 5      |

  
    
  @Parallel @Skip
  Scenario Outline: Run multiple different scans in parallel, each with different expected output
    Given there is a SAST environment configured and running
    When  running a scan for repositories "<repo_url1>" and "<repo_url2>"
    Then  SAST output for repository1 will contain high severity vulnerabilities <high1>
    And  and medium vulnerabilities <medium1> and low severity vulnerabilities  <low1>
    Then  SAST output for repository2 will contain high severity vulnerabilities <high2>
    And  and medium vulnerabilities <medium2> and low severity vulnerabilities  <low2>

    Examples:
      | repo_url1                                     | repo_url2                                                      | high1 | medium1 | low1 | high2 | medium2 | low2 |
      | https://github.com/cxflowtestuser/VB_3845.git | https://github.com/cxflowtestuser/amplify-multienv-example.git | 2     | 3       | 0    | 0     | 0       | 1    |


  @Skip
  Scenario Outline:  Input Validation for parameters project, team, preset
    Given SAST env running
    When team is: "<team>" and project is: "<project>" and preset is "<preset>" but parameters are invalid
    Then exception <exception> will be thrown
    Examples:
      | project        | team         | preset            | exception              |
      | unexisting     | CxServer     | Checkmarx Default | project not found      |
      | not_provided   | CxServer     | Checkmarx Default | project is mandatory   |
      | CodeInjection1 | unexisting   | Checkmarx Default | team not found in SAST |
      | CodeInjection1 | not_provided | Checkmarx Default | team is mandatory      |

  Scenario Outline:  test how excluding folders and files will change scan result
    Given there is a SAST environment configured and running
    When running a scan for repository "<repoUrl>"
    And The request sent to SAST will contain exclude-folder "<exclude-folders>" and exclude files "<exclude-files>"
    Then SAST output will contain high severity number <high> and medium severity number <medium> and low severity number <low>
    And scanned lines of code will be "<loc>"
    
    Examples:
      | repoUrl                                                        | exclude-folders | exclude-files           | loc   | high | medium | low |
      | https://github.com/cxflowtestuser/amplify-multienv-example.git |                 |                         | 13712 | 0    | 0      | 1   |
      #| https://github.com/cxflowtestuser/amplify-multienv-example.git | src             |                         | 11538 | 0    | 0      | 1   |
      | https://github.com/cxflowtestuser/amplify-multienv-example.git | public          |                         | 13656 | 0    | 0      | 0   |
      | https://github.com/cxflowtestuser/amplify-multienv-example.git | public,src      |                         | 11482 | 0    | 0      | 0   |
      | https://github.com/cxflowtestuser/amplify-multienv-example.git | src             | index.html              | 11497 | 0    | 0      | 0   |
      | https://github.com/cxflowtestuser/amplify-multienv-example.git | amplify,graphql | App.js,serviceWorker.js | 10388 | 0    | 0      | 1   |



