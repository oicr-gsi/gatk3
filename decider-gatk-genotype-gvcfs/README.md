##GATK Genotype GVCFs decider

Version 1.0

###Overview

This decider launches the [GATK Genotype GVCFs workflow](../workflow-gatk-genotype-gvcfs) on GVCFs produced by the [GATK Haplotype Caller workflow](../workflow-gatk-haplotype-caller).  

The decider identifies files to operate on by:

* Selects all files from the parent workflow (parent-wf-accessions) of type **application/g-vcf-gz** or **application/tbi**
* Filters out files that do not match the filter criteria (e.g. --study-name, --root-sample-name, --before-date, --tissue-type, see #options for all possible filters)
* If there are multiple GVCFs with metadata = \<ROOT_SAMPLE_NAME + TISSUE_ORIGIN + TISSUE_TYPE + TISSUE_PREP + TISSUE_REGION + LIBRARY_TEMPLATE_TYPE + GROUP_ID\>, the most recently created GVCF is selected
* Groups files (--group-by) into workflow run(s) (default: no grouping)

For each workflow run, the decider:

* Configures the output file name prefix using a combination of the filters or the group value defined by "--group-by" (override this value using "--id")
* Performs validation to ensure the workflow run can be launched

Please see [basic deciders](https://seqware.github.io/docs/6-pipeline/basic_deciders) for additional information.

###Compile

```
mvn clean install
```

###Usage

```
java -jar Decider.jar \
--wf-accession <gatk-genotype-gvcfs-workflow-accession> \
--parent-wf-accessions <gatk-haplotype-caller-workflow-accession> \
--study-name <study-name> \
--library-template-type <WG/EX/TS>
```

###Options

Please see [basic deciders](http://seqware.github.io/docs/17-plugins/#basicdecider) and [oicr deciders](https://github.com/oicr-gsi/pipedev/tree/master/deciders#options) for general decider options.

#####Required:

Parameter | Description
---|---
--library-template-type | Restrict the processing to samples of a particular template type (WG/EX/TS)

#####Optional:

Parameter | Description | Default
---|---|---
--group-by | Specify how files that pass filter criteria will be grouped into workflow runs | (no grouping)
--id | Override final filename prefix |
--tissue-type | Restrict the processing to samples of particular tissue types (e.g. P, R, X, C) | (no restriction)
--tissue-origin | Restrict the processing to samples of particular tissue origin (e.g. Ly, Pa, Pr) | (no restriction)
--resequencing-type | Restrict the processing to samples of a particular resequencing type | (no restriction)
--group-id | Restrict the processing to samples of a particular group-id | (no restriction)
--verbose | Log all SeqWare (debug) information |

Additional [workflow properties](../workflow-gatk-genotype-gvcfs/README.md#options) can be overridden by adding \<property, value\> pairs (for example: `-- --property1 value --property2 "value1,value2"`) to the end of the command line.

###Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca .
