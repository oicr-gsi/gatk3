#!/bin/bash

cd $1
#zcat *.vcf.gz | grep -v -E '##GATKCommandLine|##reference' | sed 's/\(RankSum=\)[^;]*\(;\)/\1REMOVED\2/g' | sort | md5sum

qrsh -l h_vmem=8G -cwd -now n "\
. /oicr/local/Modules/default/init/bash; \
module load vcftools/0.1.11 2>/dev/null; \
find . -regex '.*\.vcf.gz$' \
       -exec sh -c \" vcf-stats {} ; echo \" \; \
"
