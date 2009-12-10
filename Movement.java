package cda;

public class Movement {
	private GLaDOS owner;

	public Movement(GLaDOS g) {
		owner = g;
	}

	public void update() {
		owner.setAhead(Double.POSITIVE_INFINITY);
		owner.setTurnLeftRadians(0.0001);
	}

	public void init() {
	}

}
