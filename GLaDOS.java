package cda;

import robocode.*;
import java.awt.Color;

/**
 * GLaDOS - a robot by (Chris, Dan, Alistair)
 * 
 * @author Chris Kirkham <ck5g09@ecs.soton.ac.uk>
 * @author Alistair Lynn <alistair@hovercatsw.com>
 * @author Daniel May <dm16g09@ecs.soton.ac.uk>
 */
public class GLaDOS extends Robot {

	private Radar radar = new Radar(this);
	private Gun gun = new Gun(this);
	private Movement movement = new Movement(this);

	public Radar getRadar() {
		return radar;
	}

	public Gun getGun() {
		return gun;
	}

	public Movement getMovement() {
		return movement;
	}

	/**
	 * run: GLaDOS's default behavior
	 */
	public void run() {
		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:
		setColors(Color.black, Color.yellow, Color.black);
		ahead(1);

		while (true) {
			// Replace the next 4 lines with any behavior you would like

			radar.update();
			movement.update();
			gun.update();

			// ahead(100);
			// turnGunRight(360);

		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		radar.onScannedRobot(e);
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		radar.onHitByBullet(e);
	}

}
