package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class MasterMovement extends Movement
{
	private ArrayList<Force> forces;
	private static final double WALL_FORCE = 150.0;
	private static final double FORCE_EXPONENT = 1.5;
	Force enemyForce;
	
	public void init (WallE bot)
	{
		forces = new ArrayList<Force>();
		owner = bot;
		enemyForce = new Force();
		Random rng = owner.randomNumberGenerator();
		enemyForce.attractiveForce = 20.0;
		enemyForce.origin = new Vec2(0.0, 0.0, bot.getBattleFieldWidth(), bot.getBattleFieldHeight(), rng);
		forces.add(enemyForce);
		Force centreOfFieldForce = new Force();
		centreOfFieldForce.attractiveForce = -(WALL_FORCE / 2.0);
		centreOfFieldForce.origin = new Vec2(bot.getBattleFieldWidth() * 0.5, bot.getBattleFieldHeight() * 0.5);
		forces.add(centreOfFieldForce);
		for (int i = 0; i < 12; i++)
		{
			Force randomForce = new Force();
			randomForce.origin = new Vec2(0.0, 0.0, bot.getBattleFieldWidth(), bot.getBattleFieldHeight(), rng);
			randomForce.velocity = new Vec2(-9.0, -9.0, 9.0, 9.0, rng);
			randomForce.attractiveForce = rng.nextDouble();
			randomForce.attractiveForce *= 80.0;
			randomForce.attractiveForce -= 40.0;
			System.out.println("Random force: (" + randomForce.origin.x + ", " + randomForce.origin.y + ")");
			forces.add(randomForce);
		}
	}
	
	private Vec2 totalForceAtPosition ( Vec2 position )
	{
		Vec2 total = new Vec2();
		// wall forces
		double lwd = owner.getX();
		double rwd = owner.getBattleFieldWidth() - owner.getX();
		double bwd = owner.getY();
		double twd = owner.getBattleFieldHeight() - owner.getY();
		total.x += WALL_FORCE / Math.pow(lwd, FORCE_EXPONENT);
		total.x -= WALL_FORCE / Math.pow(rwd, FORCE_EXPONENT);
		total.y += WALL_FORCE / Math.pow(bwd, FORCE_EXPONENT);
		total.y -= WALL_FORCE / Math.pow(twd, FORCE_EXPONENT);
		for (Force force : forces)
		{
			Vec2 forceVector = force.origin.sub(position);
			double magnitude = forceVector.magnitude();
			Vec2 unitVector = forceVector.unit();
			double power = force.attractiveForce / Math.pow(magnitude, FORCE_EXPONENT);
			total = total.add(unitVector.mul(power));
		}
		return total;
	}
	
	private double clamp ( double val )
	{
		return Math.max(0.0, Math.min(1.0, val));
	}
	
	public void onPaint(Graphics2D graphics)
	{
		for (Force force : forces)
		{
			if (force.attractiveForce > 0.0)
				graphics.setColor(new Color(0.0f, (float)(clamp(force.attractiveForce / 150.0)), 0.0f));
			else
				graphics.setColor(new Color((float)(clamp(-force.attractiveForce / 150.0)), 0.0f, 0.0f));
			graphics.fillRect((int)(force.origin.x - 4.0), (int)(force.origin.y - 4.0), 8, 8);
		}
	}
	
	public void update ()
	{
		Vec2 position = new Vec2(owner.getX(), owner.getY());
		Vec2 totalForce = totalForceAtPosition(position);
		double angle = totalForce.angle();
		owner.setDebugProperty("MMForce", "" + totalForce.magnitude());
		//if (totalForce.magnitudeSquared() > 0.005 || Math.abs(owner.getVelocity()) < 1.0)
			directMove(angle);
		double width = owner.getBattleFieldWidth(), height = owner.getBattleFieldHeight();
		for (Force force : forces)
		{
			if (force.velocity != null)
			{
				force.origin = force.origin.add(force.velocity);
				if (force.origin.x < 0.0 || force.origin.x > width)
					force.velocity.x = -force.velocity.x;
				if (force.origin.y < 0.0 || force.origin.y > height)
					force.velocity.y = -force.velocity.y;
			}
		}
	}
	
	public void onHitWall (HitWallEvent e)
	{
	}
	
	public void onHitRobot (HitRobotEvent e)
	{
	}
	
	public void onHitByBullet (HitByBulletEvent e)
	{
	}
	
	public void enemyPosition (ScannedRobotEvent e, double x, double y)
	{
		enemyForce.origin = new Vec2(x, y);
		double forceStrength = owner.getEnergy() - e.getEnergy() - 20.0;
		if (forceStrength > 0.0)
			forceStrength = Math.pow(forceStrength, 1.6); // agressive ramming
		enemyForce.attractiveForce = forceStrength;
	}
	
	public void detectedShot (ScannedRobotEvent e, double shotPower)
	{
	}
	
	private class Vec2
	{
		public double x, y;
		
		public Vec2 ()
		{
			x = 0.0;
			y = 0.0;
		}
		
		public Vec2 ( double _x, double _y )
		{
			x = _x;
			y = _y;
		}
		
		public Vec2 ( double xmin, double ymin, double xmax, double ymax, Random rng )
		{
			x = rng.nextDouble();
			y = rng.nextDouble();
			x *= (xmax - xmin);
			y *= (ymax - ymin);
			x += xmin;
			y += ymin;
		}
		
		Vec2 add ( Vec2 vec )
		{
			return new Vec2 ( x + vec.x, y + vec.y );
		}
		
		Vec2 sub ( Vec2 vec )
		{
			return new Vec2 ( x - vec.x, y - vec.y );
		}
		
		double squareDistance ( Vec2 vec )
		{
			return ( (vec.x - x)*(vec.x - x) +
			         (vec.y - y)*(vec.y - y) );
		}
		
		double inverseSquareDistance ( Vec2 vec )
		{
			return 1.0 / squareDistance(vec);
		}
		
		double distance ( Vec2 vec )
		{
			return Math.sqrt ( (vec.x - x)*(vec.x - x) +
			                   (vec.y - y)*(vec.y - y) );
		}
		
		double magnitude ()
		{
			return Math.sqrt(x*x + y*y);
		}
		
		double magnitudeSquared ()
		{
			return x*x + y*y;
		}
		
		double angle ()
		{
			return Math.atan2(x, y);
		}
		
		Vec2 unit ()
		{
			return div(magnitude());
		}
		
		Vec2 div ( double factor )
		{
			return mul(1.0 / factor);
		}
		
		Vec2 mul ( double factor )
		{
			return new Vec2 ( x * factor, y * factor );
		}
	}
	
	private class Force
	{
		Vec2 origin;
		Vec2 velocity;
		double attractiveForce;
	}
}
