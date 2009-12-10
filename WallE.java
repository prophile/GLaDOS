/*******************************************************************************
 * Copyright (c) 2001, 2008 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/cpl-v10.html
 *
 * Contributors:
 *     Mathew A. Nelson
 *     - Initial implementation
 *     Flemming N. Larsen
 *     - Maintainance
 *******************************************************************************/
package cda;

import robocode.*;
import robocode.util.*;
import java.io.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.*;
import java.awt.geom.*;

/**
 * Walls - a sample robot by Mathew Nelson, and maintained by Flemming N. Larsen
 * <p/>
 * Moves around the outer edge with the gun facing in.
 */
public class WallE extends AdvancedRobot {

	double moveAmount; // How much to move
	double oldEnemyHeading = 0.0;
	
	int count = 0; // Keeps track of how long we've
	// been searching for our target
	double gunTurnAmt; // How much to turn our gun when searching
	String trackName; // Name of the robot we're currently tracking
	double wallTurnAngle = 90.0;

	PrintStream log;
	
	int hitCount = 1;
	int missCount = 1;

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

		// turnLeft to face a wall.
		// getHeading() % 90 means the remainder of
		// getHeading() divided by 90.
		turnLeft(getHeading() % 90);
		
		trackName = null; // Initialize to not tracking anyone
		setAdjustGunForRobotTurn(true); // Keep the gun still when we turn
		gunTurnAmt = 10; // Initialize gunTurn to 10

		while (true) {
			setTurnGunRight(gunTurnAmt);
			// Check to see if we're wallhitting
			/*if (getX() < 8 || getY() < 8 || getX() > (getBattleFieldWidth() - 8) || getY() > (getBattleFieldHeight() - 8))
				turnDueToWall();
			else*/
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
		// Note:  We won't get scan events while we're doing this!
		// An AdvancedRobot might use setBack(); execute();
		//gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		//turnGunRight(gunTurnAmt);
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
			log.println("Tracking " + trackName);
		}
		// This is our target.  Reset count (see the run method)
		count = 0;
		// If our target is too far away, turn and move toward it.
		/*if (e.getDistance() > 150) {
			gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
			setTurnGunRight(gunTurnAmt);
			return;
		}*/
		
		// Our target is close.
		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		//setTurnGunRight(gunTurnAmt);
		int bulletSize;
		double missHitRatio = 1.0;
		if (missCount + hitCount > 40)
			missHitRatio = (double)missCount / (double)hitCount;
		if (e.getDistance() > (800.0 * missHitRatio))
			return;
		else if (e.getDistance() > (500.0 * missHitRatio))
			bulletSize = 1;
		else if (e.getDistance() > (150.0 * missHitRatio))
			bulletSize = 2;
		else
			bulletSize = 3;
		
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

		fire(bulletSize);


		//scan();
	}
	
	/**
	 * onWin:  Do a victory dance
	 */
	public void onWin(WinEvent e) {
		setAhead(0);
		for (int i = 0; i < 50; i++) {
			turnRight(30);
			turnLeft(30);
		}
	}
	
	public void onBulletHit(BulletHitEvent e){
		hitCount++;
	}
	
	public void onBulletMissed(BulletMissedEvent e){
		missCount++;
	}
	
	public void onDeath(DeathEvent e) {
		wallTurnAngle = -wallTurnAngle;
	}
}
