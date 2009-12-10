/**
 *  _  _  _ _______                   _______
 *  |  |  | |_____| |      |      ___ |______
 *  |__|__| |     | |_____ |_____     |______
 *
 *            Takes out the trash.
 *
 * A bot by Chris Kirkham, Alistair Lynn & Dan May
 */

package cda;

import robocode.*;
import robocode.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Random;
import java.util.PriorityQueue;

public class WallE extends AdvancedRobot
{
	// tracking
	double lastEnemyHeading = 0.0;
	int trackingCount;
	double gunRotation;
	boolean isTracking;
	
	// random number generation
	Random randomNumberGenerator;
	
	// shot tracking
	double expectedEnemyEnergy = 100.0;
	PriorityQueue<Long> expectedBullets;
	
	// movement
	static double wallTurnAngle = Math.PI * 0.5;
	boolean bulletDodgeFreeze = false;
	boolean isReversed = false;
	
	public void run ()
	{
		// set the colours
		setBodyColor(Color.black);
		setGunColor(Color.yellow);
		setRadarColor(Color.black);
		setBulletColor(Color.black);
		setScanColor(Color.white);
		
		randomNumberGenerator = new Random();
		expectedBullets = new PriorityQueue<Long>();
		
		// turn to a right angle to the walls
		turnLeftRadians(getHeadingRadians() % (Math.PI * 0.5));
		
		setAdjustGunForRobotTurn(true);
		double defaultGunRotationSpeed = Math.PI / 15.0;
		gunRotation = defaultGunRotationSpeed;
		
		while (true)
		{
			// turn gun by the turn amount
			setTurnGunRightRadians(gunRotation);
			// check for bullets
			long currentTime = getTime();
			Long firstBullet = expectedBullets.peek();
			if (firstBullet != null && (firstBullet.longValue() - currentTime < 20))
			{
				setAhead(0);
				if (firstBullet.longValue() < currentTime)
					expectedBullets.poll();
			}
			if (isReversed)
				setAhead(Double.NEGATIVE_INFINITY);
			else
				setAhead(Double.POSITIVE_INFINITY);
			// if tracking, increment trackingCount and:
			if (isTracking)
			{
				trackingCount++;
				switch (trackingCount)
				{
					case 3:
						gunRotation = -defaultGunRotationSpeed;
					case 6:
						gunRotation = defaultGunRotationSpeed;
						break;
					case 12: // give up the hunt
						isTracking = false;
						break;
				}
			}
			// call execute
			execute();
			// set up the colour scheme
			if (getEnergy() < 30.0)
				setGunColor(Color.red);
			else if (expectedEnemyEnergy < 30.0)
				setGunColor(Color.green);
			else
				setGunColor(Color.yellow);
		}
	}
	
	public void onHitWall(HitWallEvent e)
	{
		// do an immediate turn
		if (isReversed)
			turnRightRadians(wallTurnAngle);
		else
			turnLeftRadians(wallTurnAngle);
	}
	
	public void onHitRobot(HitRobotEvent e)
	{
		double bearing = e.getBearingRadians();
		if (Math.abs(bearing) < Math.PI * 0.5)
		{
			// if he's in front of us and we're evenly matched, back off then plough in again
			if (e.getEnergy() < getEnergy()*1.2)
				back(70);
			else // otherwise back the hell away
				isReversed = !isReversed;
		}
		else
		{
			// otherwise plough on ahead
			ahead(70);
		}
		// also, OPEN FIRE
		fire(3);
	}
	
	static private double bulletSpeed ( double power )
	{
		// this is a fairly simple formula
		return 20.0 - (3.0 * power);
	}
	
	public void onScannedRobot(ScannedRobotEvent e)
	{
		// if we don't have a target, set the target and record energy
		if (!isTracking)
		{
			isTracking = true;
			expectedEnemyEnergy = e.getEnergy();
		}
		// if we do, check the energy to see if a shot has been fired
		else
		{
			if (e.getEnergy() < expectedEnemyEnergy)
			{
				// insert into the list
				if (e.getDistance() > 600)
				{
					double power = expectedEnemyEnergy - e.getEnergy();
					long targetTime = getTime() + (long)(e.getDistance() / bulletSpeed(power)); 
					expectedBullets.add(new Long(targetTime));
				}
				// if one has, toggle bulletDodgeFreeze
				bulletDodgeFreeze = !bulletDodgeFreeze;
				// if bulletDodgeFreeze is false, call resume
				if (bulletDodgeFreeze == false)
					resume();
				// otherwise, call stop
				else
					stop();
				expectedEnemyEnergy = e.getEnergy();
			}
		}
		// update the turn rate so that next frame we'll be pointing correctly
		gunRotation = Utils.normalRelativeAngle(e.getBearingRadians() + (getHeadingRadians() - getRadarHeadingRadians()));
		// reset trackingCount to zero, since we know exactly where our target is
		trackingCount = 0;
		// if we have less than 50 energy, and it's more than 500 pixels away, abort now
		if (getEnergy() < 50.0 && e.getDistance() > 500.0)
			return; // this prevents us wasting energy on shots when we're far away
		
		// circular tracking: an algorithm from The Wiki™
		// calculate the bullet power as min(3, our energy)
		double bulletPower = Math.min(getEnergy(), 3.0);
		// get their distance
		double distance = e.getDistance();
		// get our position
		double selfX = getX();
		double selfY = getY();
		// get the angle to our opponent
		double enemyAngle = e.getBearingRadians() + getHeadingRadians();
		// ...and figure out their position
		// sin and cos are transposed from normal here because angles start at north rather than east
		// and they increase clockwise rather than anticlockwise
		double enemyX = selfX + distance*Math.sin(enemyAngle);
		double enemyY = selfY + distance*Math.cos(enemyAngle);
		// get their heading
		double enemyHeading = e.getHeadingRadians();
		// get their change in heading (for calculating distance away)
		double dEnemyHeading = enemyHeading - lastEnemyHeading;
		lastEnemyHeading = enemyHeading;
		// and their speed
		double enemySpeed = e.getVelocity();
		
		// get the battlefield width and height (for wall detection)
		double battlefieldWidth = getBattleFieldWidth();
		double battlefieldHeight = getBattleFieldHeight();
		// set initial predictions to their current position and heading
		double predictedX = enemyX;
		double predictedY = enemyY;
		double predictedHeading = enemyHeading;
		// we loop over until the current time + deltaTime
		for (double dt = 0.0;
	         dt * bulletSpeed(bulletPower) < Point2D.Double.distance(selfX, selfY, predictedX, predictedY);
	         dt = dt + 1.0)
	    {
		// at each loop, we work out where they will be, and the distance from that to us
		// as soon as we get a sample which is in range, we use that as the prediction
			// work out the velocity
			double velocityX = enemySpeed * Math.sin(predictedHeading);
			double velocityY = enemySpeed * Math.cos(predictedHeading);
			// add the velocity to the predicted position
			predictedX += velocityX;
			predictedY += velocityY;
			// add the change in heading to the predicted heading
			predictedHeading += dEnemyHeading;
			// if they're 10 or fewer units away from any wall, aim at 5 away as they'll probably stop,
			boolean wallCollision = false;
			if (predictedX < 10.0)
			{
				predictedX = 5.0;
				wallCollision = true;
			}
			else if (predictedX > (battlefieldWidth - 10.0))
			{
				predictedX = battlefieldWidth - 5.0;
				wallCollision = true;
			}
			if (predictedY < 10.0)
			{
				predictedY = 5.0;
				wallCollision = true;
			}
			else if (predictedY > (battlefieldHeight - 10.0))
			{
				predictedY = battlefieldHeight - 5.0;
				wallCollision = true;
			}
			// then exit the loop
			if (wallCollision)
				break;
		}
		// get the target angle via atan2
		// again, x and y are transposed: same reason as sin and cos being transposed
		double targetAngle = Utils.normalAbsoluteAngle(Math.atan2(predictedX - selfX, predictedY - selfY));
		// rotate the gun by the relevant amount
		double gunSwivel = Utils.normalRelativeAngle(targetAngle - getGunHeadingRadians());
		setTurnGunRightRadians(gunSwivel);
		// rotate the radar SEPARATELY so that we keep it pointing at our opponent and not their predicted position
		double radarSwivel = Utils.normalRelativeAngle(enemyAngle - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarSwivel);
		// OPEN FIRE
		fire(bulletPower);
	}
	
	public void onWin(WinEvent e)
	{
		setAhead(0);
		// do a little victory dance
		while (true)
		{
			setTurnRightRadians(Double.POSITIVE_INFINITY);
			setTurnGunLeftRadians(Double.POSITIVE_INFINITY);
			execute();
		}
	}
	
	public void onHitByBullet(HitByBulletEvent event)
	{
		// randomly swap walls and directions
		switch (randomNumberGenerator.nextInt(7))
		{
		case 0:
			turnLeftRadians(wallTurnAngle);
			wallTurnAngle = -wallTurnAngle;
			break;
		case 1:
			isReversed = true;
			break;
		}
		// opponent will have gained energy
		expectedEnemyEnergy += robocode.Rules.getBulletDamage(event.getBullet().getPower());
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
		// we hit the opponent, so we now have a new guess as to their energy
		expectedEnemyEnergy = e.getEnergy();
	}
}
