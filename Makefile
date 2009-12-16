JAVAC=javac
JAVAFLAGS=-extdirs ../../libs -cp ..
SOURCES=WallE.java AntiGravityMovement.java CircularTargetting.java LinearTargetting.java Movement.java NaiveTargetting.java RandomTargetting.java ReverseCircularTargetting.java Targetting.java VirtualGunTargetting.java WallMovement.java WaveSurfMovement.java

all: $(SOURCES)
	$(JAVAC) $(JAVAFLAGS) $<

clean:
	rm -f *.class

stordify:
	cd ../.. ; java -Xmx512M -Dsun.io.useCanonCaches=false -cp libs/robocode.jar robocode.Robocode -battle robots/cda/nemesis.battle -nodisplay -results robots/cda/nemesis.results
