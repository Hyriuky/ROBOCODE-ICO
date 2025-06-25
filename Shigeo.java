import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.*;

public class Shigeo extends AdvancedRobot {

    private int direction = 1;
    private double previousEnergy = 100;
    private Map<String, EnemyData> enemies = new HashMap<>();
    private String targetName = null;

    private static final double MAX_FIRE_DISTANCE = 350.0;
    // Distância mínima OBRIGATÓRIA que o robô tentará manter do inimigo.
    private static final double MINIMUM_DISTANCE = 100.0;
    // Distância confortável para iniciar a movimentação lateral.
    private static final double SAFE_DISTANCE = 150.0;

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
            if (targetName == null) {
                scanForClosestEnemy();
            }

            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                // Se não vemos o alvo há algum tempo, procuramos um novo
                if (target == null || getTime() - target.lastSeen > 5) {
                    targetName = null;
                    setTurnRadarRight(360); // Gira o radar para encontrar alvos
                } else {
                    // Mantém o radar travado no alvo
                    double radarTurn = getHeadingRadians() + target.lastBearingRadians - getRadarHeadingRadians();
                    setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn) * 2);
                }
            } else {
                setTurnRadarRight(360); // Gira se não tiver alvo
            }

            execute();
        }
    }

    //== LÓGICA PRINCIPAL DE COMBATE E MOVIMENTO ==//
    public void onScannedRobot(ScannedRobotEvent e) {
        //== Atualiza dados do inimigo ==//
        String enemyName = e.getName();
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData());
        data.update(e);
        enemies.put(enemyName, data);
        scanForClosestEnemy();

        // Se o robô escaneado não é nosso alvo principal, ignora o resto da lógica para ele.
        if (targetName == null || !targetName.equals(e.getName())) {
            return;
        }

        //== LÓGICA DE MOVIMENTO UNIFICADA ==//
        // Detecta tiro inimigo para mudar de direção. Apenas inverte a variável.
        double changeInEnergy = previousEnergy - e.getEnergy();
        if (changeInEnergy > 0 && changeInEnergy <= 3.0) {
            direction *= -1;
        }
        previousEnergy = e.getEnergy();

        // 1. LÓGICA DE DISTÂNCIA PRIORITÁRIA
        if (e.getDistance() < MINIMUM_DISTANCE) {
            // Se estivermos MUITO perto, a prioridade máxima é recuar.
            // Viramos perpendicularmente e damos ré para nos afastarmos enquanto nos esquivamos.
            double angleToTurn = normalRelativeAngleDegrees(e.getBearing() + 90);
            setTurnRight(angleToTurn);
            setBack(150); // Move para trás com velocidade máxima para se afastar

        } else {
            // 2. LÓGICA DE MOVIMENTO PADRÃO (se a distância for segura)
            // Anda de lado (movimento perpendicular) para desviar de tiros.
            double angle = normalRelativeAngleDegrees(e.getBearing() + 90 - (15 * direction));
            setTurnRight(angle);
            setAhead(100 * direction); // Move-se para frente ou para trás na direção perpendicular
        }

        //== Mira e disparo adaptativo ==//
        if (e.getDistance() <= MAX_FIRE_DISTANCE) {
            double firePower = calculateAdaptiveFirePower(e.getDistance());

            // MIRA COM COMPENSAÇÃO LINEAR
            double absBearingRad = getHeadingRadians() + e.getBearingRadians();
            double compensacaoLinear = e.getVelocity() * Math.sin(e.getHeadingRadians() - absBearingRad) / Rules.getBulletSpeed(firePower);
            
            // Ajuste fino da compensação
            if (e.getDistance() <= 120.0) compensacaoLinear *= 0.7;

            double gunTurnRad = Utils.normalRelativeAngle(absBearingRad - getGunHeadingRadians() + compensacaoLinear);
            setTurnGunRightRadians(gunTurnRad);

            // ATIRAR!
            if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
                setFire(firePower);
            }
        }
    }
    
    private double calculateAdaptiveFirePower(double distance) {
        distance = Math.min(distance, MAX_FIRE_DISTANCE);
        double power = 4.0 - (2.1 * (distance / MAX_FIRE_DISTANCE));
        return Math.max(0.1, Math.min(3.0, power));
    }
    
    public void onHitByBullet(HitByBulletEvent e) {
        // Ao ser atingido, inverte a direção do movimento para sair da mira do inimigo.
        direction *= -1;
    }

    public void onHitWall(HitWallEvent e) {
        // Se bater na parede, inverte a direção e recua.
        direction *= -1;
        setBack(100);
    }
    
    public void onHitRobot(HitRobotEvent e) {
        // Se colidir com outro robô, considera-o o novo alvo prioritário
        // e recua para criar distância imediatamente.
        targetName = e.getName();
        direction *= -1;
        setBack(100);
    }

    private void scanForClosestEnemy() {
        double closestDistance = Double.MAX_VALUE;
        String closestEnemy = null;
        for (EnemyData e : enemies.values()) {
            if (getTime() - e.lastSeen > 10) {
                continue; // Ignora inimigos "perdidos"
            }
            if (e.distance < closestDistance) {
                closestDistance = e.distance;
                closestEnemy = e.name;
            }
        }
        if (closestEnemy != null) {
            targetName = closestEnemy;
        }
    }
    
    // Normaliza um ângulo em graus para o intervalo -180 a 180
    private double normalRelativeAngleDegrees(double angle) {
        return Utils.normalRelativeAngleDegrees(angle);
    }

    public void onWin(WinEvent e) {
        setTurnRadarRight(36000);
        setTurnGunRight(36000);
        setTurnRight(36000);
    }

    static class EnemyData {
        String name;
        double energy;
        double lastBearingRadians; // Armazenar em radianos é mais eficiente com Utils
        double distance;
        double headingRadians;
        double velocity;
        long lastSeen;

        void update(ScannedRobotEvent e) {
            this.name = e.getName();
            this.energy = e.getEnergy();
            this.lastBearingRadians = e.getBearingRadians();
            this.distance = e.getDistance();
            this.headingRadians = e.getHeadingRadians();
            this.velocity = e.getVelocity();
            this.lastSeen = e.getTime();
        }
    }
}
