
import com.checkmarx.flow.dto.ScanRequest
import com.checkmarx.flow.utils.ScanUtils
import groovy.json.JsonSlurper

println("------------- Groovy script execution started --------------------")
println("Checking sast comment")

String branch = request.getBranch();
String targetBranch = request.getMergeTargetBranch();

String SAST_Comment = "script-prefix-" + branch;
return SAST_Comment;

