import robocode.*;
import java.awt.Color;
import java.util.*;

public class Shigeo extends AdvancedRobot {

    public void run() {
        setBodyColor(Color.black);
        setGunColor(Color.gray);
        setRadarColor(Color.gray);
        setScanColor(Color.black);
        setBulletColor(Color.gray);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            execute();
        }
    }


    public void onScannedRobot(ScannedRobotEvent e) {
    }

    public void onHitByBullet(HitByBulletEvent e) {
    }

    public void onHitWall(HitWallEvent e) {
    }

    public void onHitRobot(HitRobotEvent e) {
    }

    public void onWin(WinEvent e) {
    }
}
