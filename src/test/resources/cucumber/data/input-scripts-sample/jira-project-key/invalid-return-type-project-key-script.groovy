import com.checkmarx.flow.dto.ScanRequest

println("------------- Groovy script execution started --------------------")
println("Running groovy script for Jira project key - invalid return type")

int jiraProjectKey = 5
println("Jira project key set to: Type 'int', Value " + jiraProjectKey)
return jiraProjectKey