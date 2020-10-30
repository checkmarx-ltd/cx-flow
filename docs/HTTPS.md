To use CxFlow over HTTPS, an SSL certificate is required to be imported into a keystore.
<br>See documentation on importing certificates here: 
<br>[https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG180](https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG180)
<br>[https://www.baeldung.com/spring-boot-https-self-signed-certificate](https://www.baeldung.com/spring-boot-https-self-signed-certificate)
<br>[https://support.code42.com/Administrator/6/Configuring/Install_a_CA-signed_SSL_certificate_for_HTTPS_console_access](https://support.code42.com/Administrator/6/Configuring/Install_a_CA-signed_SSL_certificate_for_HTTPS_console_access)

## Configuration
CxFlow is a Springboot application driven by a YAML configuration file. CxFlow can be configured to run over HTTPS by updating the application.yml configuration file.
<br>Edit the application.yml file and update the server section as follows:
```

server:
    port: <desired_ssl_port - usually 443 or 8443>
    ssl:
        key-store: <keystore filename>
        key-store-password: <key/store password>
        key-store-type: { JKS | PKCS12 }
        key-alias: <key alias in the keystore>
        enabled-protocols:
          - TLSv1.3
          - TLSv1.2
```
[https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl)

## Example Setup
Assumptions:
  * JDK 8 installed
  * Windows Machine running CxFlow
<br>Open a CMD prompt and type the following
```
keytool -genkeypair -alias cxflow -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore C:\keystorecxflow.p12 -validity 3650
keytool -export -alias cxflow -keystore C:\keystorecxflow.p12 -rfc -file C:\cxflow.cert
 
//Import certificate to Windows Trust Store
certutil.exe -addstore root C:\cxflow.cert
```
Insert the following into the application.yml file
```
  port: 443
  ssl:
    key-store: C:\keystorecxflow.p12
    key-store-password: P@ssword1
    key-store-type: PKCS12
    key-alias: cxflow
    enabled-protocols:
    - TLSv1.3
    - TLSv1.2
```