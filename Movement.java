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
