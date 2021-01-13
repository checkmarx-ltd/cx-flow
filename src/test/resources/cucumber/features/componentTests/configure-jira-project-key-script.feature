@ConfigureJiraProjectKey @ComponentTest
Feature: Using Groovy script to configure Jira project key on bug tracker

  Scenario Outline: Configuring Jira project key using a script result
    Given given 'jira-project-key' script name is '<project key script>'
    When Determine JIRA project key
    Then JIRA project key is equal to '<project key>'


    Examples:
      | project key script                      | project key            |
      | jira-standard-project-key               | standard project key   |
      | invalid-return-type-project-key-script  | Default Project Key    |
      | invalid-syntax-project-key-script       | Default Project Key    |
      | script-not-exist                        | Default Project Key    |
      | empty                                   | Default Project Key    |

  Scenario: Configuring JIRA project key using a script based on request data
    Given given 'jira-project-key' script name is 'parse-jira-project-key'
    When Scan request contain feature repo name 'Test-feature-repo'
    And Determine JIRA project key
    Then JIRA project key is equal to 'script-prefix-Test-feature-repo'





