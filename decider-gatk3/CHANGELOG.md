## 1.4.1 - 2019-08-12
- [GP-2049](https://jira.oicr.on.ca/browse/GP-2049) - Include sample name in selection of latest input file for a run+lane+barcode.
## 1.4 - 2017-01-30
- [GP-875](https://jira.oicr.on.ca/browse/GP-875) - Update decider to SeqWare 1.1.1-gsi
## 1.3 - 2016-02-03
- [GP-691](https://jira.oicr.on.ca/browse/GP-691) - Remove "disable-bqsr" parameter - indel realignment and BQSR steps have been removed from workflow.
- Update to workflow-gatk3 1.3
## 1.2 - 2015-09-21
- [GP-485](https://jira.oicr.on.ca/browse/GP-485) - Calculate chr-sizes from bed file
- Update to workflow-gatk3 1.2
## 1.0.5 - 2015-09-21
- Update to workflow-gatk3 1.0.2
## 1.1 - 2015-07-24
- Upgrade to workflow-gatk3 1.1
- [GP-318](https://jira.oicr.on.ca/browse/GP-318) - Upgrade to GATK 3.4-0
- [GP-417](https://jira.oicr.on.ca/browse/GP-417) - Remove GATK3 dbsnp default value
## 1.0.4 - 2015-07-23
- [GP-406](https://jira.oicr.on.ca/browse/GP-406) - Restrict workflow runs to having one interval file
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
    the optional filters: tissue-type, tissue-origin, and resequencing type
