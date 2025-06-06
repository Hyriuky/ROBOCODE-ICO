package ai;
// Java API
import java.awt.Color;
import java.util.Random;
// Robocode Functions
import robocode.AdvancedRobot;
import robocode.util.Utils;
import robocode.Rules;
// Robocode Events
import robocode.ScannedRobotEvent;
import robocode.HitRobotEvent;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.DeathEvent;
import robocode.StatusEvent;
import robocode.BulletMissedEvent;
import robocode.BulletHitEvent;
import robocode.RoundEndedEvent;
import robocode.BulletHitBulletEvent;

/*  JukBot v9.0.7 (Updated 28 August 2016) 
	Maximum life: at 5871593 nanoseconds per turn.
	Designed: for Single Bot Mode and Multiple Bots Mode.
	Status: IN BETA TESTING, (NOT A FINAL BOT !!)
	Changes log: 
				- Add Bullet sheild protection feature
				- Reverse direction attack
				- Add bearing from gun for more precise fire and probability accurate.
	Future release: 
				- Analysis enemy heuristics
				- Improve Bullet sheild protection 
				- Calculate miss bullet probability
*/

/* JukBot Heuristics 
	Step 1 Find the best enemy target.
	Step 2 Scan and lock radar at 30 degree to an enemy.
	Step 3 Walk into the target with random to prevent bullet.
	Step 4 Turn gun into the enemy.
	Step 5 When the target distance is in bound Attack it!!.
	
	Walk Style: Learn enemy walk behavior (UNCOMPLETE)
	Radar Detection: Scan and lock the target, Track Using dynamic scale radar detection.
	Energy Saver: Smart bullet power management to prevent overheat and extend bot lifespan.
	Gun Control: Intelligence gun control to get the best absolute bearing and bullet direction.
	Random: Velocity, Walk turn
	Event: Automatic handle event and monitoring.
 */ 

	public class JukBotV9 extends AdvancedRobot{
		static int hit = 0, miss =0, low =0, cri =0, hot =0, bulhit =0, dead=0;    
		static final String robot = "JukBotV9";
		int moveDirection=1;

		public void run() {
	// INITIAL SETTINGS;
			setBodyColor(new Color(255, 255, 255));
			setGunColor(new Color(255, 255, 255));
			setRadarColor(new Color(255, 255, 255));
			setScanColor(new Color(255, 255, 255));
			setBulletColor(new Color(244,67,54));
			setAdjustRadarForRobotTurn(true);
			setAdjustGunForRobotTurn(true); 
			setAdjustRadarForGunTurn(true); 
			int R = (int)(Math.random()*256);
			int G = (int)(Math.random()*256);
			int B= (int)(Math.random()*256);
	Color color = new Color(R, G, B); //random color, but can be bright or dull
	//to get rainbow, pastel colors
	Random random = new Random();
	do {
		final float hue = random.nextFloat();
		final float sat = 0.5f;
		final float lum = 1.0f;
		color = Color.getHSBColor(hue, sat, lum);
		setRadarColor(color);
	// Reset gun heat protection
		if(getGunHeat() < 3.0) {
			hot =0;
		}
		if (getRadarTurnRemaining() == 0.0) {
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		}			
		execute();
	} while(true);
}
public void onScannedRobot(ScannedRobotEvent e) {  
	double gunTurnRad;// radius amount to turn gun
	double absBearingRad = getHeadingRadians() + e.getBearingRadians(); //Absolute angle towards target to get turn 
	double absBearingDeg = getHeading() + e.getBearing(); // Absolute angle towards target to get turn in degree
	//double maxArea = Math.max(getBattleFieldHeight(),getBattleFieldWidth()); // Maximum area of battle field
	double eneVel=e.getVelocity() * Math.sin(e.getHeadingRadians() - absBearingRad); //enemy post velocity
	double bearingFromGunDeg = Utils.normalRelativeAngleDegrees(absBearingDeg - getGunHeading()); // precise fire calculate
	double radarTurn = Utils.normalRelativeAngle(absBearingRad - getRadarHeadingRadians() );    // Subtract current radar heading to get the turn to face the enemy.
	// Distance to scan from middle of enemy to either side. 36.0 is units from the center of the enemy robot it scans.
	double extraTurn = Math.min(Math.atan(36.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);  
   // Adjust the radar turn in the direction it is going to turn
   if(radarTurn < 0) {  // Allows to overshoot the enemy with good sweep that will not slip.
	radarTurn -= extraTurn; 
   }
   else {
	radarTurn += extraTurn; 
   }
   
	// SINGLE BOT RADAR DETECTION MODE AND LOCK TO THE TARGET
   setTurnRadarRightRadians(radarTurn); 

	// BOT WALK BEHAVIOR AFTER DETECT ENEMY
	setMaxVelocity((32*Math.random())+32);//randomly change speed
	// BOT GUN BEHAVIOR
	if (e.getDistance() >= 400) { //if enemy distance is greater than 400 VERY FAR
	setTurnRightRadians(Utils.normalRelativeAngle(absBearingRad-getHeadingRadians()+eneVel/getVelocity()));//predicted future location
	gunTurnRad = Utils.normalRelativeAngle(absBearingRad - getGunHeadingRadians()+eneVel/20);//amount to turn our gun, lead just a little bit
	System.out.println("Enemy distance: " + e.getDistance() + " | Enemy post velocity: " + eneVel + " | GunTurnRad: " + gunTurnRad + " | GunHeadingRad: " + getGunHeadingRadians() + " | AbsBearingRad " + absBearingRad + " | GunHeat: " + getGunHeat() + " | Velocity: " + getVelocity() + " | BearingFromGunDeg: " + Math.abs(bearingFromGunDeg));
	setTurnGunRightRadians(gunTurnRad); //turn gun
	setAhead((e.getDistance() - 300)*moveDirection);//move forward
	setBulletColor(new Color(255,193,7));
	if(getGunHeat() ==0) {
			setFire(0.25); //not dynamic fire
		}
	}	
	else if (e.getDistance() >= 250) { //if enemy distance is greater than 250 LITTLE FAR FROM ENEMY
	setTurnRightRadians(Utils.normalRelativeAngle(absBearingRad-getHeadingRadians()+eneVel/getVelocity()));//predicted future location
	gunTurnRad = Utils.normalRelativeAngle(absBearingRad - getGunHeadingRadians()+eneVel/15);//amount to turn our gun, lead just a little bit
	System.out.println("Enemy distance: " + e.getDistance() + " | Enemy post velocity: " + eneVel + " | GunTurnRad: " + gunTurnRad + " | GunHeadingRad: " + getGunHeadingRadians() + " | AbsBearingRad " + absBearingRad + " | GunHeat: " + getGunHeat() + " | Velocity: " + getVelocity() + " | BearingFromGunDeg: " + Math.abs(bearingFromGunDeg));
	setTurnGunRightRadians(gunTurnRad); //turn gun
	setAhead((e.getDistance() - 200)*moveDirection);//move forward
	setBulletColor(new Color(255,152,0));
	if(getGunHeat() ==0) {
			setFire(2); //not dynamic fire
		}
	}	
	else if (e.getDistance() >= 180 || Math.abs(bearingFromGunDeg) <= 5) { //if enemy distance is greater than 180 CLOSER TO ENEMY
	setTurnRightRadians(Utils.normalRelativeAngle(absBearingRad-getHeadingRadians()+eneVel/getVelocity()));//predicted future location
	gunTurnRad = Utils.normalRelativeAngle(absBearingRad - getGunHeadingRadians()+eneVel/15);//amount to turn our gun, lead just a little bit
	System.out.println("Enemy distance: " + e.getDistance() + " | Enemy post velocity: " + eneVel + " | GunTurnRad: " + gunTurnRad + " | GunHeadingRad: " + getGunHeadingRadians() + " | AbsBearingRad " + absBearingRad + " | GunHeat: " + getGunHeat() + " | Velocity: " + getVelocity() + " | BearingFromGunDeg: " + Math.abs(bearingFromGunDeg));
	setTurnGunRightRadians(gunTurnRad);//turn gun  
	setAhead((e.getDistance() - 130)*moveDirection);//move forward
	setBulletColor(new Color(255,87,24));
	if(getGunHeat() ==0) {
			setFire(2.75); //not dynamic fire
		}
	}
	else if (e.getDistance() < 180 || Math.abs(bearingFromGunDeg) <= 3) { // if enemy distance is less than 180 THE CLOSEST TO ENEMY
	setTurnRightRadians(Utils.normalRelativeAngle(absBearingRad-getHeadingRadians()+eneVel/getVelocity()));//predicted future location
	gunTurnRad = Utils.normalRelativeAngle(absBearingRad - getGunHeadingRadians()+eneVel/10);//amount to turn our gun, lead just a little bit
	System.out.println("Enemy distance: " + e.getDistance() + " | Enemy post velocity: " + eneVel + " | GunTurnRad: " + gunTurnRad + " | GunHeadingRad: " + getGunHeadingRadians() + " | AbsBearingRad " + absBearingRad + " | GunHeat: " + getGunHeat() + " | Velocity: " + getVelocity() + " | BearingFromGunDeg: " + Math.abs(bearingFromGunDeg));
	setTurnGunRightRadians(gunTurnRad);//turn gun
	setTurnRight(-90-e.getBearing()); //turn perpendicular to enemy
	setAhead((e.getDistance() - 100)*moveDirection);//move forward
	setBulletColor(new Color(244,67,54));
	if(getGunHeat() ==0) {
			setFire(3); //not dynamic fire
		}
	}	
	
	// MULTIPLE BOTS RADAR LOCK DETECTION MODE (FOR TEAM)
/*   double radarTurnMultiLock = absBearingRad;
	  setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurnMultiLock));
	turnRadarRight(Utils.normalRelativeAngleDegrees(getHeading()+e.getBearing()-getRadarHeading()));
	turnGunRight(Utils.normalRelativeAngleDegrees(getHeading()+e.getBearing()-getGunHeading())); 	
	 // !! UNCOMPLETE !!
*/	
}

	// EVENT HANDLE ZONE
public void onHitRobot(HitRobotEvent e) {
	System.out.println("!!COLLISSION!! " + robot + " is collide " + e.getName());
	setBulletColor(new Color(244,67,54));
	if(getGunHeat() ==0) {
			setFire(3); //fire
		}
	 // IN DEVELOPMENT AI
	}

public void onHitByBullet(HitByBulletEvent e) {
		hit++;
		System.out.println("!!WARNING!! " + robot + " is hit by " + e.getName() + " (Hit Count: " + hit + " Energy: " + getEnergy() + ")");
	    setAhead(100*moveDirection);//move forward
	 // IN DEVELOPMENT AI
}

public void onHitWall(HitWallEvent e) {
	moveDirection=-moveDirection; //reverse direction upon hitting a wall
	 // IN DEVELOPMENT AI
}

public void onBulletHit(BulletHitEvent event) {
	bulhit++;
	System.out.println("!!WELL DONE!! " + robot + " bullet's hit " + event.getName() + "!");
	// IN DEVELOPMENT AI
}

public void onBulletHitBullet(BulletHitBulletEvent e) {
	System.out.println("!!WELL DONE!! " +robot + " bullet's hit a bullet fired by " + e.getBullet().getName() + "!!");
	moveDirection=-moveDirection;
	setAhead(100*moveDirection);//move forward 
	if(getGunHeat() ==0) {
		setFire(3); //not dynamic fire
	}
   // IN DEVELOPMENT AI
} 

public void onBulletMissed(BulletMissedEvent e) {
	miss++;
	System.out.println("!!WARNING!! " + robot + " bullet's missed." + " (Missed Count: " + miss + " Energy: " + getEnergy() + ")");
	 // IN DEVELOPMENT AI
}

public void onDeath(DeathEvent e) {
	dead++;
	System.out.println(robot + " is Dead (X_X)");
}

public void onRoundEnded(RoundEndedEvent event) {
	System.out.println("====== SUMMARY ====== " + "Be Hit:" + hit + " | Bullet Missed:" + miss + " | Bullet Hit:" + bulhit + " | " + "Total dead:" + dead + " out of " + getNumRounds() + " | Survivor probability:" + ((getRoundNum()+1)-dead)*100/(getRoundNum()+1) + "%");
	hit=0; miss=0; bulhit=0; hot =0; cri =0; low =0; // Reset status monitoring
}

	// MONITORING ZONE
public void onStatus(StatusEvent e) {
	if (getEnergy() < 30 && low < 3) {
		System.out.println("!!DANGER!! " + robot + " is low energy");
		low++;
	} 
	if(getEnergy() < 10 && cri < 1) {
		System.out.println("!!DANGER!! " + robot + " is critical low energy");
		cri++;
	} 
	if(getGunHeat() == 3 && hot < 3) {
		System.out.println("!!DANGER!! " + robot + " is overheat");
		hot++;
	}
}
}