
This page provides documentation for the `cx-flow.zip-exclude` configuration option. The option instructs CxFlow to exclude specific files when it creates a zip archive.

### Example
The following option excludes all `.png` files from the archive, as well as all files inside a root-level `.git` directory:
```
cx-flow:
  zip-exclude: \.git/.*, .*\.png
```

### Details
The meaning and syntax of the `cx-flow.zip-exclude` option are different as opposed to the `checkmarx.exclude-folders` and `checkmarx.exclude-files` options.

|cx-flow.zip-exclude|checkmarx.exclude-folders, checkmarx.exclude-files|
|-------------|---------|
|Uses regexes|Use wildcards|
|Works locally, before the sources are sent for scan|Work in CxSAST when it already has the sources|

`cx-flow.zip-exclude` is a comma-separated list. Each of the list items is a regex (not a wildcard). Spaces before and after a comma are ignored.

During zipping, CxFlow checks each file in the target directory against each of the regexes in `cx-flow.zip-exclude`. If there is a match, CxFlow excludes the file from the archive. In this case, when log level is **debug**, CxFlow writes a message to the log having the following format:
```
match: <regex>|<relative_file_path>
```

CxFlow uses relative file path to test the regex match. E.g. if the following file exists:
```   
c:\cxdev\projectsToScan\myproj\bin\internal-dir\exclude-me.txt
```
and we specify this CLI option: `--f="c:\cxdev\projectsToScan\myproj`,

then CxFlow will check the following relative file path against the regexes:
```
bin/internal-dir/exclude-me.txt
```
CxFlow normalizes slashes in the relative path into a forward slash (`/`).

For a file to be excluded, a regex must match the **whole** relative file path. Thus, the `.*` regex expression should be used where necessary.
