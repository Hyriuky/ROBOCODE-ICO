package omni;

import robocode.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;

/**
 * OmniBot - inspirado nos melhores elementos de 8 robôs diferentes
 */
public class OmniBot extends AdvancedRobot {

    private Map<String, EnemyData> enemies = new HashMap<>();
    private static final double BULLET_DETECTION_THRESHOLD = 0.1;

    private double previousEnergy = 100;
    private double direction = 1;
    private String targetName = null;

    public void run() {
        setColors(Color.BLUE, Color.BLACK, Color.CYAN);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            if (targetName != null && enemies.containsKey(targetName)) {
                EnemyData target = enemies.get(targetName);
                double radarTurn = getHeading() + target.lastBearing - getRadarHeading();
                setTurnRadarRight(Utils.normalRelativeAngleDegrees(radarTurn) * 2);
            } else {
                setTurnRadarRight(360); // Busca padrão
            }
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        String enemyName = e.getName();
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData());

        // Atualiza informações do inimigo
        data.update(e);
        enemies.put(enemyName, data);

        targetName = enemyName;

        double changeInEnergy = previousEnergy - e.getEnergy();
        if (changeInEnergy > 0 && changeInEnergy <= 3.0) {
            // Possível tiro detectado, desvie
            direction *= -1;
            setAhead(150 * direction);
        }
        previousEnergy = e.getEnergy();

        // Movimentação estilo BarbieScript (lateral com inversão ao detectar tiro)
        double angle = Utils.normalRelativeAngleDegrees(e.getBearing() + 90 - (15 * direction));
        setTurnRight(angle);
        setAhead(100 * direction);

        // Mira por predição linear (estilo UR4N0)
        double bulletPower = Math.min(3.0, Math.max(1.0, e.getDistance() > 400 ? 1.0 : 3.0));
        double bulletSpeed = 20 - 3 * bulletPower;

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
            if (predictedX < 18.0 || predictedY < 18.0 || predictedX > getBattleFieldWidth() - 18.0 || predictedY > getBattleFieldHeight() - 18.0) {
                predictedX = Math.min(Math.max(18.0, predictedX), getBattleFieldWidth() - 18.0);
                predictedY = Math.min(Math.max(18.0, predictedY), getBattleFieldHeight() - 18.0);
                break;
            }
        }

        double theta = Utils.normalRelativeAngle(Math.atan2(predictedX - getX(), predictedY - getY()) - Math.toRadians(getGunHeading()));
        setTurnGunRight(Math.toDegrees(theta));

        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
            setFire(bulletPower);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        direction *= -1;
        setTurnRight(90 - e.getBearing());
        setAhead(100 * direction);
    }

    public void onHitWall(HitWallEvent e) {
        direction *= -1;
        setBack(50);
        setTurnRight(90);
    }

    static class EnemyData {
        double energy;
        double lastBearing;
        long lastSeen;

        void update(ScannedRobotEvent e) {
            this.energy = e.getEnergy();
            this.lastBearing = e.getBearing();
            this.lastSeen = e.getTime();
        }
    }

    static class Utils {
        static double normalRelativeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        static double normalRelativeAngleDegrees(double angle) {
            while (angle > 180) angle -= 360;
            while (angle < -180) angle += 360;
            return angle;
        }
    }
}