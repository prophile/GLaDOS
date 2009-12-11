package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.Random;

public class AntiGravityMovement extends Movement
{
	// movement
	double enemyX, enemyY;
	double randomForceX1, randomForceY1;
	double randomForceX2, randomForceY2;
	double randomForceX3, randomForceY3;
	double randomForceX4, randomForceY4;
	int enemyPresenceIndicator = 0;
	boolean isReversing = false;
	int reverseCount = 0;
	
	public void init(WallE bot)
	{
		owner = bot;
		Random random = bot.randomNumberGenerator();
		double width = bot.getBattleFieldWidth();
		double height = bot.getBattleFieldHeight();
		randomForceX1 = random.nextDouble() * width;
		randomForceX2 = random.nextDouble() * width;
		randomForceX3 = random.nextDouble() * width;
		randomForceX4 = random.nextDouble() * width;
		randomForceY1 = random.nextDouble() * height;
		randomForceY2 = random.nextDouble() * height;
		randomForceY3 = random.nextDouble() * height;
		randomForceY4 = random.nextDouble() * height;
	}
	
	public void onHitByBullet(HitByBulletEvent e)
	{
	}
	
	public void onHitWall(HitWallEvent e)
	{
		owner.setTurnLeft(Math.PI);
	}
	
	public void onHitRobot(HitRobotEvent e)
	{
	}
	
	private double wallForce ( double val, double influenceFactor )
	{
		return influenceFactor / (val*val);
	}
	
	private static double pointForceX ( double x, double y, double influenceFactor )
	{
		double oppositeX = x;
		double influence = influenceFactor / (x*x + y*y);
		oppositeX *= influence;
		return oppositeX;
	}
	
	private static double pointForceY ( double x, double y, double influenceFactor )
	{
		double oppositeY = y;
		double influence = influenceFactor / (x*x + y*y);
		oppositeY *= influence;
		return oppositeY;
	}
	
	private static double pointForceLinearX ( double x, double y, double influenceFactor )
	{
		double oppositeX = x;
		double influence = influenceFactor / (x*x + y*y);
		oppositeX *= influence;
		return oppositeX;
	}
	
	private static double pointForceLinearY ( double x, double y, double influenceFactor )
	{
		double oppositeY = y;
		double influence = influenceFactor / (x*x + y*y);
		oppositeY *= influence;
		return oppositeY;
	}
	
	public void update ()
	{
		double x = owner.getX();
		double y = owner.getY();
		double forceX = 0.0, forceY = 0.0;
		// left wall pushing force
		forceX += wallForce(x, 200.0);
		// bottom wall pushing force
		forceY += wallForce(y, 200.0);
		// top wall pushing force
		forceY -= wallForce(owner.getBattleFieldHeight() - y, 200.0);
		// right wall pushing force
		forceX -= wallForce(owner.getBattleFieldWidth() - x, 200.0);
		// enemy force
		enemyPresenceIndicator--;
		if (enemyPresenceIndicator > 0)
		{
			forceX += pointForceLinearX(enemyX - x, enemyY - y, -10.0);
			forceY += pointForceLinearY(enemyX - x, enemyY - y, -10.0);
		}
		
		/*if (owner.randomNumberGenerator().nextInt(40) == 4)
			isReversing = !isReversing;*/
		
		// random point force
		forceX += pointForceX(randomForceX1 - x, randomForceY1 - y, 50.0);
		forceY += pointForceY(randomForceX1 - x, randomForceY1 - y, 50.0);
		forceX += pointForceX(randomForceX2 - x, randomForceY2 - y, 50.0);
		forceY += pointForceY(randomForceX2 - x, randomForceY2 - y, 50.0);
		forceX += pointForceX(randomForceX3 - x, randomForceY3 - y, 50.0);
		forceY += pointForceY(randomForceX3 - x, randomForceY3 - y, 50.0);
		forceX += pointForceX(randomForceX4 - x, randomForceY4 - y, 50.0);
		forceY += pointForceY(randomForceX4 - x, randomForceY4 - y, 50.0);
		
		// centre of field force
		forceX += pointForceX((owner.getBattleFieldWidth()*0.5 - x), (owner.getBattleFieldHeight()*0.5 - y), 30.0);
		forceY += pointForceY((owner.getBattleFieldWidth()*0.5 - x), (owner.getBattleFieldHeight()*0.5 - y), 30.0);
	
		double angle = Utils.normalAbsoluteAngle(Math.atan2(forceX, forceY));
		// do rotation
		double rotation = Utils.normalRelativeAngle(angle - owner.getHeadingRadians());
		double totalForceMagnitude = (forceX*forceX) + (forceY*forceY);
		owner.setBodyColor(new Color(0.0f, Math.min(1.0f, (float)totalForceMagnitude * 0.1f), 0.0f));
		if (isReversing)
		{
			rotation = -rotation;
		}
		owner.setTurnRightRadians(rotation);
		if (isReversing)
			owner.setAhead(Double.NEGATIVE_INFINITY);
		else
			owner.setAhead(Double.POSITIVE_INFINITY);
	}
	
	public void enemyPosition (ScannedRobotEvent e, double x, double y)
	{
		enemyX = x;
		enemyY = y;
		enemyPresenceIndicator = 10;
	}
	
	public void detectedShot (ScannedRobotEvent e, double shotPower)
	{
		// if one has, toggle bulletDodgeFreeze
		reverseCount++;
		if (reverseCount % 2 == 0)
			isReversing = !isReversing;
	}
}
