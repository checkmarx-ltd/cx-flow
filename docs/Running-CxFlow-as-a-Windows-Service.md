CxFlow can be run as a Windows Service using the Windows Service Wrapper (winsw). Here's a step-by-step guide on how to run CxFlow as a Windows Service.

* [Setup](#setup)
    * [Step 1](#one)
    * [Step 2](#two)
    * [Step 3](#three)
    * [Step 4](#four)
* [Running CxFlow as a specific service](#running)
* [Installing, Starting, Stopping, Uninstalling](#installing)

## <a name="setup">Setup</a>
### <a name="one">Step 1</a>
Download the Windows Service Wrapper from GitHub : https://github.com/kohsuke/winsw/releases
<br/>There are two executables available for download. Make sure you use the version corresponding to the .NET libraries installed in your server.
<br/>For Microsoft .Net (not .Net Core): Executing the following Powershell command will give you a version stamp that can be matched to Microsoft .Net Version information.  Select an executable that is built for the installed version or lower. 
```
(Get-ItemProperty "HKLM:SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full").Release
```
For .Net Core: Execute the following command in Powershell:
```

dotnet --version
```

### <a name="two">Step 2</a>
Rename the WinSW.NET.x.exe to CxFlow.exe

### <a name="three">Step 3</a>
Move the CxFlow.exe to the directory where the CxFlow executable jar resides.

### <a name="four">Step 4</a>
Create an XML configuration file for the Windows Service Wrapper
<br/>Example Configuration XML
```
<?xml version="1.0" encoding="UTF-8"?>
<service>
    <id>CxFlow</id>
    <name>CxFlow</name>
    <description>CxFlow Windows Service</description>
    <executable>java</executable>
    <arguments>-jar "cx-flow-1.X.jar" --spring.config.location=/path/to/application.yml</arguments>
    <logmode>rotate</logmode>
</service>
```
### <a name="running">Running CxFlow as a specific service</a>
If CxFlow needs to run as a specific service account, include the following section in the above XML configuration file.
```
<serviceaccount>
    <domain>NT AUTHORITY</domain>
    <user>NetworkService</user>
</serviceaccount>
```

## <a name="installing"> Installing, Starting, Stopping, Uninstalling</a>
Execute CxFlow.exe <operation>
<br/>Where operation can be:
  * install to install the service to Windows Service Controller. 
  * uninstall to uninstall the service. The opposite operation of above.
  * start to start the service. The service must have already been installed.
  * stop to stop the service.
  * restart to restart the service. If the service is not currently running, this command acts like start.
  * status to check the current status of the service.
    * This command prints one line to the console.
      * NonExistent indicates the service is not currently installed
      * Started to indicate the service is currently running
      * Stopped to indicate that the service is installed but not currently running.