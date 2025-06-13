import robocode.*;
import java.awt.Color;
import java.util.*;
import java.awt.geom.Point2D;

public class RoboTesteV1 extends AdvancedRobot {

    // == Sistema de movimentação do OmniBotV20 == //
    private int direction = 1;
    private double previousEnergy = 100;
    
    // == Sistema de tiro inteligente que você já tinha == //
    private static final double MAX_FIRE_DISTANCE = 250.0;
    
    // == Radar e mira == //
    private Map<String, EnemyData> enemies = new HashMap<>();
    private String targetName = null;

    public void run() {
        setColors(Color.RED, Color.RED, Color.BLACK); // Cores do robô
        setAdjustGunForRobotTurn(true); // Mira livre
        setAdjustRadarForRobotTurn(true); // Radar livre

        while (true) {
            scanForClosestEnemy(); // Procura por inimigo perto
            
            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                if (target == null || getTime() - target.lastSeen > 5) { // Vê se o inimigo morreu há mais de 5 unidades de tempo
                    targetName = null; // Diz que não tem mais alvo
                }
            }

            if (targetName != null) {
                EnemyData target = enemies.get(targetName); // Pega dados do alvo
                double radarTurn = getHeading() + target.lastBearing - getRadarHeading(); // Ajuste do giro do radar
                setTurnRadarRight(normalRelativeAngleDegrees(radarTurn) * 2); // Faz o radar girar
            } else {
                setTurnRadarRight(360); // Procurar inimigos
            }

            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	
        // == Atualização do sistema de rastreamento == //
        String enemyName = e.getName(); // Pega os dados do inimigo
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData()); // Pega os dados do inimigo
        data.update(e); // Pega os dados do inimigo
        enemies.put(enemyName, data); // Pega os dados do inimigo
        scanForClosestEnemy(); // Escaneia por inimigos

        // == NOVA MOVIMENTAÇÃO (padrão OmniBotV20) == //
        double changeInEnergy = previousEnergy - e.getEnergy(); // Vê se houve mudança de energia
        if (changeInEnergy > 0 && changeInEnergy <= 3.0) { // Se mudou energia
            direction *= -1; // Muda a direção
            setAhead(150 * direction); // Vai para frente
        }
        previousEnergy = e.getEnergy();

        double angle = normalRelativeAngleDegrees(e.getBearing() + 90 - (15 * direction));
        setTurnRight(angle);
        setAhead(100 * direction);

        // == Seu sistema de tiro inteligente adaptado == //
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

    // == Métodos de reação a colisões (novos) == //
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
        double angle = normalRelativeAngleDegrees(e.getBearing());
        setTurnGunRight(angle);
        setFire(3);
        direction *= -1;
        setBack(100);
    }

    // == Métodos auxiliares == //
    private double calculateAdaptiveFirePower(double distance) {
        distance = Math.min(distance, MAX_FIRE_DISTANCE);
        double power = 3.5 - (2.9 * (distance / MAX_FIRE_DISTANCE));
        return Math.max(0.1, Math.min(3.0, power));
    }

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

    // == Utilitários == //
    private static double normalRelativeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private static double normalRelativeAngleDegrees(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
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

    // == Comemoração de vitória == //
    public void onWin(WinEvent e) {
        for (int i = 0; i < 30; i++) {
            setBodyColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
			setGunColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
			setRadarColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            turnRight(20);
            turnLeft(20);
            execute();
        }
    }
}