import robocode.*;
import java.awt.Color;
import java.util.*;
import java.awt.geom.Point2D;

public class Shigeo extends AdvancedRobot {

    private int direction = 1;
	private double previousEnergy = 100;
    private Map<String, EnemyData> enemies = new HashMap<>();
    private String targetName = null;

    private static final double MAX_FIRE_DISTANCE = 350.0;

    public void run() {
		//== Cores ==//
        setBodyColor(Color.black);
        setGunColor(Color.gray);
        setRadarColor(Color.gray);
        setScanColor(Color.black);
        setBulletColor(Color.gray);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);

        //== Loop principal ==// 
        while (true) {
            scanForClosestEnemy();

            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                if (target == null || getTime() - target.lastSeen > 5) {
                    targetName = null;
                }
            }

            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                double radarTurn = getHeading() + target.lastBearing - getRadarHeading();
                setTurnRadarRight(normalRelativeAngleDegrees(radarTurn) * 2);
            } else {
                setTurnRadarRight(360);
            }

            execute();
        }
    }

 //== Mira e tiro adaptativo + movimentação por energia ==//
    public void onScannedRobot(ScannedRobotEvent e) {
        //== Atualiza dados do inimigo ==//
        String enemyName = e.getName();
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData());
        data.update(e);
        enemies.put(enemyName, data);
        scanForClosestEnemy();

        //== Detecta tiro inimigo e ajusta movimentação ==//
        double changeInEnergy = previousEnergy - e.getEnergy();
        if (changeInEnergy > 0 && changeInEnergy <= 3.0) {
            direction *= -1;
            setAhead(150 * direction);
        }
        previousEnergy = e.getEnergy();

        //== Movimentação padrão: andar perpendicular ao inimigo ==//
        double angle = normalRelativeAngleDegrees(e.getBearing() + 90 - (15 * direction));
        setTurnRight(angle);
        setAhead(100 * direction);

        //== Mira e disparo adaptativo ==//
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
	
    //== Reação ao ser atingido por tiro ==//
    public void onHitByBullet(HitByBulletEvent e) {
        direction *= -1;
        setTurnRight(90 - e.getBearing());
        setAhead(100 * direction);
    }

    //== Reação ao bater na parede ==//
    public void onHitWall(HitWallEvent e) {
        direction *= -1;
        setBack(50);
        setTurnRight(90);
    }

    //== Reação ao colidir com outro robô ==//
    public void onHitRobot(HitRobotEvent e) {
        targetName = e.getName(); 
        setTurnRadarRight(360);
	double angle = normalRelativeAngleDegrees(e.getBearing());
        setTurnGunRight(angle);
        setFire(3);
        direction *= -1;
        setBack(100);
    }

    private static double normalRelativeAngleDegrees(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
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

    //== Auxiliares ==//

    private void scanForClosestEnemy() {
        double closestDistance = Double.MAX_VALUE;
        String closestEnemy = null;

        Iterator<Map.Entry<String, EnemyData>> it = enemies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EnemyData> entry = it.next();
            EnemyData e = entry.getValue();

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

    // == Classe de dados do inimigo == //
	
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
}
