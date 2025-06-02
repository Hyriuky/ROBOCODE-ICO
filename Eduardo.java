package eduardo;
import robocode.*;

public class Eduardo extends Robot {
	
	public void run () {
	
		turnRadarRight(360);
		turnRight(getHeading());
		
		while (true) {
			
			turnGunRight(1000);
			ahead(100);
			turnLeft(90);


		}
		
	}
	public void onScannedRobot(ScannedRobotEvent e) {
		
		double distancia = e.getDistance();
		double potenciaTiro;
		
		if (distancia <100) {
			potenciaTiro = 3;
		}
		else if (distancia < 300) {	
			potenciaTiro = 2;
		}
		else {
			potenciaTiro = 1;
		}
		fire (potenciaTiro);
	}

	public void onHitByBullet(HitByBulletEvent e) {
			
		turnLeft(90);
		back(30);
	}
	public void onHitWall(HitWallEvent e) {
	
		back(20);
	
	}

}
