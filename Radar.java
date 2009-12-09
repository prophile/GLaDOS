package cda;

import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

public class Radar {
	
	private GLaDOS owner;
	private boolean sawRobot = false;
	private double dRadarHeading = Math.PI / 10;
	
	public Radar(GLaDOS g){
		owner = g;
		
	}///cons
	
	public void onScannedRobot(ScannedRobotEvent e){
		//owner.fire(1);
		//sawRobot = true;
		dRadarHeading = -1 * dRadarHeading;
		
		
	}//onscannedrobot
	
	public void onHitByBullet(HitByBulletEvent e) {
		owner.turnLeft(90 - e.getBearing());
	}
	
	public boolean didSeeRobot () {
		return sawRobot;
	}
	
	public void update(){
		sawRobot = false;
		owner.turnRadarRightRadians(dRadarHeading);
		owner.scan();
	}//update

}
