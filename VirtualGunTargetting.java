package cda;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.awt.*;
import java.util.List;

public class VirtualGunTargetting extends Targetting
{
	static Targetting[] targetters;
	static boolean initted = false;
	static int virtualBulletTick = 0;
	static private final int numVirtualGuns = 6;
	static private int[] successes;
	
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
					break;
				case 5:
					graphics.setColor(Color.cyan);
					break;
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
			initted = true;
			successes = new int[numVirtualGuns];
		}
		targetters[0] = new CircularTargetting();
		targetters[1] = new NaiveTargetting();
		targetters[2] = new LinearTargetting();
		targetters[3] = new ReverseCircularTargetting();
		targetters[4] = new RandomTargetting(Math.PI * 0.12);
		targetters[5] = new RandomTargetting(Math.PI * 0.2);
		targetters[0].init(bot);
		targetters[1].init(bot);
		targetters[2].init(bot);
		targetters[3].init(bot);
		targetters[4].init(bot);
		targetters[5].init(bot);
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
			else if (Point2D.Double.distance(bullet.x, bullet.y, enemyX, enemyY) < 30.0)
			{
				// log in array
				successes[bullet.targetter]++;
				// hit!
				virtualBullets.remove(i);
				i--;
			}
		}
		owner.setDebugProperty("VGVBCount", "" + virtualBullets.size());
		owner.setDebugProperty("VGHits", "circ=" + successes[0] +
		                                 " naive=" + successes[1] +
		                                 " linear=" + successes[2] +
		                                 " rc=" + successes[3] +
		                                 " rN=" + successes[4] +
		                                 " rW=" + successes[5]);
		for (int i = 0; i < numVirtualGuns; i++)
		{
			targetters[i].update();
		}
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		// select the targetter with the best record
		int bestTargetter = 0; // default to the first
		int mostHits = 0;
		for (int i = 0; i < numVirtualGuns; i++)
		{
			if (successes[i] > mostHits)
			{
				bestTargetter = i;
				mostHits = successes[i];
			}
		}
		return targetters[bestTargetter].target(e, bulletPower);
	}
	
	public void enemyPosition(ScannedRobotEvent e, double x, double y)
	{
		// poll subtargetters for their opinions
		virtualBulletTick++;
		lastEnemyScanTime = owner.getTime();
		// only log it when we actually fire: the enemy may respond to that
		if (owner.shouldFireShot(e))
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
