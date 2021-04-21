@ConfigureSastComment @ComponentTest
Feature: Using Groovy script to configure SAST comment on scan request

  Scenario Outline: Configuring SAST comment using a script result
    Given given 'sast-comment' script name is '<comment script>'
    When CxFlow triggers SAST scan
    Then CxFlow scan comment is equal to '<comment>'


    Examples:
      | comment script                          | comment                                  |
      | sast-standard-comment                   | standard comment                         |
      | special-characters-comment-script       | comment_with-special/characters*$@!66 ^* |
      | invalid-return-type-comment-script      | CxFlow Automated Scan                    |
      | script-not-exist                        | CxFlow Automated Scan                    |
      | invalid-syntax-script-comment           | CxFlow Automated Scan                    |
      | empty                                   | CxFlow Automated Scan                    |


  Scenario: Configuring SAST comment using a script based on request data
    Given given 'sast-comment' script name is 'parse-branch-name-comment'
    When Scan request contains feature branch name 'Test-feature-branch'
    And CxFlow triggers SAST scan
    Then CxFlow scan comment is equal to 'script-prefix-Test-feature-branch'






