VF=`ls -1 ../generated/Base*.sv`

for fn in $VF; do
	echo $f
    BN=`basename $fn`
    TOP=${BN%%.sv}
	./yosys-fpga-techmap-stat.sh  $fn | tee -a stat_lut_$TOP.txt
	./yosys-techmap-stat.sh $fn | tee -a stat_cells_$TOP.txt
done
