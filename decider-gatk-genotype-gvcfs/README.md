##GATK Genotype GVCFs decider

Version 1.0

###Overview


Please see [basic deciders](https://seqware.github.io/docs/6-pipeline/basic_deciders) for additional information.



###Compile

```
mvn clean install
```

###Usage
```
java -jar Decider.jar --study-name \<study-name\> --wf-accession \<gatk-genotype-gvcfs-workflow-accession\>
```

###Options

**Required**
See [basic deciders](https://seqware.github.io/docs/6-pipeline/basic_deciders) for general decider options. No additional options are strictly required.

**Optional**
Parameter | Type | Description \[default\]
----------|------|-------------
verbose | none | Log all SeqWare (debug) information


##Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca .
