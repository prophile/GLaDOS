package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

public class WallMovement extends Movement
{
	// movement
	static double wallTurnAngle = Math.PI * 0.5;
	boolean isReversed = false;
	boolean nonAligned = true;
	boolean bulletDodgeFreeze = false;
	
	public void onHitByBullet(HitByBulletEvent e)
	{
		// randomly swap walls and directions
		switch (owner.randomNumberGenerator().nextInt(8))
		{
		case 0:
			owner.setTurnLeftRadians(wallTurnAngle);
			wallTurnAngle = -wallTurnAngle;
			break;
		case 1:
			isReversed = true;
			break;
		case 2:
			isReversed = false;
			break;
		}
	}
	
	public void enemyPosition(ScannedRobotevent e, double x, double y)
	{
	}
	
	public void onHitWall(HitWallEvent e)
	{
		// do an immediate turn
		if (isReversed)
			owner.turnRightRadians(wallTurnAngle);
		else
			owner.turnLeftRadians(wallTurnAngle);
	}
	
	public void onHitRobot(HitRobotEvent e)
	{
		double bearing = e.getBearingRadians();
		if (Math.abs(bearing) < Math.PI * 0.5)
		{
			// if he's in front of us and we're evenly matched, back off then plough in again
			if (e.getEnergy() < owner.getEnergy()*0.5)
			{
				// do nothing and keep ramming
			}
			else if (e.getEnergy() < owner.getEnergy()*1.2)
			{
				owner.back(70);
			}
			else // otherwise back the hell away
			{
				isReversed = !isReversed;
			}
		}
	}
	
	public void update ()
	{
		if (nonAligned)
		{
			double rotate = owner.getHeadingRadians() % (Math.PI * 0.5);
			if (rotate < 0.001)
				nonAligned = false;
			else
				owner.setTurnLeftRadians(rotate);
		}
		if (isReversed)
			owner.setAhead(Double.NEGATIVE_INFINITY);
		else
			owner.setAhead(Double.POSITIVE_INFINITY);
	}
	
	public void detectedShot (ScannedRobotEvent e, double shotPower)
	{
		// if one has, toggle bulletDodgeFreeze
		bulletDodgeFreeze = !bulletDodgeFreeze;
		// if bulletDodgeFreeze is false, call resume
		if (bulletDodgeFreeze == false)
			owner.resume();
		// otherwise, call stop
		else
		{
			owner.stop();
			nonAligned = true;
		}
	}
}
