@DeleteBranchFeature @ComponentTest
Feature: CxFlow should delete SAST project when corresponding GitHub branch is deleted

  Scenario Outline: CxFlow deletes SAST project when a non-protected GitHub branch is deleted
    Given GitHub repo name is "VB_3845"
    And GitHub branch is "test1"
    And the "test1" branch is "<protected>" as determined by application.yml
    And a project "VB_3845-test1" "<exists>" in SAST
    When GitHub notifies cxFlow that a "test1" branch was deleted
    Then CxFlow will "<call>" the SAST delete API for the "VB_3845-test1" project
    And no exception will be thrown

    Examples:
      | exists | protected | call  |
      | true   | true      | false |
      | true   | false     | true  |
      | false  | true      | false |
      | false  | false     | false |

  Scenario Outline: Github triggers delete event both when branch and tag are deleted,
  but CxFlow will call SAST delete API only when branch is deleted
    Given GitHub repo name is "VB_3845"
    And GitHub trigger is "<trigger>"
    And the "test2" branch is "not protected" as determined by application.yml
    And a project "VB_3845-test2" "exists" in SAST
    When GitHub notifies cxFlow that a "test2" ref was deleted
    Then CxFlow will "<call>" the SAST delete API for the "VB_3845-test2" project
    And no exception will be thrown

    Examples:
      | trigger | call  |
      | branch  | true  |
      | tag     | false |