## 1.1.99 - 2017-01-30
- [GP-875](https://jira.oicr.on.ca/browse/GP-875) - Update decider to SeqWare 1.1.1-gsi
## 1.1 - 2015-09-22
- [GP-485](https://jira.oicr.on.ca/browse/GP-485) - Calculate chr-sizes from bed file
- Update to workflow-gatk-haplotype-caller 1.1
## 1.0 - 2015-07-24
- Initial release of the GATK Haplotype Caller decider
- Schedules one workflow run for each unique 
    <root sample name, tissue origin + type + prep + region, template type, group id> 
  group of bam files that pass the user specified filters: library-template-type, 
  tissue-type, tissue-origin, and resequencing type
- [GP-318](https://jira.oicr.on.ca/browse/GP-318) - Upgrade to GATK 3.4-0
