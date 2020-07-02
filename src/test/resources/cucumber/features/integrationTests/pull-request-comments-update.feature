@PullRequestUpdateComment @Integration
  Feature: After scan that was initiated by pull request, CxFlow should update the PR comments, instead of creating new one.

    Scenario Outline: Pull request arrives to CxFlow, then scan is initiated, pull request should be without comments, and we should verify that the comments are new.
      Given scanner is set to "<scanner>"
      Given source control is GitHub
      And no comments on pull request
      When pull request arrives to CxFlow
      Then Wait for comments
      And verify new comments

      Examples:
        | scanner |
        | sca    |
        | sast   |
        | both    |


    Scenario Outline: Pull request arrives to CxFlow, then scan is initiated, and pull request comments should be updated.
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

    Scenario Outline: Pull request arrives from ADO to CxFlow, then scan is initiated, pull request should be without comments, and we should verify that the comments are new.
      Given scanner is set to "<scanner>"
      Given source control is ADO
#      Given branch is udi-test-2
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
