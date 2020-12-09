@PullRequestUpdateComment @Integration
  Feature: After scan that was initiated by pull request, CxFlow should update the PR comments, instead of creating new one.

    Scenario Outline: CxFlow should create new comments on Github Pull request with scan results summary
      Given scanner is set to "<scanner>"
      Given source control is GitHub
      And no comments on pull request
      When pull request arrives to CxFlow
      Then Wait for comments
      And verify new comments

      Examples:
        | scanner |
        | sca     |
        | sast    |
        | both    |


    Scenario Outline: CxFlow should update existing comments on Github Pull request with scan results summary
      Given scanner is set to "<scanner>"
      Given source control is GitHub
      And no comments on pull request
      And pull request arrives to CxFlow
      And Wait for comments
      And different filters configuration is set
      When pull request arrives to CxFlow
      Then Wait for updated comment

      Examples:
        | scanner |
        | sca    |
        | sast   |
        | both    |

    Scenario Outline: CxFlow should create new comments on Github Pull request with scan results summary
      Given scanner is set to "<scanner>"
      Given source control is ADO
      And no comments on pull request
      When pull request arrives to CxFlow
      Then Wait for comments
      And verify new comments

      Examples:
        | scanner |
        | sca    |
        | sast   |
        | both    |

    Scenario Outline: ADO Pull request arrives to CxFlow, then scan is initiated, and pull request comments should be updated.
      Given scanner is set to "<scanner>"
      Given source control is ADO
      And no comments on pull request
      And pull request arrives to CxFlow
      And Wait for comments
      When pull request arrives to CxFlow
      Then Wait for comments
      And Wait for updated comment

      Examples:
        | scanner |
        | sca    |
        | sast   |
        | both    |
