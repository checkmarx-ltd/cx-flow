Feature: Source file URL generation for issue trackers

  The file/line link embedded in a reported issue must point at the source file
  without a double slash, regardless of whether the repo URL ends with ".git",
  ".git/" or has no ".git" suffix. GitLab links must use the "/-/blob/" route.

  Scenario Outline: GitHub file URL has no double slash
    Given a scan request with repo-url "<repo-url>" and branch "main"
    When the file url is generated for "src/Foo.java" using the github tracker
    Then the generated file url is "<expected-url>"

    Examples:
      | repo-url                          | expected-url                                       |
      | https://github.com/org/repo.git   | https://github.com/org/repo/blob/main/src/Foo.java |
      | https://github.com/org/repo.git/  | https://github.com/org/repo/blob/main/src/Foo.java |
      | https://github.com/org/repo       | https://github.com/org/repo/blob/main/src/Foo.java |

  Scenario Outline: GitLab file URL uses /-/blob/ and has no double slash
    Given a scan request with repo-url "<repo-url>" and branch "main"
    When the file url is generated for "foo/bar/src/err.php" using the gitlab tracker
    Then the generated file url is "<expected-url>"

    Examples:
      | repo-url                                | expected-url                                                     |
      | https://gitlab.example.fr/project.git   | https://gitlab.example.fr/project/-/blob/main/foo/bar/src/err.php |
      | https://gitlab.example.fr/project.git/  | https://gitlab.example.fr/project/-/blob/main/foo/bar/src/err.php |
      | https://gitlab.example.fr/project       | https://gitlab.example.fr/project/-/blob/main/foo/bar/src/err.php |
