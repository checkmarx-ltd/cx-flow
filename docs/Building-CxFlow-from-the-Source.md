## Source
The source can be found [here](https://github.com/checkmarx-ltd/cx-flow.git).

## Compiling
CxFlow uses Gradle for building the applicable jar files.  There are 2 Gradle build files:
* build.gradle → used to build cx-flow-<ver>.jar for WebHook Webservice with JRE 8
* build-11.gradle → used to build cx-flow-11-<ver>.jar for WebHook Webservice with JRE 11

**Note**:  JRE 11 requires a special build dependency configuration to allow for JAXB SOAP classes that are only required for the specific calls to Checkmarx legacy SOAP.

### Compile
`gradlew -b <gradle build file> clean build`
A directory structure is created (_build/libs/_) to which the jar is compiled.

### Pre-Built Binary
The latest compiled releases can be found [here](https://github.com/checkmarx-ts/cx-flow/releases). Look for the latest release.

The archive (release.zip) contains two jar files in the build/libs folder: 
* **cx-flow-<ver>.jar** → used for WebHook Webservice using JRE 8.  The entry point contains a tomcat container that launches
* **cx-flow-11-<ver>.jar** → used for WebHook Webservice using JRE 11.  The entry point contains a tomcat container that launches