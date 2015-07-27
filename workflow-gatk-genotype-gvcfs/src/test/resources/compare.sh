#!/bin/bash
set -o nounset
set -o errexit
set -o pipefail

#actual output metrics is $1, expected output metrics is $2
diff <(sort $1) <(sort $2)