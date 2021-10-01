@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

import groovyx.net.http.HTTPBuilder

def repoUrl = request.getRepoUrl()
def branch = request.getBranch()
def commitId = request.getHash()

String scanComment = "Repo: $repoUrl | Branch: $branch | Commit ID: $commitId"

println "INFO : Scanning code from $scanComment"

return scanComment
