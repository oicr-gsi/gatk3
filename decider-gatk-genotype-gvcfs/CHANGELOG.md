## 1.1 - 2015-09-22
- [GP-487](https://jira.oicr.on.ca/browse/GP-487)
    - Add standard call and emit confidence support and default values
    - Add support for populating the vcf's ID column using dbSNP
- Update to workflow-gatk-genotype-gvcfs 1.1
## 1.0 - 2015-07-24
- Initial release of the GATK Genotype GVCFs decider
- Schedules one workflow run for all files that pass the required filter library-template-type and 
    the optional filters: tissue-type, tissue-origin, and resequencing type
- [GP-318](https://jira.oicr.on.ca/browse/GP-318) - Upgrade to GATK 3.4-0