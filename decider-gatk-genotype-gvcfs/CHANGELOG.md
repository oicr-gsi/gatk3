## 1.2.1 - 2019-08-12
- [GP-2049](https://jira.oicr.on.ca/browse/GP-2049) - Include sample name in selection of latest input file for a run+lane+barcode.
## 1.2 - 2017-01-30
- [GP-875](https://jira.oicr.on.ca/browse/GP-875) - Update decider to SeqWare 1.1.1-gsi
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
