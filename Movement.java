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
	
	public static double wallSmooth ( WallE bot, double x, double y, double angle, int orientation )
	{
		// a very clever bit of code: if we're about to hit the wall, round off so we miss it
		double projectedX, projectedY;
		double width = bot.getBattleFieldWidth();
		double height = bot.getBattleFieldHeight();
		while (true)
		{
			projectedX = x + Math.sin(angle) * 160.0;
			projectedY = y + Math.cos(angle) * 160.0;
			angle += orientation*0.05;
			if (!((projectedX < 18.0) || (projectedY < 18.0) || (projectedX > (width - 18.0)) || (projectedY > (height - 18.0))))
				break;
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
