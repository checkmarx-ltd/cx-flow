@Github2AdoFeature
Feature: CxFlow should read configuration from cx.config file in the root of repository

  
  @ProjectName
  Scenario Outline: CxFlow will mimic CX-SAST results and create tickets in the appropriate project in Azure 
    Given application.yml contains the Azure project "<inputProject>" and Asure namespace "<inputNamespace>"
    And Scanner is Cx-SAST
    And commit or merge pull request is performed in github repo "<repo>" and branch "<branch>"
    And project "<outputProject>" exists in Azure under namespace "<outputNamespace>"
    And SAST scan produces high and medium results
    Then CxFlow will create appropriate tickets in project "<outputProject>" in namespace "<outputNamespace>" in Azure

    
    Examples:
      | repo     | branch | inputProject | inputNamespace | outputProject | outputNamespace |
      | testsAdo | master | CxTest1      | CxNamespace    | CxTest1       | CxNamespace     |
      | testsAdo | master | CxTest2      |                | CxTest2       | cxflowtestuser  |
      | testsAdo | master | testsAdo     |                | testsAdo      | cxflowtestuser  |


  @ProjectName
  Scenario Outline: CxFlow will mimic AST results and create tickets in the appropriate project in Azure
    Given application.yml contains the Azure project "<inputProject>" and Asure namespace "<inputNamespace>"
    And Scanner is AST
    And commit or merge pull request is performed in github repo "<repo>" and branch "<branch>"
    And project "<outputProject>" exists in Azure under namespace "<outputNamespace>"
    And SAST scan produces high and medium results
    Then CxFlow will create appropriate tickets in project "<outputProject>" in namespace "<outputNamespace>" in Azure
    And description field is populated

    Examples:
      | repo     | branch | inputProject | inputNamespace | outputProject | outputNamespace |
      | testsAdo | master | CxTest1      | CxNamespace    | CxTest1       | CxNamespace     |
      | testsAdo | master | CxTest2      |                | CxTest2       | cxflowtestuser  |
      | testsAdo | master | testsAdo     |                | testsAdo      | cxflowtestuser  |

 