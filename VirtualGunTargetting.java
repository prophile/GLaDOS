package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.awt.*;

public class VirtualGunTargetting extends Targetting
{
	static Targetting[] targetters;
	static int[] successes;
	static boolean initted = false;
	static int virtualBulletTick = 0;
	static private final int numVirtualGuns = 5;
	
	double enemyX, enemyY;
	private ArrayList<VirtualBullet> virtualBullets;
	long lastEnemyScanTime;
	
	public void onPaint(Graphics2D graphics)
	{
		for (VirtualBullet bullet : virtualBullets)
		{
			switch (bullet.targetter)
			{
				case 0:
					graphics.setColor(Color.blue);
					break;
				case 1:
					graphics.setColor(Color.red);
					break;
				case 2:
					graphics.setColor(Color.green);
					break;
				case 3:
					graphics.setColor(Color.yellow);
					break;
				case 4:
					graphics.setColor(Color.magenta);
			}
			graphics.fill(new Rectangle2D.Double(bullet.x, bullet.y, 4.0, 4.0));
		}
	}
	
	public void init (WallE bot)
	{
		owner = bot;
		if (!initted)
		{
			targetters = new Targetting[numVirtualGuns];
			successes = new int[numVirtualGuns];
			successes[0] = 0;
			successes[1] = 0;
			successes[2] = 0;
			successes[3] = 0;
			successes[4] = 0;
			initted = true;
		}
		targetters[0] = new CircularTargetting();
		targetters[1] = new NaiveTargetting();
		targetters[2] = new LinearTargetting();
		targetters[3] = new RandomTargetting();
		targetters[4] = new ReverseCircularTargetting();
		targetters[0].init(bot);
		targetters[1].init(bot);
		targetters[2].init(bot);
		targetters[3].init(bot);
		targetters[4].init(bot);
		virtualBullets = new ArrayList<VirtualBullet>();
	}
	
	public void update()
	{
		// update all the virtual bullets
		double battleFieldX = owner.getBattleFieldWidth();
		double battleFieldY = owner.getBattleFieldHeight();
		for (int i = 0; i < virtualBullets.size(); i++)
		{
			VirtualBullet bullet = virtualBullets.get(i);
			long dt = lastEnemyScanTime - bullet.lastUpdateTime;
			if (dt <= 0)
				continue;
			bullet.x += bullet.velX * dt;
			bullet.y += bullet.velY * dt;
			bullet.lastUpdateTime = lastEnemyScanTime;
			if (bullet.x > battleFieldX ||
			    bullet.y > battleFieldY ||
			    bullet.x < 0.0 ||
			    bullet.y < 0.0)
			{
				// missed
				virtualBullets.remove(i);
				i--;
			}
			else if (Point2D.Double.distance(bullet.x, bullet.y, enemyX, enemyY) < 50.0)
			{
				// hit!
				successes[bullet.targetter]++;
				virtualBullets.remove(i);
				i--;
			}
		}
		owner.setDebugProperty("VGWeightings", "circ=" + successes[0] + " naive=" + successes[1] + " linear=" + successes[2] + " random=" + successes[3] + " rcirc=" + successes[4]);
		owner.setDebugProperty("VGVBCount", "" + virtualBullets.size());
		for (int i = 0; i < numVirtualGuns; i++)
		{
			targetters[i].update();
		}
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		// select the targetter with the best record
		int bestTargetter = 0; // default to the first
		int mostSuccesses = 0;
		for (int i = 0; i < numVirtualGuns; i++)
		{
			if (successes[i] > mostSuccesses)
			{
				mostSuccesses = successes[i];
				bestTargetter = i;
			}
		}
		return targetters[bestTargetter].target(e, bulletPower);
	}
	
	public void enemyPosition(ScannedRobotEvent e, double x, double y)
	{
		// poll subtargetters for their opinions
		virtualBulletTick++;
		lastEnemyScanTime = owner.getTime();
		if (virtualBulletTick % 4 == 0)
		{
			double bulletPower = e.getDistance() > 500.0 ? 2.0 : 3.0;
			for (int i = 0; i < numVirtualGuns; i++)
			{
				double angle = targetters[i].target(e, bulletPower);
				VirtualBullet bullet = new VirtualBullet();
				bullet.targetter = i;
				bullet.x = owner.getX();
				bullet.y = owner.getY();
				bullet.velX = Targetting.bulletSpeed(bulletPower) * Math.sin(angle);
				bullet.velY = Targetting.bulletSpeed(bulletPower) * Math.cos(angle);
				bullet.lastUpdateTime = lastEnemyScanTime;
				virtualBullets.add(bullet);
			}
		}
		for (int i = 0; i < numVirtualGuns; i++)
		{
			targetters[i].enemyPosition(e, x, y);
		}
		enemyX = x;
		enemyY = y;
	}
	
	private class VirtualBullet
	{
		public double x, y;
		public double velX, velY;
		public long lastUpdateTime;
		public int targetter;
	}
}
