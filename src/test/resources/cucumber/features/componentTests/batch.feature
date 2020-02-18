@Skip @BatchFeature @ComponentTest
Feature: Check Component tests - retrieve scans of existing project

  Background:
    Given Mocked SAST environment with project CodeInjection1 and team CxServer

  Scenario Outline: Basic sanity positive and negative tests - get results successfully and invalid output file path
    When team is: "<team_name>" and project is: "<project_name>"
    Then output file is created in path: "<path>" and output will contain  "<project_name>" and "<team_name>" and vulnerabilities number <number>

    Examples:
      | project_name   | team_name | path                 | number    |
      | CodeInjection1 | CxServer  | someValidPath        | 3         |
      | CodeInjection1 | CxServer  | someUnexistingFolder | exception |


  Scenario Outline:  test command line when missing basic command line args: --batch,--scan, --parse,--project
    Given running command line with file repository source
    Then exception "<exception>" will be thrown
    Examples:
      | exception                                                          |
      | --scan \| --parse \| --batch \| --project option must be specified |

