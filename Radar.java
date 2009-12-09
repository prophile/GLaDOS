package cda;

import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

public class Radar {
	
	private GLaDOS owner;
	private boolean sawRobot = false;
	
	public Radar(GLaDOS g){
		owner = g;
		
	}///cons
	
	public void onScannedRobot(ScannedRobotEvent e){
		owner.setFire(1);
		sawRobot = true;
	}//onscannedrobot
	
	public void onHitByBullet(HitByBulletEvent e) {
		owner.setTurnLeftRadians(Math.PI - e.getBearing());
	}
	
	public boolean didSeeRobot () {
		return sawRobot;
	}
	
	public void update(){
		sawRobot = false;
		owner.setTurnRadarRightRadians(Math.PI / 10);
		owner.scan();
	}//update

}
