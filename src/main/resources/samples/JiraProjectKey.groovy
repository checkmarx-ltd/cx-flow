@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

import com.checkmarx.flow.dto.ScanRequest

println("------------- Groovy script execution started --------------------")
println("Running groovy script for Jira project key")

String jiraProjectKey = request.getBugTracker().getProjectKey()
println("Jira project key set to: " + jiraProjectKey)
return jiraProjectKey