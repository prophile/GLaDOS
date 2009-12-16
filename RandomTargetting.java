package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

public class RandomTargetting extends Targetting
{
	private double angle;
	
	RandomTargetting(double targettingAngle)
	{
		angle = targettingAngle;
	}
	
	public void update ()
	{
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		double actualAngle = Utils.normalAbsoluteAngle(e.getBearingRadians() + owner.getHeadingRadians());
		double randomFactor = owner.randomNumberGenerator().nextDouble();
		randomFactor *= angle * 2.0;
		randomFactor -= angle;
		return actualAngle + randomFactor;
	}
}
