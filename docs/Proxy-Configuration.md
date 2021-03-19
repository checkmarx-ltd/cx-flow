
CxFlow can use a proxy server for making HTTP/HTTPS connections to the internet by adding additional arguments to the startup command. Irrespective of which scan engine is used, the proxy configuration will remain the same.

The following arguments are required:

### 1)Using HTTP
http.proxyHost – the host or IP address of the proxy server.

http.proxyPort – the port used by the proxy server.

http.proxyUser - Optional and only needed if the proxy server needs authentication.

http.proxyPassword - Optional and only needed if the proxy server needs authentication.

Or

### 2)Using HTTPS

https.proxyHost - the host or IP address of the proxy server.

https.proxyPort - the port used by the proxy server.

https.proxyUser - Optional and only needed if the proxy server needs authentication.

https.proxyPassword - Optional and only needed if the proxy server needs authentication.


## Syntax

### Using only host and port

```
java -Dhttp.proxyHost=myproxyserver.com -Dhttp.proxyPort=9595 -jar cxflow.jar <Additional-cxflow-parameters>
```

### Using host, port, user and password

```
java -Dhttp.proxyHost=myproxyserver.com -Dhttp.proxyPort=9595 -Dhttp.proxyUser=<proxy user> -Dhttp.proxyPassword=<proxy password> -jar cxflow.jar <Additional-cxflow-parameters>
```
