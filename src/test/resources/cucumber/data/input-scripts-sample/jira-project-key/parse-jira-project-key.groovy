import com.checkmarx.flow.dto.ScanRequest

println("------------- Groovy script execution started --------------------")
println("Running groovy script for Jira project key - parse using scan request repo name")

String repoName = request.getRepoName()
String jiraProjectKey = "script-prefix-" + repoName
println("Jira project key set to: " + jiraProjectKey)
return jiraProjectKey