#!/bin/bash
set -o nounset
set -o errexit
set -o pipefail

#enter the workflow's final output directory ($1)
cd "$1"

qrsh -l h_vmem=8G -cwd -now n "\
. /oicr/local/Modules/default/init/bash; \
module load vcftools/0.1.11 2>/dev/null; \
find . -regex '.*\.vcf.gz$' -print0 | xargs -0 -n1 -I '{}' bash -c 'vcf-validator {} && vcf-stats {} | sed -e 's/,$//' '; "