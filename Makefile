JAVAC=javac
JAVACFLAGS=-extdirs ~/robocode/libs -cp ~/robocode/robots

GLaDOS.class: GLaDOS.java Gun.java Movement.java Radar.java
	$(JAVAC) $(JAVACFLAGS) $<

Gun.class: Gun.java GLaDOS.java Radar.java
	$(JAVAC) $(JAVACFLAGS) $<

Movement.class: Movement.java GLaDOS.java Radar.java
	$(JAVAC) $(JAVACFLAGS) $<

Radar.class: Radar.java GLaDOS.java
	$(JAVAC) $(JAVACFLAGS) $<

clean:
	rm -f *.class
