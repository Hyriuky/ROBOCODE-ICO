package omni;

import robocode.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;

public class OmniBotV20 extends AdvancedRobot {

    private Map<String, EnemyData> enemies = new HashMap<>();
    private static final double BULLET_DETECTION_THRESHOLD = 0.1;

    private double previousEnergy = 100;
    private double direction = 1;
    private String targetName = null;
    private long lastScanTime = 0;

    public void run() {
        setColors(Color.RED, Color.BLACK, Color.RED);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            // Atualiza o alvo mais próximo
            scanForClosestEnemy();

            // Verifica se o alvo atual ainda é válido (existe e foi visto recentemente)
            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                if (target == null || getTime() - target.lastSeen > 5) {
                    // Inimigo sumiu ou foi eliminado, limpa o alvo
                    targetName = null;
                }
            }

            if (targetName != null) {
                // Alvo válido: trava o radar no inimigo
                EnemyData target = enemies.get(targetName);
                double radarTurn = getHeading() + target.lastBearing - getRadarHeading();
                setTurnRadarRight(Utils.normalRelativeAngleDegrees(radarTurn) * 2);
                lastScanTime = getTime();
            } else {
                // Sem alvo: gira radar 360 graus para procurar inimigos
                setTurnRadarRight(360);
            }

            execute();
        }
    }

    private void scanForClosestEnemy() {
        double closestDistance = Double.MAX_VALUE;
        String closestEnemy = null;

        Iterator<Map.Entry<String, EnemyData>> it = enemies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EnemyData> entry = it.next();
            EnemyData e = entry.getValue();

            // Remove inimigos não vistos há mais de 10 ticks para evitar travar em inimigos eliminados
            if (getTime() - e.lastSeen > 10) {
                it.remove();
                continue;
            }

            if (e.distance < closestDistance) {
                closestDistance = e.distance;
                closestEnemy = entry.getKey();
            }
        }

        targetName = closestEnemy;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        String enemyName = e.getName();
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData());

        data.update(e);
        enemies.put(enemyName, data);

        lastScanTime = getTime();

        scanForClosestEnemy();

        double changeInEnergy = previousEnergy - e.getEnergy();
        if (changeInEnergy > 0 && changeInEnergy <= 3.0) {
            direction *= -1;
            setAhead(150 * direction);
        }
        previousEnergy = e.getEnergy();

        double angle = Utils.normalRelativeAngleDegrees(e.getBearing() + 90 - (15 * direction));
        setTurnRight(angle);
        setAhead(100 * direction);

        double bulletPower = Math.min(3.0, Math.max(0.1, 400 / e.getDistance()));
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

    public void onHitRobot(HitRobotEvent e) {
        targetName = e.getName();
        double angle = Utils.normalRelativeAngleDegrees(e.getBearing());
        setTurnGunRight(angle);
        setFire(3);
        direction *= -1;
        setBack(100);
    }

    static class EnemyData {
        double energy;
        double lastBearing;
        double distance;
        long lastSeen;

        void update(ScannedRobotEvent e) {
            this.energy = e.getEnergy();
            this.lastBearing = e.getBearing();
            this.distance = e.getDistance();
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

    public void onWin(WinEvent e) { // Comemoração de vitória 
    for (int i = 0; i < 30; i++) {
        setBodyColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
        turnRight(20);
        turnLeft(20);
        execute();  // Adiciona para garantir que os comandos sejam executados
        }
    }
}
