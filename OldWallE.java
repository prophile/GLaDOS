/*******************************************************************************
 *  _  _  _ _______                   _______
 *  |  |  | |_____| |      |      ___ |______
 *  |__|__| |     | |_____ |_____     |______
 *
 * A bot by Chris Kirkham, Alistair Lynn & Dan May
 */
package cda;

import robocode.*;
import robocode.util.*;
import java.io.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.*;
import java.awt.geom.*;

public class OldWallE extends AdvancedRobot {

	double moveAmount; // How much to move
	double oldEnemyHeading = 0.0;
	
	double expectedEnemyEnergy = 100.0;
	
	int count = 0; // Keeps track of how long we've
	// been searching for our target
	double gunTurnAmt; // How much to turn our gun when searching
	String trackName; // Name of the robot we're currently tracking
	static double wallTurnAngle = 90.0;
	boolean stayStill = false;

	PrintStream log;

	/**
	 * run: Move around the walls
	 */
	public void run() {
		try {
			log = new PrintStream(new RobocodeFileOutputStream(getDataFile("wall.log")));
		} catch (IOException e) {
			// do nothing, this will never happen, honest.
		}
		// Set colors
		setBodyColor(Color.black);
		setGunColor(Color.yellow);
		setRadarColor(Color.black);
		setBulletColor(Color.black);
		setScanColor(Color.white);

		// Initialize moveAmount to the maximum possible for this battlefield.
		moveAmount = Math.max(getBattleFieldWidth(), getBattleFieldHeight());

		// turn to the wall
		turnLeft(getHeading() % 90);
		
		trackName = null; // Initialize to not tracking anyone
		setAdjustGunForRobotTurn(true); // Keep the gun still when we turn
		gunTurnAmt = 10; // Initialize gunTurn to 10

		while (true) {
			setTurnGunRight(gunTurnAmt);
			setAhead(moveAmount);
			// Keep track of how long we've been looking
			count++;
			// If we've haven't seen our target for 2 turns, look left
			if (count > 2) {
				gunTurnAmt = -10;
			}
			// If we still haven't seen our target for 5 turns, look right
			if (count > 5) {
				gunTurnAmt = 10;
			}
			// If we *still* haven't seen our target after 10 turns, find another target
			if (count > 11) {
				trackName = null;
			}
			execute();
			int currentEnergy = (int)getEnergy();
			if (currentEnergy < 30)
				setGunColor(Color.red);
			else if (expectedEnemyEnergy < 30.0)
				setGunColor(Color.green);
			else
				setGunColor(Color.yellow);
		}
	}
	
	public void turnDueToWall () {
		setAhead(0);
		//back(10);
		turnLeft(wallTurnAngle);
	}
	
	public void onHitWall(HitWallEvent e) {
		turnDueToWall();
	}

	/**
	 * onHitRobot:  Move away a bit.
	 */
	public void onHitRobot(HitRobotEvent e) {
		// If he's in front of us, set back up a bit.
		if (e.getBearing() > -90 && e.getBearing() < 90) {
			back(100);
		} // else he's in back of us, so set ahead a bit.
		else {
			ahead(100);
		}
		// Set the target
		trackName = e.getName();
		// Back up a bit.
		fire(3);
	}

	/**
	 * onScannedRobot:  Here's the good stuff
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		
		// If we have a target, and this isn't it, return immediately
		// so we can get more ScannedRobotEvents.
		if (trackName != null && !e.getName().equals(trackName)) {
			return;
		}
		
		// If we don't have a target, well, now we do!
		if (trackName == null) {
			trackName = e.getName();
			// log.println("Tracking " + trackName);
			expectedEnemyEnergy = e.getEnergy();
		} else {
			if (e.getEnergy() < expectedEnemyEnergy) {
				boolean changed = false;
				if (stayStill) {
					changed = true;
					stayStill = false;
				} else if (e.getDistance() > 100 && !(trackName.contains("Wall"))) {
					changed = true;
					stayStill = true;
				}
				if (changed && stayStill)
					stop();
				else if (changed)
					resume();
				// log.println("Detected fire");
				expectedEnemyEnergy = e.getEnergy();
			}
		}
		// This is our target.  Reset count (see the run method)
		count = 0;
		
		// Our target is close.
		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		//setTurnGunRight(gunTurnAmt);
		
		if (getEnergy() < 20.0 && e.getDistance() > 500.0)
			return;
		
		// This is circular tracking code, based on code from the wiki
		double bulletPower = Math.min(3.0,getEnergy());
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		double enemyVelocity = e.getVelocity();
		oldEnemyHeading = enemyHeading;
		
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), 
		battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) < 
			  Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			enemyHeading += enemyHeadingChange;
			if(	predictedX < 18.0 
			   || predictedY < 18.0
			   || predictedX > battleFieldWidth - 18.0
			   || predictedY > battleFieldHeight - 18.0){
				
				predictedX = Math.min(Math.max(18.0, predictedX), 
									  battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), 
									  battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(
															predictedX - getX(), predictedY - getY()));
		
		
		setTurnGunRightRadians(Utils.normalRelativeAngle(
														 theta - getGunHeadingRadians()));

		setTurnRadarRightRadians(Utils.normalRelativeAngle(
														   absoluteBearing - getRadarHeadingRadians()));

		fire(bulletPower);


		//scan();
	}
	
	/**
	 * onWin:  Do a victory dance
	 */
	public void onWin(WinEvent e) {
		setAhead(0);
		for (int i = 0; i < 50; i++) {
			setBodyColor(Color.black);
			setGunColor(Color.yellow);
			setRadarColor(Color.black);
			setTurnRight(Double.POSITIVE_INFINITY);
			setTurnGunLeft(Double.POSITIVE_INFINITY);
			execute();
			setBodyColor(Color.yellow);
			setGunColor(Color.black);
			setRadarColor(Color.yellow);
			setTurnRight(Double.POSITIVE_INFINITY);
			setTurnGunLeft(Double.POSITIVE_INFINITY);
			execute();
		}
	}
	
	public void onBulletHit(BulletHitEvent e){
		expectedEnemyEnergy = e.getEnergy();
	}
	
	public void onDeath(DeathEvent e) {
		wallTurnAngle = -wallTurnAngle;
	}
}
