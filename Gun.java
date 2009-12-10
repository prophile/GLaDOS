package cda;

public class Gun {

	private GLaDOS owner;

	public Gun(GLaDOS g) {
		owner = g;
	}

	public void update() {
		Radar radar = owner.getRadar();
		if (radar.isTracking()) {
			double gunHeading = owner.getGunHeadingRadians();
			double targetHeading = radar.getAngleToTarget();
			if (Math.abs(targetHeading - gunHeading) < 0.05) {
				// this is close enough: fire!
				owner.setFire(1);
				// TODO: lead the target
			} else {
				double difference = targetHeading - gunHeading;
				owner.setTurnGunLeftRadians(difference);
			}
		}
	}

	public void init() {
	}
}
