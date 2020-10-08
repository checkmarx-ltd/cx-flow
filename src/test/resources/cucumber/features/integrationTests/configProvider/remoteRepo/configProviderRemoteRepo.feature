@ConfigProviderRemoteRepoFeature
Feature: CxFlow should read configuration from a remote repo

  Scenario Outline: CxFlow should read vulnerability scanner configuration from a remote repo and initialize the scanner's config with the right values
    Given github repo contains a Checkmarx configuration
    When initializing config provider
    And getting "<scanner>" config provider configuration
    Then "<scanner>" configuration on Cx-Flow side should match the remote repo configuration data

    Examples:
      | scanner |
      | ast    |
      | sca    |