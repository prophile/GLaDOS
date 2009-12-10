package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

abstract public class Movement
{
	protected WallE owner;
	public void init (WallE bot)
	{
		owner = bot;
	}
	
	abstract void update ();
	abstract void onHitWall (HitWallEvent e);
	abstract void onHitRobot (HitRobotEvent e);
	abstract void onHitByBullet (HitByBulletEvent e);
	abstract void detectedShot (ScannedRobotEvent e, double shotPower);
}
