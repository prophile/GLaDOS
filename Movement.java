package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.awt.*;

abstract public class Movement
{
	protected WallE owner;
	public void init (WallE bot)
	{
		owner = bot;
	}
	
	private static final double WALL_DISTANCE = 160.0;
	
	public static double wallSmooth ( WallE bot, double x, double y, double angle, int orientation )
	{
		// a very clever bit of code: if we're about to hit the wall, round off so we miss it
		
		angle += 4*Math.PI; // ensure angle is positive :o
		
		double battleFieldWidth = bot.getBattleFieldWidth();
		double battleFieldHeight = bot.getBattleFieldHeight();
		
		// do initial projection based on WALL_DISTANCE
		double projectedX = x + Math.sin(angle)*WALL_DISTANCE;
		double projectedY = y + Math.cos(angle)*WALL_DISTANCE;
		// calculate minimum distance to wall from initial along x
		double currentDistanceX = Math.min(x - 18, battleFieldWidth - x - 18);
		// calculate minimum distance to wall from initial along y
		double currentDistanceY = Math.min(x - 18, battleFieldHeight - x - 18);
		// calculate minimum distance to wall from first projection along x
		double projectedDistanceX = Math.min(projectedX - 18, battleFieldWidth - projectedX - 18);
		// calculate minimum distance to wall from first projection along y
		double projectedDistanceY = Math.min(projectedY - 18, battleFieldHeight - projectedY - 18);
		double distance = 0.0;
		int infiniteLoopCatcher = 0;
		// while the projected point is not within the "safe" area...
		while (projectedX < 18.0 || projectedY < 18.0 || projectedX > (battleFieldWidth - 18) || projectedY > (battleFieldHeight - 18))
		{
			if (++infiniteLoopCatcher > 10)
				break;
			// if we're inside the wall along y or y is more dangerous than x
			if (projectedDistanceY < 0.0 || projectedDistanceY < projectedDistanceX)
			{
				// formula uses casts to int to round off. (int)(foo / a) * a rounds to the nearest a.
				angle = ((int)((angle + (Math.PI/2)) / Math.PI)) * Math.PI;
            	distance = Math.abs(currentDistanceY);
            }
            // otherwise if we're inside th wall along x or x is more or equally dangerous than/to y
            else if (projectedDistanceX < 0.0 || projectedDistanceX <= projectedDistanceY)
            {
            	angle = (((int)(angle / Math.PI)) * Math.PI) + (Math.PI/2);
            	distance = Math.abs(currentDistanceX);
            }
            
            // update the angle to smooth away
            angle += orientation*Math.abs(Math.acos(distance/WALL_DISTANCE)) + 0.005;
            
            // update forecasts
			projectedX = x + Math.sin(angle)*WALL_DISTANCE;
			projectedY = y + Math.cos(angle)*WALL_DISTANCE;
			projectedDistanceX = Math.min(projectedX - 18, battleFieldWidth - projectedX - 18);
			projectedDistanceY = Math.min(projectedY - 18, battleFieldHeight - projectedY - 18);
        }
		return angle;
	}
	
	public void directMove(double angle)
	{
		// head to the angle, using whichever is faster of forward or reverse
		angle = Utils.normalRelativeAngle(angle - owner.getHeadingRadians());
		if (Math.abs(angle) > Math.PI*0.5)
		{
			owner.setTurnLeftRadians(angle);
			owner.setBack(Double.POSITIVE_INFINITY);
		}
		else
		{
			owner.setTurnRightRadians(angle);
			owner.setAhead(Double.POSITIVE_INFINITY);
		}
	}
	
	abstract public void update ();
	abstract public void onHitWall (HitWallEvent e);
	abstract public void onHitRobot (HitRobotEvent e);
	abstract public void onHitByBullet (HitByBulletEvent e);
	abstract public void enemyPosition (ScannedRobotEvent e, double x, double y);
	abstract public void detectedShot (ScannedRobotEvent e, double shotPower);
	
	public void onPaint(Graphics2D graphics)
	{
	}
}
