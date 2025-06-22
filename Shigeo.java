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

    // == Comemoração de vitória == //

    public void onWin(WinEvent e) {
        for (int i = 0; i < 50; i++) {
            // Troca cor do corpo
            setBodyColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            setGunColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            setRadarColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            // "Dança" girando
            turnRight(25);
            turnLeft(25);
            execute();
        }
    }
}
