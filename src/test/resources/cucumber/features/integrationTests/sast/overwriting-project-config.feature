Feature: CxFlow should preserve SAST project settings if the project already exists in SAST

  Scenario Outline: Preserving SAST project settings for an existing project
    Given "OverwritingProjectConfigTest" project exists in SAST
    And all of "<initial preset>", "<initial config>", "<global preset>", "<global config>" exist in SAST
    And project preset is "<initial preset>" and scan configuration is "<initial config>"
    And SAST configuration is set to "<global config>" in CxFlow config
    And SAST preset is set to "<global preset>" in CxFlow config
    And GitHub repository does not contain a config-as-code file
    When GitHub notifies CxFlow about a pull request without overriding the 'preset' parameter
    And CxFlow starts a SAST scan
    And project preset is still "<initial preset>" and scan configuration is "<initial config>"

    Examples:
      | initial preset  | initial config      | global preset     | global config         |
      | High and Medium | Multi-language Scan | Checkmarx Default | Default Configuration |