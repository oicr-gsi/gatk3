#!/bin/sh

#Stop the asterisk from expanding
set -f
CMD="${1} -Xmx${2}g -Djava.io.tmpdir=${3}\
  -jar ${4} -T VariantFiltration -R ${5} \
  -V ${6} -o ${7} -L ${8} -et NO_ET \
  --filterExpression \"MQ0 >= 4 && ((MQ0 / (1.0 * DP)) > 0.1)\" --filterName Indel_HARD_TO_VALIDATE \
  --filterExpression \"SB > -10.0\" --filterName Indel_StrandBias \
  --filterExpression \"QUAL < 30\" --filterName Indel_VeryLowQual \
  --filterExpression \"DP < 5\" --filterName Indel_LowCoverage \
  --filterExpression \"FS > 60.0\" --filterName Indel_StrandBiasFishers \
  --filterExpression \"QUAL > 30.0 && QUAL < 50.0\" --filterName Indel_LowQual "
shift 8 
CMD="$CMD $@"
echo $CMD
eval $CMD
#${1} -Xmx${2}g -Djava.io.tmpdir=${3} \
#  -jar ${4} -T VariantFiltration -R ${5} \
#  -V ${6} -o ${7} -L ${8} -et NO_ET \
#  --filterExpression 'MQ0 >= 4 && ((MQ0 / (1.0 * DP)) > 0.1)' --filterName HARD_TO_VALIDATE \
#  --filterExpression 'SB >= -1.0' --filterName StrandBiasFilter --filterExpression 'QUAL < 10' --filterName QualFilter
