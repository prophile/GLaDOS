package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

public class RandomTargetting extends Targetting
{
	public void update ()
	{
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		double actualAngle = Utils.normalAbsoluteAngle(e.getBearingRadians() + owner.getHeadingRadians());
		double randomFactor = owner.randomNumberGenerator().nextDouble();
		randomFactor *= Math.PI * 0.26;
		randomFactor -= Math.PI * 0.13;
		return actualAngle + randomFactor;
	}
}
