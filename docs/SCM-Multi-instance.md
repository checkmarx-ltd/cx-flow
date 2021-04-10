
Cx-Flow supports multiple SCM’s accounts: BitBucket, GitHub, GitLab & Azure Devops (instances)

[[/Images/github_multi_instance.png|thresholds screenshot]]


<u>**Configuration changes required**</u> – via configuration file

Optional instances are configured under each SCM section (e.g. GitHub)

[[/Images/multi_instance_example.png|thresholds screenshot]]


The supported properties which can be override are:
* webhook-token
* token
* url
* api-url


#### <u>Default properties are getting overridden with optional instance properties</u>
In order to override the default SCM’s properties with an optional properties, a webhook **scm-instance** query parameter should be set on the webhook’s **Payload URL**:
[[/Images/multi_instance_url_payload.png|thresholds screenshot]]

* In this example we are settings the scm-instance parameter key with **‘instance2’** value.
* If the optional SCM ‘instance2’ configuration is found, the default SCM configuration will be overridden with the ‘instance2’ properties.
* If the specified scm-instance is not defined, an exception will be thrown.
* The default SCM configuration is used if the **scm-instance** query parameter is not provided.


## BitBucket Multi-Instance Configuration

Configuring SCM multi-instance with BitBucket has a slightly different requirement for configuration.

The default BitBucket SCM configuration uses the **url** and **api-path** configuration options to access the API and form URLs that reference the repository.  Using SCM multi-instance would generally imply that the **url** configuration option would be overridden.  For BitBucket, it is required that the **api-url** configuration option is also provided in each optional SCM configuration.  

The **api-url** configuration option must have the full URL for the BitBucket REST API.  The image below shows a default BitBucket server configuration with an optional SCM configuration for a separate on-premise BitBucket server and BitBucket cloud.

[[/Images/bb_multi_scm.png|BitBucket multi-SCM configuration]]
