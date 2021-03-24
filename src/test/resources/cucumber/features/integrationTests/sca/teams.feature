Feature: Cx-Flow SCA teams tests

  Scenario Outline: Create project team and validated returned assignedTeams value
    Given scanner is SCA
    When creating a new project with associated "<team>" value
    Then project assignedTeams returned value is "<returned_value>"

    # empty team -> team: (team does exists, but without any value)
    # team = null -> team property doesn't exists in configuration

    Examples:
      | team             | returned_value   |
      |                  |                  |
      | null             |                  |
      | /CxServer        | /CxServer        |
      | /CxServer/MyTeam | /CxServer/MyTeam |