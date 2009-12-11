package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

abstract public class Targetting
{
	protected WallE owner;
	public void init (WallE bot)
	{
		owner = bot;
	}
	
	static public double bulletSpeed ( double power )
	{
		// this is a fairly simple formula
		return 20.0 - (3.0 * power);
	}
	
	abstract public void update();
	abstract public double target(ScannedRobotEvent e, double bulletPower);
}
