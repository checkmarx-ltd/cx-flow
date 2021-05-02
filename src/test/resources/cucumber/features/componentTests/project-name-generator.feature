Feature: Scanner project name generation

  Scenario Outline: Project name generation with different values of preserve-project-Name
    Given preserve-project-name is "<preserve>" and is-multi-tenant true
    When scan request arrives with repo-name "<repo-name>" and branch is 'master'
    Then project name used by scanner is "<project-name>"

    Examples:
      | preserve | repo-name      | project-name            |
      | false    | ch-eck^ marx@* | ch-eck--marx-----master |
      | true     | ch-eck^ marx@* | ch-eck^ marx@* -master  |

  Scenario Outline: Project name generation with different params
    Given is-multi-tenant "<is-multi-tenant>"
    When scan request arrives with namespace "<namespace>", repo-name "<repo-name>", branch "<branch>" and application "<application>"
    Then project name used by scanner is "<project-name>"

    Examples:
      | namespace     | repo-name | branch | application           | is-multi-tenant | project-name                 |
      | checkmarx-ltd | cx-flow   | ""     |  N/A                  | true            | cx-flow                      |
      | checkmarx-ltd | cx-flow   | master |  N/A                  | true            | cx-flow-master               |
      | checkmarx-ltd | cx-flow   | master |  N/A                  | false           | checkmarx-ltd-cx-flow-master |
      | ""            | ""        | ""     | checkmarx-application | false           | checkmarx-application        |

