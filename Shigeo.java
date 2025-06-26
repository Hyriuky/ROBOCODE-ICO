import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class Shigeo extends AdvancedRobot {

    //== Variáveis de estado e Constantes ==//
    private int direction = 1; // Para alternar a direção do movimento
    private Map<String, EnemyData> enemies = new HashMap<>(); // Armazena dados dos inimigos
    private String targetName = null; // O alvo atual do robô

    // Constantes de Estratégia e Comportamento
    private static final double FATOR_DE_COMPENSACAO = 0.55; // Fator de ajuste para a mira
    private static final double MAX_FIRE_DISTANCE = 350.0;   // Distância máxima de tiro
    private static final double MINIMUM_DISTANCE = 100.0;    // Distância mínima a ser mantida do alvo
    private static final double CLOSE_RANGE_DISTANCE = 150.0; // Distância considerada "curto alcance"
    private static final double MOVEMENT_DISTANCE = 150.0;   // Distância padrão de movimento
    private static final double STRAFE_DISTANCE = 100.0;     // Distância para movimento lateral (strafe)
    private static final double STRAFE_OFFSET_ANGLE = 15.0;  // Ângulo de ajuste para o strafe
    private static final double GUN_TARGET_TOLERANCE_DEGREES = 10.0; // Tolerância em graus para o canhão ser considerado na mira
    private static final int ESCAPE_THREAT_THRESHOLD = 2;        // Número de inimigos mirando para acionar a fuga
    private static final double AIMING_TOLERANCE_DEGREES = 20.0; // Tolerância para considerar que um inimigo está mirando
    private static final int TARGET_LOST_TICKS = 5;              // Ticks até o radar considerar o alvo perdido
    private static final int ENEMY_DATA_TIMEOUT_TICKS = 15;      // Ticks para dados de inimigos expirarem na análise de ameaça

    //== Loop Principal e Comportamento do Radar ==//
    public void run() {
        setBodyColor(Color.black);
        setGunColor(Color.gray);
        setRadarColor(Color.gray);
        setScanColor(Color.black);
        setBulletColor(Color.gray);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            if (targetName == null) {
                scanForClosestEnemy();
            }

            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                if (target == null || getTime() - target.lastSeen > TARGET_LOST_TICKS) {
                    targetName = null;
                    setTurnRadarRight(360);
                } else {
                    double radarTurn = getHeadingRadians() + target.lastBearingRadians - getRadarHeadingRadians();
                    setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn) * 2);
                }
            } else {
                setTurnRadarRight(360);
            }
            execute();
        }
    }

    //== Lógica Principal de Combate e Movimento ==//
    public void onScannedRobot(ScannedRobotEvent e) {
        String enemyName = e.getName();
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData());
        data.update(e, this);
        enemies.put(enemyName, data);
        scanForClosestEnemy();

        if (targetName == null || !targetName.equals(e.getName())) {
            return;
        }

        int threatCount = countIncomingAimers();

        if (threatCount >= ESCAPE_THREAT_THRESHOLD) {
            executeEscapeManeuver();
        } else {
            // Detecta tiro inimigo usando a energia anterior específica daquele inimigo
            double changeInEnergy = data.previousEnergy - e.getEnergy();
            if (changeInEnergy > 0 && changeInEnergy <= Rules.MAX_BULLET_POWER) {
                direction *= -1;
            }
            data.previousEnergy = e.getEnergy(); // Atualiza a energia anterior *deste* inimigo

            if (e.getDistance() < MINIMUM_DISTANCE) {
                setTurnRight(normalRelativeAngleDegrees(e.getBearing() + 90));
                setBack(MOVEMENT_DISTANCE);
            } else {
                setTurnRight(normalRelativeAngleDegrees(e.getBearing() + 90 - (STRAFE_OFFSET_ANGLE * direction)));
                setAhead(STRAFE_DISTANCE * direction);
            }
        }

        if (e.getDistance() <= MAX_FIRE_DISTANCE) {
            double firePower = calculateAdaptiveFirePower(e.getDistance());
            double absBearingRad = getHeadingRadians() + e.getBearingRadians();
            
            double compensacaoLinear = e.getVelocity() * Math.sin(e.getHeadingRadians() - absBearingRad) / Rules.getBulletSpeed(firePower);
            compensacaoLinear *= FATOR_DE_COMPENSACAO;

            if (e.getDistance() <= CLOSE_RANGE_DISTANCE) {
                compensacaoLinear *= 0.7; // Ajuste fino para curtas distâncias
            }

            double gunTurnRad = Utils.normalRelativeAngle(absBearingRad - getGunHeadingRadians() + compensacaoLinear);
            setTurnGunRightRadians(gunTurnRad);

            if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < GUN_TARGET_TOLERANCE_DEGREES) {
                setFire(firePower);
            }
        }
    }

    //== Análise de Ameaça ==//
    private int countIncomingAimers() {
        int aimingEnemies = 0;
        for (EnemyData enemy : enemies.values()) {
            if (getTime() - enemy.lastSeen > ENEMY_DATA_TIMEOUT_TICKS) continue;

            double angleToMe = Math.atan2(getX() - enemy.x, getY() - enemy.y);
            double angleDiff = Utils.normalRelativeAngle(enemy.headingRadians - angleToMe);

            if (Math.toDegrees(Math.abs(angleDiff)) < AIMING_TOLERANCE_DEGREES) {
                aimingEnemies++;
            }
        }
        return aimingEnemies;
    }

    //== Manobra de Fuga ==//
    private void executeEscapeManeuver() {
        double avgX = 0, avgY = 0;
        int threatCount = 0;

        for (EnemyData enemy : enemies.values()) {
            // Usa uma tolerância maior aqui para incluir inimigos relevantes
            if (getTime() - enemy.lastSeen < (ENEMY_DATA_TIMEOUT_TICKS + 5)) {
                avgX += enemy.x;
                avgY += enemy.y;
                threatCount++;
            }
        }

        if (threatCount > 0) {
            avgX /= threatCount;
            avgY /= threatCount;
            double escapeAngle = Math.atan2(getX() - avgX, getY() - avgY);
            setTurnRightRadians(Utils.normalRelativeAngle(escapeAngle - getHeadingRadians()));
            setAhead(MOVEMENT_DISTANCE);
            out.println("FUGINDO! Ameaças: " + threatCount);
        }
    }

    //== Reação a Eventos ==//
    public void onHitByBullet(HitByBulletEvent e) {
        direction *= -1;
        setAhead(MOVEMENT_DISTANCE * direction);
    }

    //== Lógica de colisão com parede ==//
    public void onHitWall(HitWallEvent e) {
        // Ao bater na parede, apenas inverte a direção do movimento.
        direction *= -1;
        setAhead(MOVEMENT_DISTANCE);
    }

    public void onHitRobot(HitRobotEvent e) {
        targetName = e.getName();
        out.println("Colisão! Novo alvo: " + targetName);

        double gunTurn = Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurn);
        
        // Atira com força máxima, usando a constante das regras do jogo
        setFire(Rules.MAX_BULLET_POWER);
        
        setBack(STRAFE_DISTANCE);
    }

    public void onRobotDeath(RobotDeathEvent e) {
        enemies.remove(e.getName());
        if (e.getName().equals(targetName)) {
            targetName = null;
            scanForClosestEnemy();
        }
    }

    //== Métodos Auxiliares ==//
    private double calculateAdaptiveFirePower(double distance) {
        double power = 4.0 - (2.5 * (Math.min(distance, MAX_FIRE_DISTANCE) / MAX_FIRE_DISTANCE));
        return Math.max(0.1, Math.min(Rules.MAX_BULLET_POWER, power));
    }

    private void scanForClosestEnemy() {
        double closestDistance = Double.MAX_VALUE;
        String closestEnemy = null;
        for (EnemyData e : enemies.values()) {
            if (getTime() - e.lastSeen > 10) continue;
            if (e.distance < closestDistance) {
                closestDistance = e.distance;
                closestEnemy = e.name;
            }
        }
        if (closestEnemy != null) {
            targetName = closestEnemy;
        }
    }
    
    private double normalRelativeAngleDegrees(double angle) {
        return Utils.normalRelativeAngleDegrees(angle);
    }

    //== Classe de Dados do Inimigo ==//
    static class EnemyData {
        String name;
        double energy, distance, headingRadians, velocity;
        double lastBearingRadians;
        long lastSeen;
        double x, y; // Posição absoluta do inimigo
        double previousEnergy = 100; // Energia anterior específica deste inimigo

        void update(ScannedRobotEvent e, AdvancedRobot robot) {
            this.name = e.getName();
            this.energy = e.getEnergy();
            this.lastBearingRadians = e.getBearingRadians();
            this.distance = e.getDistance();
            this.headingRadians = e.getHeadingRadians();
            this.velocity = e.getVelocity();
            this.lastSeen = e.getTime();

            double absBearingRad = robot.getHeadingRadians() + e.getBearingRadians();
            this.x = robot.getX() + e.getDistance() * Math.sin(absBearingRad);
            this.y = robot.getY() + e.getDistance() * Math.cos(absBearingRad);
        }
    }
    //== Comemoração de Vitória ==//
    public void onWin(WinEvent e) {
        for (int i = 0; i < 50; i++) {
            setBodyColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            setGunColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            setRadarColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f));
            
            turnRight(25);
            turnLeft(25);
            execute();
        }
    }
}
