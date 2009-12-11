package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

public class NaiveTargetting extends Targetting
{
	public void update ()
	{
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		return Utils.normalAbsoluteAngle(e.getBearingRadians() + owner.getHeadingRadians());
	}
}
