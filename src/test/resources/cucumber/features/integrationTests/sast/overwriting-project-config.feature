Feature: CxFlow should preserve SAST project settings if the project already exists in SAST

  Scenario Outline: Preserving SAST project settings for an existing project
    Given "<project name>" project exists in SAST
    And project preset is "<initial preset>"
    And project configuration is "<initial config>"
    And SAST configuration is set to "<new config>" in CxFlow config
    And SAST preset is set to "<new preset>" in CxFlow config
    And all of "<initial preset>", "<initial config>", "<new preset>", "<new config>" exist in SAST
    When GitHub notifies CxFlow about a pull request for "<project name>"
    And 'preset' parameter is not specified in GitHub request
    And GitHub repository does not contain a config-as-code file
    And CxFlow starts a SAST scan
    Then "<project name>" project still has "<initial preset>" preset
    And "<project name>" project still has "<initial config>" configuration

    Examples:
      | project name                 | initial preset  | initial config      | new preset        | new config            |
      | OverwritingProjectConfigTest | High and Medium | Multi-language Scan | Checkmarx Default | Default Configuration |