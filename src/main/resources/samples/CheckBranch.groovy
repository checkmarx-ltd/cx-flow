@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )

import groovyx.net.http.HTTPBuilder

println "Checking 'request' object for details and determine if scan is applicable for this branch (target or current)"
//must be boolean
return true