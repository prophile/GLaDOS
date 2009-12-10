package cda;

import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

public class Radar {

	private GLaDOS owner;
	private double dRadarHeading = Math.PI / 4;
	private double opponentBearing;
	private int lastTrack = 0;

	public Radar(GLaDOS g) {
		owner = g;

	}// /cons

	public void init() {
		// owner.setAdjustGunForRobotTurn(false);
	}
	
	public boolean isTracking () {
		return lastTrack > 0;
	}
	
	public double getAngleToTarget () {
		return opponentBearing;
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		if (lastTrack == 0) {
			// we've just picked up the other bot
			dRadarHeading = -1 * dRadarHeading;
			// TODO: stuff
		}
		opponentBearing = e.getBearing();
		lastTrack = 15;
	}// onscannedrobot

	public void onHitByBullet(HitByBulletEvent e) {
	}

	public void update() {
		if (lastTrack > 0) {
			// turn to follow the bot
		} else {
			// spin right round
			owner.setTurnRadarRightRadians(dRadarHeading);
		}
		owner.scan();
	}// update

}
