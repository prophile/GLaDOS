package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.Random;
import java.util.ArrayList;

public class WaveSurfMovement extends Movement
{
	// stats about the dangerous and safe parts of the waves
	private static int dangerZoneCount = 47;
	private static double dangerZones[] = new double[dangerZoneCount];
	
	// waves we know about
	private ArrayList<Wave> waves;
	// information about the enemy
	private double enemyX, enemyY;
	private int enemyOrientation;
	private double enemyAbsoluteBearing;
	
	private int enemyOrientation1, enemyOrientation2;
	private double enemyAbsoluteBearing1, enemyAbsoluteBearing2;
	
	public void init(WallE bot)
	{
		owner = bot;
		waves = new ArrayList<Wave>();
	}
	
	public void onPaint(Graphics2D graphics)
	{
		Wave target = surfTarget();
		for (Wave wave : waves)
		{
			if (target == wave)
				graphics.setColor(Color.green);
			else
				graphics.setColor(Color.red);
			double radius = wave.distanceTravelled;
			graphics.draw(new Ellipse2D.Double(wave.fireX - radius, wave.fireY - radius, radius * 2.0, radius * 2.0));
		}
	}
	
	private Wave surfTarget ()
	{
		double minimumDistance = Double.POSITIVE_INFINITY;
		Wave target = null;
		
		for (Wave wave : waves)
		{
			double distanceToSource = Point2D.Double.distance(owner.getX(), owner.getY(), wave.fireX, wave.fireY);
			double distance = distanceToSource - wave.distanceTravelled;
			if (distance <= wave.speed) // if it's going to pass us before we move, ignore it
				continue;
			if (distance < minimumDistance)
			{
				target = wave;
				minimumDistance = distance;
			}
		}
		
		return target;
	}
	
	public void onHitWall(HitWallEvent e)
	{
	}
	
	public void onHitRobot(HitRobotEvent e)
	{
	}
	
	public void update ()
	{
		// update all waves
		for (int i = 0; i < waves.size(); i++)
		{
			Wave wave = waves.get(i);
			// bump the travel distance
			wave.distanceTravelled = (owner.getTime() - wave.fireTime) * wave.speed;
			double distanceToSource = Point2D.Double.distance(owner.getX(), owner.getY(), wave.fireX, wave.fireY);
			if (wave.distanceTravelled > (distanceToSource + 40))
			{
				// no longer a problem
				waves.remove(i);
				i--;
			}
		}
		// move away from the wall if we're rammed
		//double battleFieldWidth = owner.getBattleFieldWidth();
		//double battleFieldHeight = owner.getBattleFieldHeight();
		//double x = owner.getX();
		//double y = owner.getY();
		//if (x < 51.0 || y < 51.0 || x > (battleFieldWidth - 51) || y > (battleFieldHeight - 51))
		//{
		//   directMove(Math.atan2((battleFieldWidth/2.0) - x, (battleFieldHeight/2.0) - y));
		//   return;
		//}
		// find nearest and surf it
		Wave target = surfTarget();
		if (target == null) // no waves yet, I guess?
		{
			owner.setAhead(0);
			return;
		}
		
		// pick a direction to surf
		int direction;
		if (target.dangerLevel(owner, -1) < target.dangerLevel(owner, 1))
			direction = -1;
		else
			direction = 1;
		
		double angle = Math.atan2(owner.getX() - target.fireX, owner.getY() - target.fireY);
		angle = Movement.wallSmooth(owner, owner.getX(), owner.getY(), angle + direction*(Math.PI*0.5), direction);

		directMove(angle);
	}
	
	public void onHitByBullet(HitByBulletEvent e)
	{
		// if we know of no waves, ignore it
		if (waves.isEmpty())
			return;
		double hitX = e.getBullet().getX();
		double hitY = e.getBullet().getY();
		
		Wave targetWave = null;
		
		// look through the waves to determine which one it was
		for (Wave wave : waves)
		{
			double distanceToSource = Point2D.Double.distance(owner.getX(), owner.getY(), wave.fireX, wave.fireY);
			if (Math.abs(distanceToSource - wave.distanceTravelled) < 40)
			{
				// the shoe seems to fit: it's come the right distance.
				// now, check if the speeds match
				if (Math.abs(Targetting.bulletSpeed(e.getBullet().getPower()) - wave.speed) < 0.5)
				{
					targetWave = wave;
					break;
					// this was it: mark where it hit as dangerous and kill the wave
				}
			}
		}
		if (targetWave != null)
		{
			int epicentre = targetWave.getZone(hitX, hitY);
			for (int i = 0; i < dangerZoneCount; i++)
			{
				// formula from the tutorial, thanks!
				dangerZones[i] += 1.0 / (Math.pow(epicentre - i, 2) + 1);
			}
			waves.remove(waves.indexOf(targetWave));
		}
	}
	
	public void enemyPosition(ScannedRobotEvent e, double x, double y)
	{
		enemyX = x;
		enemyY = y;
		enemyAbsoluteBearing = enemyAbsoluteBearing1;
		enemyAbsoluteBearing1 = enemyAbsoluteBearing2;
		enemyAbsoluteBearing2 = e.getBearingRadians() + owner.getHeadingRadians();
		double lateralSpeed = e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing2);
		enemyOrientation = enemyOrientation1;
		enemyOrientation1 = enemyOrientation2;
		enemyOrientation2 = lateralSpeed > 0 ? 1 : -1;
	}
	
	public void detectedShot(ScannedRobotEvent e, double shotPower)
	{
		Wave wave = new Wave();
		// we detected it this frame, therefore it was fired LAST frame
		wave.fireTime = owner.getTime() - 1;
		wave.speed = Targetting.bulletSpeed(shotPower);
		wave.distanceTravelled = wave.speed; // it's travelled one frame
		wave.orientation = enemyOrientation;
		wave.angle = enemyAbsoluteBearing;
		wave.fireX = enemyX;
		wave.fireY = enemyY;
		waves.add(wave);
	}
	
	private class Wave
	{
		public double fireX, fireY;
		public long fireTime;
		public double speed, angle, distanceTravelled;
		public int orientation;
		
		public int getZone ( double x, double y )
		{
			// get the danger zone associated with a hit in a given place
			double angleOffset = Math.atan2(x - fireX, y - fireY) - angle;
			double factor = Utils.normalRelativeAngle(angleOffset) /
			                Math.asin(8.0/speed) * orientation; // the asin is the maximum escape angle
			int zone = (int)((factor * ((dangerZoneCount - 1) / 2)) + ((dangerZoneCount - 1) / 2));
			if (zone >= dangerZoneCount) zone = dangerZoneCount - 1;
			if (zone < 0) zone = 0;
			return zone;
		}
		
		public double dangerLevel ( WallE bot, int orientation )
		{
			double predictedX = bot.getX(), predictedY = bot.getY();
			double predictedSpeed = bot.getVelocity();
			double predictedHeading = bot.getHeadingRadians();
			
			double dt;
			for (dt = 0.0; dt < 500.0; dt += 1.0)
			{
				double moveAngle = Movement.wallSmooth(bot, predictedX, predictedY, Math.atan2(predictedX - fireX, predictedY - fireY) + (orientation * Math.PI*0.5), orientation) - predictedHeading;
				double moveDirection = 1;
				
				// catch the case where we have to back out
				if (Math.cos(moveAngle) < 0.0)
				{
					moveAngle += Math.PI;
					moveDirection = -1;
				}
				
				moveAngle = Utils.normalRelativeAngle(moveAngle);
				
				// a formula shamelessly lifted
				double maxTurn = Math.PI/720.0*(40.0 - 3.0*Math.abs(predictedSpeed));
				if (moveAngle > maxTurn)
					moveAngle = maxTurn;
				else if (moveAngle < -maxTurn)
					moveAngle = -maxTurn;
				predictedHeading = Utils.normalRelativeAngle(predictedHeading + moveAngle);
				predictedSpeed += ((predictedSpeed * moveDirection)<0.0) ? (2.0*moveDirection) : moveDirection;
				
				if (predictedSpeed > 8.0)
					predictedSpeed = 8.0;
				if (predictedSpeed < -8.0)
					predictedSpeed = -8.0;
				
				predictedX += Math.sin(predictedHeading)*predictedSpeed;
				predictedY += Math.cos(predictedHeading)*predictedSpeed;

				// intercept condition: we've caught the wave!
				if (Point2D.Double.distance(predictedX, predictedY, fireX, fireY) <
				    distanceTravelled + (dt * speed) + dt)
				{
					break;
				}
			}
			
			return dangerZones[getZone(predictedX, predictedY)];
		}
	}
}
