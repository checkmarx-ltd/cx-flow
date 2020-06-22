@PullRequestUpdateComment @Integration
  Feature: After scan that was initiated by pull request, CxFlow should update the PR comments, instead of creating new one.

    Scenario: Pull request arrives to CxFlow, then scan is initiated, pull request should be without comments, and we should verify that the comments are new.
      Given source control is GitHub
      Given branch is pr-comments-tests
      And no comments on pull request
      When pull request arrives to CxFlow
      Then Wait for comments
      And verify 2 new comments

    Scenario: Pull request arrives to CxFlow, then scan is initiated, and pull request comments should be updated. number of comments should be 2.
      Given source control is GitHub
      Given branch is pr-comments-tests
      And no comments on pull request
      And pull request arrives to CxFlow
      And Wait for comments
      And different filters configuration is set
      When pull request arrives to CxFlow
      Then Wait for updated comment

    Scenario: Pull request arrives from ADO to CxFlow, then scan is initiated, pull request should be without comments, and we should verify that the comments are new.
      Given source control is ADO
      Given branch is udi-test-2
      And no comments on pull request
      When pull request arrives to CxFlow
      Then Wait for comments
      And verify 2 new comments

    Scenario: ADO Pull request arrives to CxFlow, then scan is initiated, and pull request comments should be updated. number of comments should be 2
      Given source control is ADO
      Given branch is udi-test-2
      And no comments on pull request
      And pull request arrives to CxFlow
      And Wait for comments
      When pull request arrives to CxFlow
      Then Wait for comments
      And Wait for updated comment


