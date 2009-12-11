package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

public class LinearTargetting extends Targetting
{
	public void update ()
	{
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		// get their distance
		double distance = e.getDistance();
		// get our position
		double selfX = owner.getX();
		double selfY = owner.getY();
		// get the angle to our opponent
		double enemyAngle = e.getBearingRadians() + owner.getHeadingRadians();
		// ...and figure out their position
		// sin and cos are transposed from normal here because angles start at north rather than east
		// and they increase clockwise rather than anticlockwise
		double enemyX = selfX + distance*Math.sin(enemyAngle);
		double enemyY = selfY + distance*Math.cos(enemyAngle);
		// get their heading
		double enemyHeading = e.getHeadingRadians();
		// and their speed
		double enemySpeed = e.getVelocity();
		
		// get the battlefield width and height (for wall detection)
		double battlefieldWidth = owner.getBattleFieldWidth();
		double battlefieldHeight = owner.getBattleFieldHeight();
		// set initial predictions to their current position and heading
		double predictedX = enemyX;
		double predictedY = enemyY;
		// we loop over until the current time + deltaTime
		for (double dt = 0.0;
	         dt * bulletSpeed(bulletPower) < Point2D.Double.distance(selfX, selfY, predictedX, predictedY);
	         dt = dt + 1.0)
	    {
		// at each loop, we work out where they will be, and the distance from that to us
		// as soon as we get a sample which is in range, we use that as the prediction
			// work out the velocity
			double velocityX = enemySpeed * Math.sin(enemyHeading);
			double velocityY = enemySpeed * Math.cos(enemyHeading);
			// add the velocity to the predicted position
			predictedX += velocityX;
			predictedY += velocityY;
			// add the change in heading to the predicted heading
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
		return targetAngle;
	}
}
