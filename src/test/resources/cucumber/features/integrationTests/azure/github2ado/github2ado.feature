@Github2AdoFeature
Feature: CxFlow should read configuration from cx.config file in the root of repository

  
  Scenario Outline: CxFlow will create tickets in the appropriate project in Azure 
    Given application.yml contains the Azure project "<inputProject>" and Asure namespace "<inputNamespace>"
    And commit or merge pull request is performed in github repo "<repo>" and branch "<branch>"
    And project "<outputProject>" exists in Azure under namespace "<outputNamespace>"
    And SAST scan produces high and medium results
    Then CxFlow will create appropriate tickets in project "<outputProject>" in namespace "<outputNamespace>" in Azure

    Examples:
      | repo     | branch | inputProject | inputNamespace | outputProject | outputNamespace |
      | testsAdo | master | CxTest1      | CxNamespace    | CxTest1       | CxNamespace     |
      | testsAdo | master | CxTest2      |                | CxTest2       | cxflowtestuser  |
      #| testsAdo | master |              | CxNamespace    | testsAdo      | cxflowtestuser  |
      | testsAdo | master |              |                | testsAdo      | cxflowtestuser  | 

 