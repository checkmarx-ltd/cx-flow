@DeleteBranchFeature @ComponentTest
Feature: CxFlow should delete SAST project when corresponding GitHub branch is deleted
  
  Scenario Outline: CxFlow SAST project when GitHub branch is deleted
    Given GitHub repoName is "<repoName>"
    And GitHub webhook is configured for delete branch or tag
    And github branch is "<branch>" and it is set "<set_in_app>" application.yml
    And a project "<projectName>" "<exists>" in SAST
    Then CxFlow will call or not call the SAST delete API based on the fact whether the project "<exists>" or not in SAST
    And SAST delete API will be called for project "<projectName>"
    And no exception will be thrown

    Examples:
      | repoName | branch | projectName   | exists | set_in_app |
      | VB_3845  | test1  | VB_3845-test1 | true   | false      |
      | VB_3845  | test1  | VB_3845-test1 | true   | true       |
      | VB_3845  | test1  | VB_3845-test1 | false  | false      |
    
  Scenario Outline: Github triggers delete event both when branch and tag are deleted,
                    but CxFlow will call SAST delete API only when branch is deleted
    Given GitHub repoName is "<repoName>"
    And GitHub webhook is configured for delete branch or tag
    And github trigger can be branch or tag "<trigger>" 
    And a project "<projectName>" "<exists>" in SAST
    Then CxFlow will call the SAST delete API only if trigger is branch
    And no exception will be thrown

    Examples:
      | repoName | trigger | exists |
      | VB_3845  | branch  | true   |
      | VB_3845  | tag     | true   |
  