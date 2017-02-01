#!/bin/bash

diff <( sed -e 's/,$//' $1 | sort ) <( sed -e 's/,$//' $2 | sort )