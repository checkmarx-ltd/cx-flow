@Skip @ScanFeature @ComponentTest
Feature: Check Component tests command line functionality - scan. Parameter set example:
  And java -jar cx-flow-1.5.2.jar --scan --github --cx-project="CodeInjection" --repo-url="https://github.com/username/Inj.git"  --namespace="MyNamespace" --app="MyApp" --branch="master" --spring.config.location="C:\MyProjects\cx-flow-runtime\application.yml"
  And java -jar cx-flow-1.5.2.jar --scan --f="C:\Users\test\Desktop\projects_to_scan\Code_Injection\Code_Injection" --cx-team="CxServer" --cx-project="CodeInjection1" --app="ABC" --cx-team="/CxServer" --spring.config.location="C:\MyProjects\cx-flow-runtime\application.yml"

  Scenario Outline:  test scan with unsupported repositories: bitbucket and Azure DevOps
    Given running scan flow
    When unsupported repository Mock <repository> which contains project CodeInjection and SAST mock
    Then exception "<exception>" will be thrown
    Examples:
      | repository  | exception                                   |
      | --bitbucket | Bitbucket git clone scan not implemented    |
      | --ado       | Azure DevOps git clone scan not implemented |





  Scenario Outline:  test team name for different scan parameters: cx-project,branch,repo-name,namespace,app,multi-tenant. Using github or gitlab as a respoiroty.
    Given repository Mock <repository> which contains project CodeInjection and SAST mock
    When project is: "<cx-project>" and branch="<branch>"
    And namespace is: "<namespace>" and application is "<app>" and multi-tenant="<multi-tenant>"
    And repo-name is "<repo-name>" and --repo-url is supplied
    Then The request sent to SAST reporitory will contain scan result with project name="<OutProjectName>"
    And and team "<teamOut>"

    Examples:
      | repository | cx-project    | branch | repo-name  | namespace   | app   | multi-tenant | OutProjectName                | teamOut               |
      | --gitlab   | CodeInjection | master |            |             | MyApp | true         | CodeInjection                 | CxServer              |
      | --gitlab   |               | master |            | MyNamespace | MyApp | true         | CodeInjection                 | \CxServer\MyNamespace |
      | --gitlab   |               | master | MyRepoName |             |       | true         | MyRepoName-master             | \CxServer             |
      | --gitlab   |               | master | MyRepoName | MyNamespace |       | false        | MyNamespace-MyRepoName-master | \CxServer             |
      | --github   |               | master |            | MyNamespace | MyApp | true         | CodeInjection                 | \CxServer\MyNamespace |
      | --github   |               | master | MyRepoName |             | MyApp | true         | MyRepoName-master             | \CxServer\MyNamespace |


  Scenario Outline:  test how excluding folders and files will change scan result
    Given SAST env Mock
    When repository Mock "<repository>" which contains project Bookstore
    And exclude-folders is: "<exclude-folders>" and exclude-files is "<exclude-files>"
    Then The request sent to SAST will contain exclude-folder "<exclude-folders>" and exclude files "<exclude-files>"

    Examples:
      | repository | exclude-folders | exclude-files |
      | --gitlab   |                 |               |
      | --github   |                 |               |



  Scenario Outline:  Input Validation for parameters cx-project,branch,repo-name,namespace,app
    Given github repository Mock and SAST env Mock
    When project is: "<cx-project>" and branch="<branch>" and multi-tenant "<multi-tenant>"
    And namespace is: "<namespace>" and application is "<app>"
    And repo-name is "<repo-name>" and --repo-url is supplied and source repository type <repo-type>
    Then exception "<exception>" will be thrown

    Examples:
      | cx-project    | namespace   | app   | repo-type | branch | repo-name  | multi-tenant | exception                                     |
      | CodeInjection | MyNamespace | MyApp |           | master | MyReponame | true         | No valid option was provided for driving scan |
      |               |             | MyApp | --gitlab  | master |            | true         | Namespace / RepoName / Branch are required    |
      |               |             | MyApp | --github  | master |            | true         | Namespace / RepoName / Branch are required    |

  Scenario Outline:  test incremental scan flow
    Given SAST env Mock
    When repository "<repository>" which contains project "<cx-project>" CodeInjectionIncremental
    And running with incremental flag "<incrementalIn>"
    Then The request sent to SAST will contain parameter incremental "<incrementalOut>"
    Examples:
      | repository | incrementalIn | incrementalOut |
      | --github   | incremental   | inc            |
      | --gitlab   | incremental   | inc            |

      ## flag not sent => parameter not created in a request
      | --github   |               |                |
      | --gitlab   |               |                |

  Scenario Outline:  generate SAST scan for a preset which is supplied or calculated
    Given SAST env Mock
    When preset is "<preset>"
    Then SAST request object will contain "<PresetToSAST>"

    Examples:
      | preset            | PresetToSAST         |
      | Checkmarx Default | Checkmarx Default    |
      | XSS and SQLi only | XSS and SQLi only    |
      | Empty preset      | Empty preset         |
      |                   | someCalculatedPreset |


