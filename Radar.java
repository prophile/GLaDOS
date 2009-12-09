package cda;

import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

public class Radar {

	private GLaDOS owner;
	private boolean sawRobot = false;
	private double dRadarHeading = Math.PI / 4;

	public Radar(GLaDOS g){
		owner = g;

	}///cons

	public void init(){
		//owner.setAdjustGunForRobotTurn(false);
	}

	public void onScannedRobot(ScannedRobotEvent e){

		//owner.setFire(1);
		//sawRobot = true;
		dRadarHeading = -1 * dRadarHeading;

		owner.setFire(1);

	}//onscannedrobot

	public void onHitByBullet(HitByBulletEvent e) {
		owner.setTurnLeftRadians(Math.PI - e.getBearing());
	}

	public boolean didSeeRobot () {
		return sawRobot;
	}

	public void update(){
		sawRobot = false;
		owner.setTurnRadarRightRadians(dRadarHeading);
		owner.scan();
	}//update

}
