# GATK3

This [SeqWare](http://seqware.github.io/) workflow performs indel and SNP variant calling using [GATK](https://www.broadinstitute.org/gatk/).

Two methods of GATK's best practices are supported:
- joint variant calling: perform preprocessing and variant calling for all samples in one workflow run
- joint genotyping: preprocess individual samples in separate workflow runs, combine preprocessed output for variant calls

The benefits of "joint genotyping" over "joint variant calling" are examined in this [article](https://www.broadinstitute.org/gatk/guide/article?id=3893).

##Workflow

Joint variant calling is performed using the [GATK3 SeqWare workflow](workflow-gatk3).

Joint genotyping is performed using the [GATK Haplotype Caller SeqWare workflow](workflow-gatk-haplotype-caller) and the [GATK Genotype GVCFs SeqWare workflow](workflow-gatk-genotype-gvcfs)

###Decider

The joint variant calling workflow can be launched using the [GATK3 SeqWare decider](decider-gatk3).

The joint genotyping workflow can be launched using the [GATK Haplotype Caller SeqWare decider](decider-gatk-haplotype-caller) followed by the [GATK Genotype GVCFs SeqWare decider](decider-gatk-genotype-gvcfs).

##Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca .