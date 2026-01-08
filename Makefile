test:
	@sbt test

clean:
	rm -f *.anno.json
	rm -f *.fir
	rm -f *.v
	rm -f generated/*
