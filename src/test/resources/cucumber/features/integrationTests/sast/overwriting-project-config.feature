@Integration
Feature: CxFlow should preserve SAST project settings if the project already exists in SAST

  Scenario Outline: Preserving SAST project settings for an existing project
    Given "<project name>" project exists in SAST
    And all of "<initial preset>", "<initial config>", "<global preset>", "<global config>" exist in SAST
    And project has the "<initial preset>" preset and the "<initial config>" scan configuration
    And CxFlow config has the "<global preset>" preset and the "<global config>" scan configuration
    And GitHub repository does not contain a config-as-code file
    When GitHub notifies CxFlow about a pull request for the "<project name>" project
    And CxFlow starts a SAST scan
    Then project preset is still "<initial preset>" and scan configuration is "<initial config>"

    Examples:
      | project name                 | initial preset  | initial config      | global preset     | global config         |
      | OverwritingProjectConfigTest | High and Medium | Multi-language Scan | Checkmarx Default | Default Configuration |