@CxConfigBugTrackerFeature

  Feature: CxFlow can get a different bug tracker implementation from config-as-code configuration file.

    Scenario: Cx Flow receives a pull request webhook, and in the config-as-code configuration file, GitHub is defined as bug tracker. bug tracker should be GITHUBPULL
      Given github branch is udi-tests
      When pull request webhook arrives
      Then scan request should have GITHUBPULL bug tracker


    Scenario: CxFlow receives a push event and in the config-as-code configuration file, GitHub is defined as bug tracker, bug tracker should be GitHub
      Given github branch is udi-tests
      When push event arrives
      Then scan request should have GitHub bug tracker


