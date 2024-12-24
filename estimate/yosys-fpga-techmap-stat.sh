#!/bin/bash

if [ -z "$1" ] ; then
	echo "$0 verilogfile.v"
	exit 0
fi

FN=$1
BN=`basename $FN`
TOP=${BN%%.v}

cat <<EOF > tmp.ys
read -sv2012 $FN
hierarchy -top $TOP
proc; opt; techmap; opt
abc -lut 4; opt
techmap -map fpga_cells.v; opt
stat
EOF

yosys tmp.ys

rm -f tmp.ys
