package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.Random;
import java.awt.geom.*;     // for Point2D's
import java.util.ArrayList; // for collection of waves

public class WaveSurfMovement extends Movement
{
	// movement
	public static int BINS = 47;
	public static double surfStats[] = new double[BINS];
	public Point2D.Double selfLocation;     // our bot's location
	public Point2D.Double enemyLocation;  // enemy bot's location

	public ArrayList<EnemyWave> enemyWaves;
	public ArrayList<Integer> surfDirections;
	public ArrayList<Double> surfAbsoluteBearings;

	/** This is a rectangle that represents an 800x600 battle field,
	 * used for a simple, iterative WallSmoothing method (by PEZ).
	 * If you're not familiar with WallSmoothing, the wall stick indicates
	 * the amount of space we try to always have on either end of the tank
	 * (extending straight out the front or back) before touching a wall.
	 */
	public static Rectangle2D.Double _fieldRect
	    = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
	public static double WALL_STICK = 160;

	public void init(WallE bot)
	{
		owner = bot;
		enemyWaves = new ArrayList<EnemyWave>();
		surfDirections = new ArrayList<Integer>();
		surfAbsoluteBearings = new ArrayList<Double>();
	}

	public void onHitByBullet(HitByBulletEvent e)
	{
		// If the enemyWaves collection is empty, we must have missed the
		// detection of this wave somehow.
		if (!enemyWaves.isEmpty())
		{
			Point2D.Double hitBulletLocation = new Point2D.Double(
			    e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for (int x = 0; x < enemyWaves.size(); x++)
			{
				EnemyWave ew = (EnemyWave)enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled -
				             selfLocation.distance(ew.fireLocation)) < 50
				    && Math.round(bulletVelocity(e.getBullet().getPower()) * 10)
				    == Math.round(ew.bulletVelocity * 10))
				{
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null)
			{
				logHit(hitWave, hitBulletLocation);

				// We can remove this wave now, of course.
				enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
			}
		}
	}

	public Point2D.Double predictPosition(EnemyWave surfWave, int direction)
	{
		Point2D.Double predictedPosition = (Point2D.Double)selfLocation.clone();
		double predictedVelocity = owner.getVelocity();
		double predictedHeading = owner.getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do
		{ // the rest of these code comments are rozu's
			moveAngle =
			    wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
			                                                     predictedPosition) + (direction * (Math.PI/2)), direction)
			    - predictedHeading;
			moveDir = 1;

			if(Math.cos(moveAngle) < 0)
			{
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this in one tick
			maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
			                                             + limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity +=
			    (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
			predictedVelocity = limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
			                            predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.fireLocation) <
			    surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
			    + surfWave.bulletVelocity)
			{
				intercepted = true;
			}
		} while(!intercepted && counter < 500);

		return predictedPosition;
	}

	public double checkDanger(EnemyWave surfWave, int direction)
	{
		int index = getFactorIndex(surfWave,
		                           predictPosition(surfWave, direction));

		return surfStats[index];
	}

	public void doSurfing()
	{
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null)
		{ return; }

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = absoluteBearing(surfWave.fireLocation, selfLocation);
		if (dangerLeft < dangerRight)
		{
			goAngle = wallSmoothing(selfLocation, goAngle - (Math.PI/2), -1);
		} else
		{
			goAngle = wallSmoothing(selfLocation, goAngle + (Math.PI/2), 1);
		}

		setBackAsFront(owner, goAngle);
	}

	public void onHitWall(HitWallEvent e)
	{
	}

	public void onHitRobot(HitRobotEvent e)
	{
	}

	public void updateWaves()
	{
		for (int x = 0; x < enemyWaves.size(); x++)
		{
			EnemyWave ew = (EnemyWave)enemyWaves.get(x);

			ew.distanceTraveled = (owner.getTime() - ew.fireTime) * ew.bulletVelocity;
			if (ew.distanceTraveled >
			    selfLocation.distance(ew.fireLocation) + 50)
			{
				enemyWaves.remove(x);
				x--;
			}
		}
	}

	public EnemyWave getClosestSurfableWave()
	{
		double closestDistance = 50000; // I juse use some very big number here
		EnemyWave surfWave = null;

		for (int x = 0; x < enemyWaves.size(); x++)
		{
			EnemyWave ew = (EnemyWave)enemyWaves.get(x);
			double distance = selfLocation.distance(ew.fireLocation)
			                  - ew.distanceTraveled;

			if (distance > ew.bulletVelocity && distance < closestDistance)
			{
				surfWave = ew;
				closestDistance = distance;
			}
		}

		return surfWave;
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation)
	{
		double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
		                      - ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
		                / maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int)limit(0,
		                  (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
		                  BINS - 1);
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, update our stat array to reflect the danger in that area.
	public void logHit(EnemyWave ew, Point2D.Double targetLocation)
	{
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < BINS; x++)
		{
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...
			surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}

	public void update ()
	{
		updateWaves();
		doSurfing();
	}

	public void enemyPosition (ScannedRobotEvent e, double x, double y)
	{
		enemyLocation = new Point2D.Double(x, y);

		selfLocation = new Point2D.Double(owner.getX(), owner.getY());

		double lateralVelocity = owner.getVelocity()*Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + owner.getHeadingRadians();

		surfDirections.add(0,
		                   new Integer((lateralVelocity >= 0) ? 1 : -1));
		surfAbsoluteBearings.add(0, new Double(absBearing + Math.PI));
	}

	public void detectedShot (ScannedRobotEvent e, double shotPower)
	{
		EnemyWave ew = new EnemyWave();
		ew.fireTime = owner.getTime() - 1;
		ew.bulletVelocity = bulletVelocity(shotPower);
		ew.distanceTraveled = bulletVelocity(shotPower);
		ew.direction = ((Integer)surfDirections.get(2)).intValue();
		ew.directAngle = ((Double)surfAbsoluteBearings.get(2)).doubleValue();
		ew.fireLocation = (Point2D.Double)enemyLocation.clone();

		enemyWaves.add(ew);
	}

	class EnemyWave
	{
		Point2D.Double fireLocation;
		long fireTime;
		double bulletVelocity, directAngle, distanceTraveled;
		int direction;

		public EnemyWave()
		{
		}
	}

	public static double wallSmoothing(Point2D.Double botLocation, double angle, int orientation)
	{
		while (!_fieldRect.contains(project(botLocation, angle, WALL_STICK)))
		{
			angle += orientation*0.05;
		}
		return angle;
	}

	public static Point2D.Double project(Point2D.Double sourceLocation,
	                                     double angle, double length)
	{
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
		                          sourceLocation.y + Math.cos(angle) * length);
	}

	public static double absoluteBearing(Point2D.Double source, Point2D.Double target)
	{
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public static double limit(double min, double value, double max)
	{
		return Math.max(min, Math.min(value, max));
	}

	public static double bulletVelocity(double power)
	{
		return (20.0 - (3.0*power));
	}

	public static double maxEscapeAngle(double velocity)
	{
		return Math.asin(8.0/velocity);
	}

	public static void setBackAsFront(AdvancedRobot robot, double goAngle)
	{
		double angle =
		    Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI/2))
		{
			if (angle < 0)
			{
				robot.setTurnRightRadians(Math.PI + angle);
			} else
			{
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else
		{
			if (angle < 0)
			{
				robot.setTurnLeftRadians(-1*angle);
			} else
			{
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}
}
