#!/bin/bash

if [ -z "$1" ] ; then
	echo "$0 verilogfile.v"
	exit 0
fi

FN=$1
BN=`basename $FN`
TOP=${BN%%.sv}

cat <<EOF > tmp.ys
read -sv $FN
hierarchy -top $TOP
proc
opt
techmap; opt
stat
EOF

yosys tmp.ys

rm -f tmp.ys
