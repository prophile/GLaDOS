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
	private static int strategyNumber = 3;
	private static int games[];
	private static double energyAtEnd[];
	// change this to true to enable learning
	private static boolean isLearning = true;
	
	private boolean loadIdeal()
	{
		File dataFile = getDataFile(enemyName + ".nemesis");
		try
		{
			FileInputStream inputStream = new FileInputStream(dataFile);
			int movementStrategyNumber = inputStream.read() - 'A';
			int targettingStrategyNumber = inputStream.read() - 'A';
			strategyNumber = movementStrategyNumber*3 + targettingStrategyNumber;
			inputStream.close();
			isLearning = false;
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	private void storeIdeal(int strategy)
	{
		File dataFile = getDataFile(enemyName + ".nemesis");
		try
		{
			RobocodeFileOutputStream outputStream = new RobocodeFileOutputStream(dataFile);
			outputStream.write((strategy / 3) + 'A');
			outputStream.write((strategy % 3) + 'A');
			outputStream.close();
		}
		catch (Exception e)
		{
			// do nothing, it doesn't matter
		}
	}
	
	private void setupStrategies ()
	{
		int movementStrategyNumber = strategyNumber / 3;
		int targettingStrategyNumber = strategyNumber % 3;
		
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
		
		//switch (targettingStrategyNumber)
		//{
		//case 0:
		//	targettingStrategy = new CircularTargetting();
		//	break;
		//case 1:
		//	targettingStrategy = new NaiveTargetting();
		//	break;
		//case 2:
		//	targettingStrategy = new LinearTargetting();
		//	break;
		//}
		targettingStrategy = new VirtualGunTargetting();
		targettingStrategy.init(this);
	}
	
	public Random randomNumberGenerator ()
	{
		return randomNumberGenerator;
	}
	
	public void run ()
	{
		// set the colours
		setBodyColor(Color.black);
		setGunColor(Color.blue);
		setRadarColor(Color.black);
		setBulletColor(Color.black);
		setScanColor(Color.white);
		
		randomNumberGenerator = new Random();
		
		if (isLearning)
		{
			strategyNumber = randomNumberGenerator.nextInt(9);
		}
		
		if (getOthers() > 1)
		{
			// this is a melee
			enemyName = "melee";
			isLearning = false;
			strategyNumber = 6 + randomNumberGenerator.nextInt(3);
		}
		else if (enemyName == null)
		{
			if (isLearning)
			{
				games = new int[9];
				energyAtEnd = new double[9];
				for (int i = 0; i < 9; i++)
				{
					games[i] = 0;
					energyAtEnd[i] = 0.0;
				}
			}
		}
		if (isLearning)
		{
			games[strategyNumber]++;
		}
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		gunRotation = defaultGunRotationSpeed;
		
		setupStrategies();
		
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
				if (++trackingCount == 12)
					isTracking = false;
			}
			else
			{
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
				gunRotation = 0;
			}
			// call execute
			execute();
			// set up the colour scheme
			if (getEnergy() < 30.0)
				setGunColor(Color.red);
			else if (isTracking && expectedEnemyEnergy < 30.0)
				setGunColor(Color.green);
			else if (isTracking)
				setGunColor(Color.yellow);
			else
				setGunColor(Color.blue);
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
	
	public void onPaint(Graphics2D graphics)
	{
		movementStrategy.onPaint(graphics);
		targettingStrategy.onPaint(graphics);
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
				if (loadIdeal())
					setupStrategies();
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
		targettingStrategy.enemyPosition(e, enemyX, enemyY);
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
		setTurnRadarRightRadians(2.0*radarSwivel);
		// OPEN FIRE
		if (getGunHeat() < 0.1 && !(getEnergy() < 50.0 && e.getDistance() > 500.0))
			setFire(bulletPower);
		didSeeEnemy = true;
	}
	
	public void onWin(WinEvent e)
	{
		setAhead(0);
		// do a little victory dance
		energyAtEnd[strategyNumber] += getEnergy();
		while (true)
		{
			setTurnRightRadians(Double.POSITIVE_INFINITY);
			setTurnGunLeftRadians(Double.POSITIVE_INFINITY);
			execute();
		}
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
	
	public void onBattleEnded(BattleEndedEvent event)
	{
		if (isLearning)
		{
			// select the strategy with the fewest losses
			double mostEnergy = 0.0;
			int bestStrategy = -1;
			for (int i = 0; i < 9; i++)
			{
				if (games[i] == 0)
					continue;
				double energy = energyAtEnd[i] / (double)games[i];
				if (energy > mostEnergy)
				{
					mostEnergy = energy;
					bestStrategy = i;
				}
			}
			if (bestStrategy != -1)
			{
				storeIdeal(bestStrategy);
			}
		}
	}
}
