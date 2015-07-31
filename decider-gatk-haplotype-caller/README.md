##GATK Haplotype Caller decider

Version 1.0

###Overview

This decider launches the [GATK Haplotype Caller workflow](../workflow-gatk-haplotype-caller) on BAM files produced by the [BAM Filter Merge Collapse workflow](https://github.com/oicr-gsi/bam-filter-merge-collapse/tree/master/workflow-bam-filter-merge-collapse).

The decider identifies files to operate on by:

* Selects all files from the parent workflow (--parent-wf-accessions) of type **application/bam** or **application/bam-index**
* Filters out files that do not match the filter criteria (e.g. --study-name, --root-sample-name, --before-date, --tissue-type, see [options](#options) for all possible filters)
* If there are multiple BAMs with metadata = \<ROOT_SAMPLE_NAME + TISSUE_ORIGIN + TISSUE_TYPE + TISSUE_PREP + TISSUE_REGION + LIBRARY_TEMPLATE_TYPE + GROUP_ID\>, the most recently created BAM is selected
* Groups files (--group-by) into workflow run(s) (default: group by \<ROOT_SAMPLE_NAME + TISSUE_ORIGIN + TISSUE_TYPE + TISSUE_PREP + TISSUE_REGION + LIBRARY_TEMPLATE_TYPE + GROUP_ID\>)

For each workflow run, the decider:

* Configures the output file name prefix using a combination of the filters or the group value defined by "--group-by" (override this value using "--id")
* Iterates over the input files to find the interval file (Note: for each workflow run group, a single interval file is currently required)
* Disables downsampling if LIBRARY_TEMPLATE_TYPE is TS
* Performs validation to ensure the workflow run can be launched

See [basic deciders](http://seqware.github.io/docs/17-plugins/#basicdecider) for general decider options.

###Compile

```
mvn clean install
```

###Usage

```
java -jar Decider.jar \
--wf-accession <gatk-haplotype-caller-workflow-accession> \
--parent-wf-accessions <bam-filter-merge-collapse-workflow-accession> \
--study-name <study-name> \
--library-template-type <WG/EX/TS> \
--dbsnp <path-to-dbsnp-vcf>
```

###Options

Please see [basic deciders](http://seqware.github.io/docs/17-plugins/#basicdecider) and [oicr deciders](https://github.com/oicr-gsi/pipedev/tree/master/deciders#options) for general decider options.

#####Required:

Parameter | Description
---|---
--library-template-type | Restrict the processing to samples of a particular template type (WG/EX/TS)
--dbsnp | Specify the absolute path to the dbSNP vcf

#####Optional:

Parameter | Description | Default
---|---|---
--group-by | Specify how files that pass filter criteria will be grouped into workflow runs | \<ROOT_SAMPLE_NAME + TISSUE_ORIGIN + TISSUE_TYPE + TISSUE_PREP + TISSUE_REGION + LIBRARY_TEMPLATE_TYPE + GROUP_ID\>
--id | Override final filename prefix |
--tissue-type | Restrict the processing to samples of particular tissue types (e.g. P, R, X, C) | (no restriction)
--tissue-origin | Restrict the processing to samples of particular tissue origin (e.g. Ly, Pa, Pr) | (no restriction)
--resequencing-type | Restrict the processing to samples of a particular resequencing type | (no restriction)
--group-id | Restrict the processing to samples of a particular group-id | (no restriction)
--chr-sizes | Comma separated list of chromosome intervals used to parallelize indel realigning and variant calling | (by chromosome)
--interval-padding | Amount of padding to add to each interval (chr-sizes and interval-file determined by decider) in bp | 100
--disable-bqsr | Disable BQSR (BaseRecalibrator + PrintReads steps) and pass indel realigned BAMs directly to variant calling | (bqsr enabled)
--downsampling | Set whether or not the variant caller should downsample the reads | (false for TS, true for WG and EX)
--rsconfig-file | Specify location of .xml file which should be used to configure references, will be used if resequencing-type is different from the default | /.mounts/labs/PDE/data/rsconfig.xml
--verbose | Log all SeqWare (debug) information |

Additional [workflow properties](../workflow-gatk-haplotype-caller/README.md#options) can be overridden by adding \<property, value\> pairs (for example: `-- --property1 value --property2 "value1,value2"`) to the end of the command line.

###Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi/gatk3) or send an email to gsi@oicr.on.ca .
