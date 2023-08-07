
all: src/main/scala/hardfloat
	@echo Target:
	@echo clean

src/main/scala/hardfloat : prephardfloat.sh
	sh prephardfloat.sh

test:
	@sbt test

clean:
	rm -f *.anno.json
	rm -f *.fir
	rm -f *.v
	rm -f generated/*
