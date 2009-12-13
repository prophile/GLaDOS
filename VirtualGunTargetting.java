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
	static private final int numVirtualGuns = 4;
	
	static private KdTree<Integer> targetSelector;
	
	double enemyX, enemyY;
	private ArrayList<VirtualBullet> virtualBullets;
	long lastEnemyScanTime;
	
	static private final int logEntryCount = 4;
	
	private double lateralVelocities[];
	private double advancingVelocities[];
	
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
			targetSelector = new KdTree.Manhattan<Integer>(10, new Integer(800));
		}
		targetters[0] = new CircularTargetting();
		targetters[1] = new NaiveTargetting();
		targetters[2] = new LinearTargetting();
		targetters[3] = new ReverseCircularTargetting();
		targetters[0].init(bot);
		targetters[1].init(bot);
		targetters[2].init(bot);
		targetters[3].init(bot);
		lateralVelocities = new double[logEntryCount];
		advancingVelocities = new double[logEntryCount];
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
				// log in kdtree
				SituationLog log = bullet.log;
				double[] location = new double[10];
				for (int j = 0; j < logEntryCount; j++)
				{
					location[j] = log.lateralVelocities[j];
					location[j+logEntryCount] = log.advancingVelocities[j];
				}
				location[8] = log.distance;
				location[9] = log.energy;
				targetSelector.addPoint(location, new Integer(bullet.targetter));
				// hit!
				virtualBullets.remove(i);
				i--;
			}
		}
		owner.setDebugProperty("VGVBCount", "" + virtualBullets.size());
		owner.setDebugProperty("VGLogSize", "" + targetSelector.size());
		for (int i = 0; i < numVirtualGuns; i++)
		{
			targetters[i].update();
		}
	}
	
	public double target(ScannedRobotEvent e, double bulletPower)
	{
		// select the targetter with the best record
		int bestTargetter = 0; // default to the first
		double position[] = new double[10];
		for (int i = 0; i < logEntryCount; i++)
		{
			position[i] = lateralVelocities[(i + virtualBulletTick) % 4];
			position[i + 4] = advancingVelocities[(i + virtualBulletTick) % 4];
		}
		position[8] = e.getDistance();
		position[9] = e.getEnergy();
		List<KdTree.Entry<Integer>> entries = targetSelector.nearestNeighbor(position, 1, false);
		if (!entries.isEmpty() && entries.get(0).distance < 50.0)
		{
			owner.setDebugProperty("VGSelection", "" + entries.get(0).value + ", dist=" + entries.get(0).distance);
			bestTargetter = entries.get(0).value.intValue();
		}
		else
		{
			owner.setDebugProperty("VGSelection", "" + bestTargetter + " (no closer point)");
		}
		return targetters[bestTargetter].target(e, bulletPower);
	}
	
	public void enemyPosition(ScannedRobotEvent e, double x, double y)
	{
		// poll subtargetters for their opinions
		virtualBulletTick++;
		lastEnemyScanTime = owner.getTime();
		double absoluteBearing = e.getBearingRadians() + owner.getHeadingRadians();
		double lateralSpeed = e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing);
		double advancingSpeed = e.getVelocity() * Math.cos(e.getHeadingRadians() - absoluteBearing);
		lateralVelocities[virtualBulletTick % 4] = lateralSpeed;
		advancingVelocities[virtualBulletTick % 4] = advancingSpeed;
		if (virtualBulletTick % 4 == 3)
		{
			SituationLog log = new SituationLog();
			log.lateralVelocities = new double[logEntryCount];
			log.advancingVelocities = new double[logEntryCount];
			for (int i = 0; i < logEntryCount; i++)
			{
				log.lateralVelocities[i] = lateralVelocities[i];
				log.advancingVelocities[i] = advancingVelocities[i];
			}
			log.distance = e.getDistance();
			log.energy = e.getEnergy();
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
				bullet.log = log;
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
		public SituationLog log;
	}
	
	private class SituationLog
	{
		public double distance, energy;
		public double lateralVelocities[];
		public double advancingVelocities[];
	}
}
