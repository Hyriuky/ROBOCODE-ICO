import robocode.*;
import java.awt.Color;
import java.awt.geom.Point2D;

public class Shigeo extends AdvancedRobot {

    private static final double MAX_FIRE_DISTANCE = 350.0;

    public void run() {
		setBodyColor(Color.black);
		setGunColor(Color.gray);
		setRadarColor(Color.gray);
		setScanColor(Color.black);
		setBulletColor(Color.gray);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            turnRadarRight(360);
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (e.getDistance() <= MAX_FIRE_DISTANCE) {
            double firePower = calculateAdaptiveFirePower(e.getDistance());
            double bulletSpeed = 20 - 3 * firePower;

            double enemyX = getX() + e.getDistance() * Math.sin(Math.toRadians(getHeading() + e.getBearing()));
            double enemyY = getY() + e.getDistance() * Math.cos(Math.toRadians(getHeading() + e.getBearing()));

            double enemyHeading = Math.toRadians(e.getHeading());
            double enemyVelocity = e.getVelocity();

            double deltaTime = 0;
            double predictedX = enemyX;
            double predictedY = enemyY;

            while ((++deltaTime) * bulletSpeed < Point2D.distance(getX(), getY(), predictedX, predictedY)) {
                predictedX += Math.sin(enemyHeading) * enemyVelocity;
                predictedY += Math.cos(enemyHeading) * enemyVelocity;

                if (predictedX < 18.0 || predictedY < 18.0 || 
                    predictedX > getBattleFieldWidth() - 18.0 || 
                    predictedY > getBattleFieldHeight() - 18.0) {
                    predictedX = Math.min(Math.max(18.0, predictedX), getBattleFieldWidth() - 18.0);
                    predictedY = Math.min(Math.max(18.0, predictedY), getBattleFieldHeight() - 18.0);
                    break;
                }
            }

            double theta = normalRelativeAngle(Math.atan2(predictedX - getX(), predictedY - getY()) - 
                             Math.toRadians(getGunHeading()));
            setTurnGunRight(Math.toDegrees(theta));

            if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
                setFire(firePower);
            }
        }
    }

    private double calculateAdaptiveFirePower(double distance) {
        distance = Math.min(distance, MAX_FIRE_DISTANCE);
        double power = 4.0 - (2.9 * (distance / MAX_FIRE_DISTANCE));
        return Math.max(0.1, Math.min(3.0, power));
    }

    private static double normalRelativeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
}
