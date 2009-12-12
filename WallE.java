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
import java.io.File;
import java.io.FileInputStream;

public class WallE extends AdvancedRobot
{
	// tracking
	private int trackingCount;
	private double gunRotation;
	private boolean isTracking;
	private boolean didSeeEnemy = false;
	private static String enemyName = null;
	
	// random number generation
	private Random randomNumberGenerator;
	
	// shot tracking
	private double expectedEnemyEnergy = 100.0;
	private static double defaultGunRotationSpeed = Math.PI / 15.0;
	
	// strategies
	private Movement movementStrategy;
	private Targetting targettingStrategy;
	
	// strategy selection
	private static int movementStrategyNumber = 1;
	private static int targettingStrategyNumber = 0;
	private static int wins = 0;
	private static int losses = 0;
	private static boolean isIdeal = false;
	
	private boolean loadIdeal()
	{
		File dataFile = getDataFile(enemyName + ".nemesis");
		try
		{
			FileInputStream inputStream = new FileInputStream(dataFile);
			movementStrategyNumber = inputStream.read() - 'A';
			targettingStrategyNumber = inputStream.read() - 'A';
			inputStream.close();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	private void storeIdeal()
	{
		File dataFile = getDataFile(enemyName + ".nemesis");
		try
		{
			RobocodeFileOutputStream outputStream = new RobocodeFileOutputStream(dataFile);
			outputStream.write(movementStrategyNumber + 'A');
			outputStream.write(targettingStrategyNumber + 'A');
			outputStream.close();
		}
		catch (Exception e)
		{
			// do nothing, it doesn't matter
		}
	}
	
	public Random randomNumberGenerator ()
	{
		return randomNumberGenerator;
	}
	
	public void run ()
	{
		// set the colours
		setBodyColor(Color.black);
		setGunColor(Color.yellow);
		setRadarColor(Color.black);
		setBulletColor(Color.black);
		setScanColor(Color.white);
		
		if (getOthers() > 1 && enemyName == null)
		{
			// this is a melee
			enemyName = "melee";
		}
		
		randomNumberGenerator = new Random();
		if (wins + losses >= 4 && !isIdeal)
		{
			switch (wins)
			{
				case 0:
				case 1:
				case 2: // not doing so well
					// reshuffle
					movementStrategyNumber = randomNumberGenerator.nextInt(3);
					targettingStrategyNumber = randomNumberGenerator.nextInt(3);
					wins = 0;
					losses = 0;
					break;
				case 3: // this isn't bad at all
					wins = 0;
					losses = -1; // slight bias
					break; // stick with it, see what happens
				case 4: // looks like we have our ideal choice
					if (!enemyName.equals("melee")) // if it's a melee, don't bother to log
					{
						isIdeal = true;
						// log it
						storeIdeal();
					}
					else
					{
						// it's a melee, just reset the counter
						wins = 0;
						losses = -2; // bias
					}
					break;
			}
		}
		
		switch (movementStrategyNumber)
		{
		case 0:
			movementStrategy = new WaveSurfMovement();
			break;
		case 1:
			movementStrategy = new AntiGravityMovement();
			break;
		case 2:
			movementStrategy = new WallMovement();
			break;
		}
		movementStrategy.init(this);
		
		switch (targettingStrategyNumber)
		{
		case 0:
			targettingStrategy = new CircularTargetting();
			break;
		case 1:
			targettingStrategy = new NaiveTargetting();
			break;
		case 2:
			targettingStrategy = new LinearTargetting();
			break;
		}
		targettingStrategy.init(this);
		
		setAdjustGunForRobotTurn(true);
		gunRotation = defaultGunRotationSpeed;
		
		while (true)
		{
			movementStrategy.update();
			// turn gun by the turn amount
			if (isTracking)
				targettingStrategy.update();
			if (didSeeEnemy)
				didSeeEnemy = false;
			else
				setTurnGunRightRadians(gunRotation);
			// if tracking, increment trackingCount and:
			if (isTracking)
			{
				trackingCount++;
				switch (trackingCount)
				{
					case 3:
						gunRotation = -gunRotation;
					case 6:
						gunRotation = -gunRotation;
						break;
					case 12: // give up the hunt
						isTracking = false;
						break;
				}
			}
			else
			{
				gunRotation = Double.POSITIVE_INFINITY;
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
		movementStrategy.onHitWall(e);
	}
	
	public void onHitRobot(HitRobotEvent e)
	{
		movementStrategy.onHitRobot(e);
		// also, OPEN FIRE
		fire(3);
	}
	
	public void onScannedRobot(ScannedRobotEvent e)
	{
		// if we don't have a target, set the target and record energy
		if (!isTracking)
		{
			isTracking = true;
			expectedEnemyEnergy = e.getEnergy();
			if (enemyName == null)
			{
				enemyName = e.getName();
				// try to load ideal strategies
				isIdeal = loadIdeal();
			}
		}
		// if we do, check the energy to see if a shot has been fired
		else
		{
			if (e.getEnergy() < expectedEnemyEnergy)
			{
				double power = expectedEnemyEnergy - e.getEnergy();
				movementStrategy.detectedShot(e, power);
				expectedEnemyEnergy = e.getEnergy();
			}
		}
		// reset trackingCount to zero, since we know exactly where our target is
		trackingCount = 0;
		// if we have less than 50 energy, and it's more than 500 pixels away, abort now
		if (getEnergy() < 50.0 && e.getDistance() > 500.0)
			return; // this prevents us wasting energy on shots when we're far away
		
		// circular tracking: an algorithm from The Wiki™
		// calculate the bullet power as min(rBP, our energy)
		double requestedBulletPower;
		if (e.getDistance() < 400.0)
			requestedBulletPower = 3.0;
		else
			requestedBulletPower = 2.0;
		double bulletPower = Math.min(getEnergy(), requestedBulletPower);
		double targetAngle = targettingStrategy.target(e, bulletPower);
		double enemyAngle = e.getBearingRadians() + getHeadingRadians();
		double distance = e.getDistance();
		double enemyX = getX() + distance*Math.sin(enemyAngle);
		double enemyY = getY() + distance*Math.cos(enemyAngle);
		movementStrategy.enemyPosition(e, enemyX, enemyY);
		// rotate the gun by the relevant amount
		double gunSwivel = Utils.normalRelativeAngle(targetAngle - getGunHeadingRadians());
		setTurnGunRightRadians(gunSwivel);
		if (gunSwivel > 0.0)
		{
			gunRotation = defaultGunRotationSpeed;
		}
		else
		{
			gunRotation = -defaultGunRotationSpeed;
		}
		// rotate the radar SEPARATELY so that we keep it pointing at our opponent and not their predicted position
		double radarSwivel = Utils.normalRelativeAngle(enemyAngle - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarSwivel);
		// OPEN FIRE
		if (getGunHeat() < 0.1)
			setFire(bulletPower);
		didSeeEnemy = true;
	}
	
	public void onWin(WinEvent e)
	{
		setAhead(0);
		// do a little victory dance
		wins++;
		while (true)
		{
			setTurnRightRadians(Double.POSITIVE_INFINITY);
			setTurnGunLeftRadians(Double.POSITIVE_INFINITY);
			execute();
		}
	}
	
	public void onDeath(DeathEvent e)
	{
		losses++;
	}
	
	public void onHitByBullet(HitByBulletEvent event)
	{
		movementStrategy.onHitByBullet(event);
		// opponent will have gained energy
		expectedEnemyEnergy += robocode.Rules.getBulletDamage(event.getBullet().getPower());
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
		// we hit the opponent, so we now have a new guess as to their energy
		expectedEnemyEnergy = e.getEnergy();
	}
}
