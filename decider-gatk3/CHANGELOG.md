## 1.0.3 - 2015-07-14
 - [GP-386](https://jira.oicr.on.ca/browse/GP-386) - Use "group-by" or filters to produce identifier/output file name prefix
    - If "--id" is provided, set identifier to "--id" value
      Else if "--group-by" is provided, set identifier to "--group-by" value
      Else set identifier to concatenation of filter values
## 1.0.2 - 2015-07-07
 - [GP-378](https://jira.oicr.on.ca/browse/GP-378) - Add "group-by" support
## 1.0.1 - 2015-06-18
- Update decider to use latest version of workflow-gatk3 (1.0.1)
## 1.0 - 2015-06-04
- Initial release of the GATK3 decider based off of the OICR GATK3 helper script and workflow-gatk
- Schedules one workflow run for all files that pass the required filter library-template-type and 
    the optional filters: tissue-type, tissue-origin, and resequencing type.