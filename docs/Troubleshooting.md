## Common Issues
This section refers to common issues that users are facing.

### SSL/TLS
If any of the connecting components are using an internal CA or self-signed certificate, the Java Runtime in use must trust the appropriate certificates.  Information on this can be found in the JRE folder of the installed Java Runtime, and there under **lib/security**. The Java trust store is typically named **cacerts** and has a default passphrase of **changeit** - note, this is outside of the control of Checkmarx and CxFlow.

### XML Encoding
There have been cases when the Checkmarx REST API responds with XML that is not well encoded, and some defensive programming logic has been introduced to remove non-UTF-8 compatible characters.  This issue surfaces from time to time and detailed logging is required to find the exact problem - See below.

### Debugging
Debug logs can be enabled by adding the following configuration to the yaml (_application.yml_, or _application-\<profile\>.yml_) properties or by adding additional command line arguments when launching.

#### YAML Configuration
```
logging:
  file:
    name: cx-flow.log
  level:
    com:
       checkmarx: TRACE
    org:
       apache:
          http:
             wire: TRACE
       springframework:
          web:
             client:
                RestTemplate: TRACE
```
**Note:** including the `wire` and `RestTemplate` sections (as in the example above) will produce lots of log output, including full contents of HTTP requests. Please be aware that such logs may expose sensitive data. If you don't want the log to be this detailed, just remove the whole `logging.level.org` section.   

#### Log Grouping
Events that drive scanning/results feedback are given a unique ID and that ID is passed through the various logs to ensure you can make a link between all of the events.

```
2019-06-23 20:06:53.606  INFO 19980 --- [  restartedMain] c.c.f.CxFlowRunner  [u9UzT5SU] : Executing scan process
2019-06-23 20:06:53.607  INFO 19980 --- [  restartedMain] c.c.f.CxFlowRunner  [u9UzT5SU] : Initiating scan using Checkmarx git clone
2019-06-23 20:06:53.607  INFO 19980 --- [  restartedMain] c.c.f.CxFlowRunner  [u9UzT5SU] : Git url: https://github.com/Custodela/Riches.git
2019-06-23 20:06:53.614  INFO 19980 --- [  restartedMain] c.c.f.s.CxService   [u9UzT5SU] : Logging into Checkmarx http://localhost:8100/cxrestapi/auth/identity/connect/token
2019-06-23 20:06:53.730  INFO 19980 --- [  restartedMain] c.c.f.s.CxService   [u9UzT5SU] : Retrieving Cx presets
2019-06-23 20:06:53.748  INFO 19980 --- [  restartedMain] c.c.f.s.CxService   [u9UzT5SU] : Found preset Checkmarx Default with ID 36
2019-06-23 20:06:53.748  INFO 19980 --- [  restartedMain] c.c.f.s.CxService   [u9UzT5SU] : Retrieving Cx engineConfigurations
2019-06-23 20:06:53.755  INFO 19980 --- [  restartedMain] c.c.f.s.CxService   [u9UzT5SU] : Found xml/engine configuration Default Configuration with ID 1
2019-06-23 20:06:53.755  INFO 19980 --- [  restartedMain] c.c.f.s.FlowService [u9UzT5SU] : Overriding team with \CxServer\SP\Checkmarx\Custodela1111
```
**Note**: **u9UzT5SU** is the unique event ID in this sample.

#### Command Line Arguments
```
<cx-flow command> --logging.level.org.springframework.web.client.RestTemplate=TRACE 
--logging.level.com.checkmarx.flow.service=DEBUG --logging.level.org.apache.http.wire=TRACE
```
##### Sample Output
```
2019-05-02 10:45:53.052  INFO 23472 --- [  restartedMain] com.checkmarx.flow.CxFlowApplicationCmd  : Starting CxFlowApplicationCmd on xxxxxx with PID 23472 (started by xxxxxx)
2019-05-02 10:45:53.053  INFO 23472 --- [  restartedMain] com.checkmarx.flow.CxFlowApplicationCmd  : The following profiles are active: cmd
2019-05-02 10:45:53.122  INFO 23472 --- [  restartedMain] s.c.a.AnnotationConfigApplicationContext : Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@1cf0bc67: startup date [Thu May 02 10:45:53 EDT 2019]; root of context hierarchy
2019-05-02 10:45:55.161  INFO 23472 --- [  restartedMain] ptablePropertiesBeanFactoryPostProcessor : Post-processing PropertySource instances
2019-05-02 10:45:55.286  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource configurationProperties [org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertySource] to AOP Proxy
2019-05-02 10:45:55.288  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource commandLineArgs [org.springframework.core.env.SimpleCommandLinePropertySource] to EncryptableEnumerablePropertySourceWrapper
2019-05-02 10:45:55.290  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource systemProperties [org.springframework.core.env.MapPropertySource] to EncryptableMapPropertySourceWrapper
2019-05-02 10:45:55.290  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource systemEnvironment [org.springframework.boot.env.SystemEnvironmentPropertySourceEnvironmentPostProcessor$OriginAwareSystemEnvironmentPropertySource] to EncryptableMapPropertySourceWrapper
2019-05-02 10:45:55.291  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource random [org.springframework.boot.env.RandomValuePropertySource] to EncryptablePropertySourceWrapper
2019-05-02 10:45:55.291  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource applicationConfig: [classpath:/application-cmd.yml] [org.springframework.boot.env.OriginTrackedMapPropertySource] to EncryptableMapPropertySourceWrapper
2019-05-02 10:45:55.291  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource applicationConfig: [classpath:/application.yml] [org.springframework.boot.env.OriginTrackedMapPropertySource] to EncryptableMapPropertySourceWrapper
2019-05-02 10:45:55.291  INFO 23472 --- [  restartedMain] c.u.j.EncryptablePropertySourceConverter : Converting PropertySource refresh [org.springframework.core.env.MapPropertySource] to EncryptableMapPropertySourceWrapper
2019-05-02 10:45:55.430  INFO 23472 --- [  restartedMain] c.u.j.filter.DefaultLazyPropertyFilter   : Property Filter custom Bean not found with name 'encryptablePropertyFilter'. Initializing Default Property Filter
2019-05-02 10:45:55.522  INFO 23472 --- [  restartedMain] c.u.j.r.DefaultLazyPropertyResolver      : Property Resolver custom Bean not found with name 'encryptablePropertyResolver'. Initializing Default Property Resolver
2019-05-02 10:45:55.524  INFO 23472 --- [  restartedMain] c.u.j.d.DefaultLazyPropertyDetector      : Property Detector custom Bean not found with name 'encryptablePropertyDetector'. Initializing Default Property Detector
2019-05-02 10:45:55.964  INFO 23472 --- [  restartedMain] o.s.oxm.jaxb.Jaxb2Marshaller             : Creating JAXBContext with context path [checkmarx.wsdl.portal]
2019-05-02 10:45:57.868  INFO 23472 --- [  restartedMain] o.s.ws.soap.saaj.SaajSoapMessageFactory  : Creating SAAJ 1.3 MessageFactory with SOAP 1.1 Protocol
2019-05-02 10:45:58.536  INFO 23472 --- [  restartedMain] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService
2019-05-02 10:45:58.546  INFO 23472 --- [  restartedMain] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService  'webHook'
2019-05-02 10:45:58.550  INFO 23472 --- [  restartedMain] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService
2019-05-02 10:45:58.551  INFO 23472 --- [  restartedMain] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService  'scanRequest'
2019-05-02 10:45:59.730  INFO 23472 --- [  restartedMain] o.s.b.d.a.OptionalLiveReloadServer       : LiveReload server is running on port 35729
2019-05-02 10:45:59.765  INFO 23472 --- [  restartedMain] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2019-05-02 10:45:59.785  INFO 23472 --- [  restartedMain] com.checkmarx.flow.CxFlowApplicationCmd  : Started CxFlowApplicationCmd in 7.643 seconds (JVM running for 9.696)
2019-05-02 10:45:59.793  INFO 23472 --- [  restartedMain] com.checkmarx.flow.CxFlowApplicationCmd  : Using custom bean implementation  for bug tracking
2019-05-02 10:45:59.805  INFO 23472 --- [  restartedMain] com.checkmarx.flow.service.CxService     : Logging into Checkmarx http://localhost:8100/cxrestapi/auth/identity/connect/token
2019-05-02 10:45:59.830 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Created POST request for "http://localhost:8100/cxrestapi/auth/identity/connect/token"
2019-05-02 10:45:59.873 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Setting request Accept header to [application/json, application/*+json]
2019-05-02 10:45:59.874 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Writing [{username=[admin], password=[*****], grant_type=[password], scope=[sast_rest_api], client_id=[resource_owner_client], client_secret=[014DF517-39D1-4453-B7B3-9930C563627C]}] as "application/x-www-form-urlencoded" using [org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter@7ddc1774]
2019-05-02 10:45:59.909 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : POST request for "http://localhost:8100/cxrestapi/auth/identity/connect/token" resulted in 200 (OK)
2019-05-02 10:45:59.910 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Reading [class com.checkmarx.flow.dto.cx.CxAuthResponse] as "application/json;charset=utf-8" using [org.springframework.http.converter.json.MappingJackson2HttpMessageConverter@3c27337d]
2019-05-02 10:45:59.931  INFO 23472 --- [  restartedMain] com.checkmarx.flow.service.CxService     : Retrieving Cx teams
2019-05-02 10:45:59.933 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Created GET request for "http://localhost:8100/cxrestapi/auth/teams"
2019-05-02 10:45:59.938 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Setting request Accept header to [application/json, application/*+json]
2019-05-02 10:45:59.945 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : GET request for "http://localhost:8100/cxrestapi/auth/teams" resulted in 200 (OK)
2019-05-02 10:45:59.945 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Reading [class [Lcom.checkmarx.flow.dto.cx.CxTeam;] as "application/json;charset=utf-8" using [org.springframework.http.converter.json.MappingJackson2HttpMessageConverter@3c27337d]
2019-05-02 10:45:59.950  INFO 23472 --- [  restartedMain] com.checkmarx.flow.service.CxService     : Found team \CxServer\SP\Checkmarx\Automation\custodela-test with ID eb247bd3-a465-4ec3-9d5a-63e624173979
2019-05-02 10:45:59.951 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Created GET request for "http://localhost:8100/cxrestapi/projects?projectName=riches.net-master&amp;teamId=eb247bd3-a465-4ec3-9d5a-63e624173979"
2019-05-02 10:45:59.951 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Setting request Accept header to [text/plain, text/plain, application/json, application/*+json, */*, */*]
2019-05-02 10:45:59.959 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : GET request for "http://localhost:8100/cxrestapi/projects?projectName=riches.net-master&amp;teamId=eb247bd3-a465-4ec3-9d5a-63e624173979" resulted in 200 (OK)
2019-05-02 10:45:59.960 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Reading [java.lang.String] as "application/json;charset=utf-8" using [org.springframework.http.converter.StringHttpMessageConverter@5304343b]
2019-05-02 10:45:59.963 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Created GET request for "http://localhost:8100/cxrestapi/projects/20048"
2019-05-02 10:45:59.971 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Setting request Accept header to [application/json, application/*+json]
2019-05-02 10:45:59.980 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : GET request for "http://localhost:8100/cxrestapi/projects/20048" resulted in 200 (OK)
2019-05-02 10:45:59.980 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Reading [class com.checkmarx.flow.dto.cx.CxProject] as "application/json;charset=utf-8" using [org.springframework.http.converter.json.MappingJackson2HttpMessageConverter@3c27337d]
2019-05-02 10:45:59.981  INFO 23472 --- [  restartedMain] com.checkmarx.flow.service.CxService     : Finding last Scan Id for project Id 20048`
2019-05-02 10:45:59.982 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Created GET request for "http://localhost:8100/cxrestapi/sast/scans?projectId=20048&amp;scanStatus=7&amp;last=1"
2019-05-02 10:45:59.983 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Setting request Accept header to [text/plain, text/plain, application/json, application/*+json, */*, */*]
2019-05-02 10:46:00.010 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : GET request for "http://localhost:8100/cxrestapi/sast/scans?projectId=20048&amp;scanStatus=7&amp;last=1" resulted in 200 (OK)
2019-05-02 10:46:00.011 DEBUG 23472 --- [  restartedMain] o.s.web.client.RestTemplate              : Reading [java.lang.String] as "application/json;charset=utf-8" using [org.springframework.http.converter.StringHttpMessageConverter@5304343b]
2019-05-02 10:46:00.012  INFO 23472 --- [  restartedMain] com.checkmarx.flow.service.CxService     : Scan found with Id 1020148 for project Id 20048
2019-05-02 10:46:00.025  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Creating report for xml Id 1020148
2019-05-02 10:46:00.027 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Created POST request for "http://localhost:8100/cxrestapi/reports/sastScan"
2019-05-02 10:46:00.027 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Setting request Accept header to [text/plain, text/plain, application/json, application/*+json, */*, */*]
2019-05-02 10:46:00.027 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Writing [{'reportType':'XML', 'scanId':1020148}] as "application/json;charset=UTF-8" using [org.springframework.http.converter.StringHttpMessageConverter@5304343b]
2019-05-02 10:46:00.180 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : POST request for "http://localhost:8100/cxrestapi/reports/sastScan" resulted in 202 (Accepted)
2019-05-02 10:46:00.180 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Reading [java.lang.String] as "application/json;charset=utf-16" using [org.springframework.http.converter.StringHttpMessageConverter@5304343b]
2019-05-02 10:46:00.181  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Report with Id 2267 created
2019-05-02 10:46:20.181  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Retrieving report status of report Id 2267`
2019-05-02 10:46:20.182 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Created GET request for "http://localhost:8100/cxrestapi/reports/sastScan/2267/status"
2019-05-02 10:46:20.182 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Setting request Accept header to [text/plain, text/plain, application/json, application/*+json, */*, */*]
2019-05-02 10:46:20.258 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : GET request for "http://localhost:8100/cxrestapi/reports/sastScan/2267/status" resulted in 200 (OK)
2019-05-02 10:46:20.259 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Reading [java.lang.String] as "application/json;charset=utf-8" using [org.springframework.http.converter.StringHttpMessageConverter@5304343b]
2019-05-02 10:46:20.259 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Report status is 2 - Created for report Id 2267
2019-05-02 10:46:40.369  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Retrieving report contents of report Id 2267 in XML format
2019-05-02 10:46:40.370 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Created GET request for "http://localhost:8100/cxrestapi/reports/sastScan/2267"
2019-05-02 10:46:40.370 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Setting request Accept header to [text/plain, text/plain, application/json, application/*+json, */*, */*]
2019-05-02 10:46:40.383 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : GET request for "http://localhost:8100/cxrestapi/reports/sastScan/2267" resulted in 200 (OK)
2019-05-02 10:46:40.383 DEBUG 23472 --- [  scan-results1] o.s.web.client.RestTemplate              : Reading [java.lang.String] as "application/xml" using [org.springframework.http.converter.StringHttpMessageConverter@5304343b]
2019-05-02 10:46:40.393 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Headers:
2019-05-02 10:46:40.394 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : {Cache-Control=no-cache, Pragma=no-cache, Content-Type=application/xml, Expires=-1, Server=Microsoft-IIS/10.0, api-version=1.0, X-AspNet-Version=4.0.30319, X-Powered-By=ASP.NET, Date=Thu, 02 May 2019 14:46:40 GMT, Content-Length=431378}
2019-05-02 10:46:40.394  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Report downloaded for report Id 2267
2019-05-02 10:46:40.394 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : XML String Output:
2019-05-02 10:46:40.394 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : <FULL XML INSERTED HERE>
2019-05-02 10:46:40.406 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : Base64:
2019-05-02 10:46:40.417 DEBUG 23472 --- [  scan-results1] com.checkmarx.flow.service.CxService     : <BASE64 ENCODED XML RESPONSE FROM CHECKMARX>
2019-05-02 10:46:40.524 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 4
2019-05-02 10:46:40.546 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 5
2019-05-02 10:46:40.563 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 6
2019-05-02 10:46:40.576 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 7
2019-05-02 10:46:40.592 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 8
2019-05-02 10:46:40.607 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 9
2019-05-02 10:46:40.622 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 10
2019-05-02 10:46:40.638 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 11
2019-05-02 10:46:40.652 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 12
2019-05-02 10:46:40.668 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 13
2019-05-02 10:46:40.686 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 14
2019-05-02 10:46:40.706 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 15
2019-05-02 10:46:40.725 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 16
2019-05-02 10:46:40.740 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 17
2019-05-02 10:46:40.756 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 18
2019-05-02 10:46:40.771 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 19
2019-05-02 10:46:40.787 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 20
2019-05-02 10:46:40.806 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 21
2019-05-02 10:46:40.821 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 48
2019-05-02 10:46:40.837 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 49
2019-05-02 10:46:40.852 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 50
2019-05-02 10:46:40.868 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 51
2019-05-02 10:46:40.884 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 52
2019-05-02 10:46:40.899 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 53
2019-05-02 10:46:40.914 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 1
2019-05-02 10:46:40.929 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 2
2019-05-02 10:46:40.944 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 3
2019-05-02 10:46:40.958 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 22
2019-05-02 10:46:40.972 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 23
2019-05-02 10:46:40.988 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 24
2019-05-02 10:46:41.017 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 54
2019-05-02 10:46:41.033 DEBUG 23472 --- [  scan-results1] c.c.flow.service.CxLegacyService         : Retrieving description for 1020148 / 55
2019-05-02 10:46:41.051  INFO 23472 --- [  scan-results1] c.checkmarx.flow.service.ResutlsService  : Issue tracking is custom bean implementation
2019-05-02 10:46:41.057  INFO 23472 --- [  scan-results1] c.c.flow.custom.JsonIssueTracker         : Creating file C:\tmp/CxServer_SP_Checkmarx_Automation_custodela-test-riches.net-master-20190502.104641.json
2019-05-02 10:46:41.057  INFO 23472 --- [  scan-results1] c.c.flow.custom.JsonIssueTracker         : Deleting if already exists
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Processing Issues with custom bean Json
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Admin/Newsletter.aspx.cs
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Account/Register.aspx
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Anonymous/FindLocations.aspx.cs
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/App_Code/Components/ProfileDB.cs
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/App_Code/Components/AccountDB.cs
2019-05-02 10:46:41.059  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/404.aspx.cs
2019-05-02 10:46:41.060  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Users/ViewMessage.aspx
2019-05-02 10:46:41.060  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/App_Code/Restful/RestfulServices.cs
2019-05-02 10:46:41.060  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Admin/Admin.aspx.cs
2019-05-02 10:46:41.060  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Users/AccountDetails.aspx.cs
2019-05-02 10:46:41.060  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Users/Transfer.aspx.cs
2019-05-02 10:46:41.060  INFO 23472 --- [  scan-results1] com.checkmarx.flow.service.IssueService  : Creating new issue with key RichesDotnet/Users/AdminControlPage.aspx.cs
2019-05-02 10:46:41.102  INFO 23472 --- [  scan-results1] c.checkmarx.flow.service.ResutlsService  : Process completed Succesfully
2019-05-02 10:46:41.103 ERROR 23472 --- [  restartedMain] com.checkmarx.flow.CxFlowApplicationCmd  : Exiting with Error code 10 due to issues present
2019-05-02 10:46:41.104  INFO 23472 --- [      Thread-24] s.c.a.AnnotationConfigApplicationContext : Closing org.springframework.context.annotation.AnnotationConfigApplicationContext@1cf0bc67: startup date [Thu May 02 10:45:53 EDT 2019]; root of context hierarchy
Disconnected from the target VM, address: '127.0.0.1:61601', transport: 'socket
2019-05-02 10:46:41.165  INFO 23472 --- [      Thread-24] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
2019-05-02 10:46:41.166  INFO 23472 --- [      Thread-24] o.s.s.concurrent.ThreadPoolTaskExecutor  : Shutting down ExecutorService 'scanRequest
2019-05-02 10:46:41.166  INFO 23472 --- [      Thread-24] o.s.s.concurrent.ThreadPoolTaskExecutor  : Shutting down ExecutorService 'webHook
```

##### JDK Version
If you run the Java 8 bundled version of CxFlow, but are using a system JRE/JDK of 10+, you have unsatisfied dependencies regarding SAX/JAXB parsing objects.  This is due to the fact that they have been removed in JRE 10 onward.  The message looks as illustrated below.Simply ensure that the correct JAR/JRE is leveraged to avoid this.  The need to maintain separate builds per JRE is linked to the use of SOAP based APIs for Checkmarx, of which only three APIs are still in use (Login, GetIssueDescription, CreateTeam).  The XML parsing is required as well for ingesting the XML results from Checkmarx.  If both move to a REST/JSON based API request/response model, the dependency is removed.

```
Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2019-05-16 16:51:26.228 ERROR 80706 --- [           main] o.s.boot.SpringApplication               : Application run failed
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'cxFlowApplication': Unsatisfied dependency expressed through ...
...`
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'cxLegacyService' defined in URL [jar:file:/Users/mixu94/checkmarx/cx-flow-cmd-1.2.jar!/BOOT-INF/classes!/com/checkmarx/flow/service/CxLegacyService.class]: Unsatisfied dependency expressed through constructor parameter 1; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name webServiceTemplate' defined in class path resource [com/checkmarx/flow/config/CxWSConfig.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.client.core.WebServiceTemplate]: Factory method 'webServiceTemplate' threw exception; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.soap.saaj.SaajSoapMessageFactory]: Unresolvable class definition; nested exception is java.lang.NoClassDefFoundError: javax/xml/soap/SOAPException
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:732) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:197) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1267) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1124) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:535) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:495) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:317) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:222) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:315) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:199) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]`
at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:251) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1135) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1062) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:818) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:724) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
... 52 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'webServiceTemplate' defined in class path resource [com/checkmarx/flow/config/CxWSConfig.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.client.core.WebServiceTemplate]: Factory method webServiceTemplate' threw exception; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.soap.saaj.SaajSoapMessageFactory]: Unresolvable class definition; nested exception is java.lang.NoClassDefFoundError: javax/xml/soap/SOAPException
at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:590) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1247) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1096) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:535) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:495) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:317) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:222) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:315) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:199) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]`
at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:251) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1135) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1062) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:818) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:724) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
... 66 common frames omitted
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.client.core.WebServiceTemplate]: Factory method 'webServiceTemplate' threw exception; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.soap.saaj.SaajSoapMessageFactory]: Unresolvable class definition; nested exception is java.lang.NoClassDefFoundError: javax/xml/soap/SOAPException
at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:185) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:582) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
... 79 common frames omitted
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.ws.soap.saaj.SaajSoapMessageFactory]: Unresolvable class definition; nested exception is java.lang.NoClassDefFoundError: javax/xml/soap/SOAPException
at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:130) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.ws.support.DefaultStrategiesHelper.instantiateBean(DefaultStrategiesHelper.java:152) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at org.springframework.ws.support.DefaultStrategiesHelper.getDefaultStrategies(DefaultStrategiesHelper.java:134) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at org.springframework.ws.support.DefaultStrategiesHelper.getDefaultStrategy(DefaultStrategiesHelper.java:219) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at org.springframework.ws.support.DefaultStrategiesHelper.getDefaultStrategy(DefaultStrategiesHelper.java:203) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at org.springframework.ws.client.core.WebServiceTemplate.initMessageFactory(WebServiceTemplate.java:353) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at org.springframework.ws.client.core.WebServiceTemplate.initDefaultStrategies(WebServiceTemplate.java:342) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at org.springframework.ws.client.core.WebServiceTemplate.<init>(WebServiceTemplate.java:130) ~[spring-ws-core-3.0.3.RELEASE.jar!/:na]
at com.checkmarx.flow.config.CxWSConfig.webServiceTemplate(CxWSConfig.java:31) ~[classes!/:na]
at com.checkmarx.flow.config.CxWSConfig$$EnhancerBySpringCGLIB$$e501ace4.CGLIB$webServiceTemplate$1(<generated>) ~[classes!/:na]
at com.checkmarx.flow.config.CxWSConfig$$EnhancerBySpringCGLIB$$e501ace4$$FastClassBySpringCGLIB$$8ee2f8a3.invoke(<generated>) ~[classes!/:na]
at org.springframework.cglib.proxy.MethodProxy.invokeSuper(MethodProxy.java:228) ~[spring-core-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(ConfigurationClassEnhancer.java:365) ~[spring-context-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
at com.checkmarx.flow.config.CxWSConfig$$EnhancerBySpringCGLIB$$e501ace4.webServiceTemplate(<generated>) ~[classes!/:na]
at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:na]
at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
at java.base/java.lang.reflect.Method.invoke(Method.java:566) ~[na:na]
at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:154) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
... 80 common frames omitted
Caused by: java.lang.NoClassDefFoundError: javax/xml/soap/SOAPException
at java.base/java.lang.Class.getDeclaredConstructors0(Native Method) ~[na:na]
at java.base/java.lang.Class.privateGetDeclaredConstructors(Class.java:3138) ~[na:na]
at java.base/java.lang.Class.getConstructor0(Class.java:3343) ~[na:na]
at java.base/java.lang.Class.getDeclaredConstructor(Class.java:2554) ~[na:na]
at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:123) ~[spring-beans-5.0.9.RELEASE.jar!/:5.0.9.RELEASE]
... 98 common frames omitted Caused by: java.lang.ClassNotFoundException: javax.xml.soap.SOAPException
at java.base/java.net.URLClassLoader.findClass(URLClassLoader.java:471) ~[na:na]
at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:588) ~[na:na]
at org.springframework.boot.loader.LaunchedURLClassLoader.loadClass(LaunchedURLClassLoader.java:93) ~[cx-flow-cmd-1.2.jar:na]
at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:521) ~[na:na]
... 103 common frames omitted
```

