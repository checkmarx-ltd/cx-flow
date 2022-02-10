## Integration with CodeBashing

CxFlow adds CodeBashing link when creating a Bug-Tracker ticket.
The link to CodeBashing is added under '**Training**' in the ticket recommendation:

[[/Images/Training_link.png|Training_Link]]

By default CxFlow puts codebashing-url value in the link url
```
cx-flow:
  codebash-url: https://api.codebashing.com/lessons
```

Now each customer (CodeBashing tenant), can use CxFlow to add training link to his tickets - with direct link to relevant course per language and CWE!
The link will direct to the specific lesson under the customer CodeBashing account.
for example:  (https://{tenant}.codebashing.com/courses/backend_java/lessons/sql_injection)


To get direct CodeBashing integration you need your own tenant specific url for organization and secret.
Here, secret is the x-api-key of the codebashing application, provided on tenant creation.

 ```
codebashing:
  codebashing-api-url: https://api.codebashing.com/lessons
  tenant-base-url: https://{tenant}.codebashing.com
  api-secret: {SECRET}
 ```


 