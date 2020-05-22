@CxConfigBugTrackerFeature

  Feature: CxFlow can get a different bug tracker implementation from config-as-code configuration file.

    Scenario: Cx Flow receives a pull request webhook, and in the config-as-code configuration file, GitHub is defined as bug tracker. corresponding issues shoud be seen in GitHub.
      Given github branch is udi-tests
      When pull request webhook arrives
      Then scan request should have CUSTOM bug tracker, and GitHub custom bean name


