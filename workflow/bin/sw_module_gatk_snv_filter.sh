#!/bin/sh

#Stop the asterisk from expanding
set -f
CMD="${1} -Xmx${2}g -Djava.io.tmpdir=${3} -jar ${4} -T VariantFiltration -R ${5} \
  -V ${6} -o ${7} -L ${8} -et NO_ET --clusterWindowSize 10 \
  --clusterSize 3 \
  --mask ${9} --maskName InDel \
  --filterExpression \"MQ0 >= 4 && ((MQ0 / (1.0 * DP)) > 0.1)\" --filterName HARD_TO_VALIDATE \
  --filterExpression \"DP < 5 \" --filterName \"LowCoverage\" \
  --filterExpression \"QUAL < 30.0 \" --filterName \"VeryLowQual\" \
  --filterExpression \"QUAL > 30.0 && QUAL < 50.0 \" --filterName \"LowQual\" \
  --filterExpression \"QD < 2.0 \" --filterName \"LowQD\" \
  --filterExpression \"FS > 60.0 \" --filterName \"StrandBiasFishers\" \
  --filterExpression \"SB > -10.0\" --filterName \"StrandBias\" "


shift 9 
CMD="$CMD $@"
echo $CMD
eval $CMD

#${1} -Xmx${2}g -Djava.io.tmpdir=${3} -jar ${4} -T VariantFiltration -R ${5} \
#  -V ${6} -o ${7} -L ${8} -et NO_ET --clusterWindowSize 10 \
#  --filterExpression 'MQ0 >= 4 && ((MQ0 / (1.0 * DP)) > 0.1)' --filterName HARD_TO_VALIDATE \
#  --mask ${9} --filterExpression 'QUAL < 30.0 || QD < 5.0 || HRun > 5 || SB > -0.10' \
#  --filterName GATKStandard

