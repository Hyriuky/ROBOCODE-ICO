import robocode.*; // Importa a biblioteca principal do Robocode
import java.awt.Color; // Importa a classe de cores
import java.util.*; // Importa as estruturas de dados, como Map
import java.awt.geom.Point2D; // Importa a classe para cálculos com pontos em 2D

public class RoboTesteV1 extends AdvancedRobot {

    // == Sistema de movimentação do OmniBotV20 == //
	
    private int direction = 1; // Direção atual do robô (1 para frente, -1 para trás)
    private double previousEnergy = 100; // Armazena a energia anterior do inimigo para detectar tiros

    // == Sistema de tiro inteligente  == //
    private static final double MAX_FIRE_DISTANCE = 350.0; // Distância máxima para disparo

    // == Radar e mira == //
    private Map<String, EnemyData> enemies = new HashMap<>(); // Mapeia nomes de inimigos para seus dados
    private String targetName = null; // Nome do inimigo atual a ser seguido/atacado

    public void run() {
		setBodyColor(Color.red); // Cor do corpo
		setGunColor(Color.black); // Cor da arma
		setRadarColor(Color.red); // Cor do radar
		setScanColor(Color.red); // Cor do scan
		setBulletColor(Color.blue); // Cor da bala
        setAdjustGunForRobotTurn(true); // Permite que a arma se mova independentemente do corpo
        setAdjustRadarForRobotTurn(true); // Permite que o radar se mova independentemente do corpo

        while (true) {
            scanForClosestEnemy(); // Atualiza o alvo mais próximo

            if (targetName != null) {
                EnemyData target = enemies.get(targetName); // Pega dados do inimigo atual
                if (target == null || getTime() - target.lastSeen > 5) {
                    targetName = null; // Reseta o alvo se ele sumiu
                }
            }

            if (targetName != null) {
                EnemyData target = enemies.get(targetName);
                double radarTurn = getHeading() + target.lastBearing - getRadarHeading(); // Calcula quanto o radar deve virar
                setTurnRadarRight(normalRelativeAngleDegrees(radarTurn) * 2); // Gira o radar para mirar no inimigo
            } else {
                setTurnRadarRight(360); // Gira o radar continuamente para procurar inimigos
            }

            execute(); // Executa os comandos pendentes
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	
        // == Atualização do sistema de rastreamento == //
        String enemyName = e.getName(); // Nome do robô escaneado
        EnemyData data = enemies.getOrDefault(enemyName, new EnemyData()); // Recupera ou cria dados do inimigo
        data.update(e); // Atualiza dados com base no escaneamento atual
        enemies.put(enemyName, data); // Armazena os dados no mapa
        scanForClosestEnemy(); // Atualiza o inimigo mais próximo

        // == NOVA MOVIMENTAÇÃO (padrão OmniBotV20) == //
        double changeInEnergy = previousEnergy - e.getEnergy(); // Detecta se o inimigo atirou
        if (changeInEnergy > 0 && changeInEnergy <= 3.0) { // Se perdeu energia, provavelmente atirou
            direction *= -1; // Inverte a direção
            setAhead(150 * direction); // Move-se na nova direção
        }
        previousEnergy = e.getEnergy(); // Atualiza energia anterior

        double angle = normalRelativeAngleDegrees(e.getBearing() + 90 - (15 * direction)); // Gira perpendicular ao inimigo
        setTurnRight(angle); // Gira o robô
        setAhead(100 * direction); // Anda na nova direção

        // == Sistema de tiro inteligente adaptado == //
        if (e.getDistance() <= MAX_FIRE_DISTANCE) { // Se o inimigo está ao alcance
            double firePower = calculateAdaptiveFirePower(e.getDistance()); // Calcula potência de fogo adaptativa
            double bulletSpeed = 20 - 3 * firePower; // Calcula velocidade da bala

            // Calcula posição atual do inimigo
            double enemyX = getX() + e.getDistance() * Math.sin(Math.toRadians(getHeading() + e.getBearing()));
            double enemyY = getY() + e.getDistance() * Math.cos(Math.toRadians(getHeading() + e.getBearing()));

            double enemyHeading = Math.toRadians(e.getHeading()); // Direção do inimigo
            double enemyVelocity = e.getVelocity(); // Velocidade do inimigo

            double deltaTime = 0;
            double predictedX = enemyX;
            double predictedY = enemyY;

            // Simula onde o inimigo estará quando a bala chegar
            while ((++deltaTime) * bulletSpeed < Point2D.distance(getX(), getY(), predictedX, predictedY)) {
                predictedX += Math.sin(enemyHeading) * enemyVelocity;
                predictedY += Math.cos(enemyHeading) * enemyVelocity;

                // Impede que o alvo extrapole os limites da arena
                if (predictedX < 18.0 || predictedY < 18.0 || 
                    predictedX > getBattleFieldWidth() - 18.0 || 
                    predictedY > getBattleFieldHeight() - 18.0) {
                    predictedX = Math.min(Math.max(18.0, predictedX), getBattleFieldWidth() - 18.0);
                    predictedY = Math.min(Math.max(18.0, predictedY), getBattleFieldHeight() - 18.0);
                    break;
                }
            }

            // Calcula o ângulo para virar a arma até a posição prevista
            double theta = normalRelativeAngle(Math.atan2(predictedX - getX(), predictedY - getY()) - 
                             Math.toRadians(getGunHeading()));
            setTurnGunRight(Math.toDegrees(theta)); // Gira a arma

            // Atira se a arma estiver pronta
            if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
                setFire(firePower); // Dispara com a potência calculada
            }
        }
    }

    // == Métodos de reação a colisões (novos) == //

    public void onHitByBullet(HitByBulletEvent e) {
        direction *= -1; // Inverte a direção
        setTurnRight(90 - e.getBearing()); // Gira perpendicular ao tiro
        setAhead(100 * direction); // Se afasta
    }

    public void onHitWall(HitWallEvent e) {
        direction *= -1; // Inverte direção
        setBack(50); // Recuo
        setTurnRight(90); // Gira para longe da parede
    }

    public void onHitRobot(HitRobotEvent e) {
        targetName = e.getName(); // Define o novo alvo como o robô colidido
        setTurnRadarRight(360); // Força um giro rápido do radar para tentar localizá-lo imediatamente
		double angle = normalRelativeAngleDegrees(e.getBearing()); // Calcula o ângulo para girar a arma
        setTurnGunRight(angle); // Gira arma para o inimigo
        setFire(3); // Dispara com força máxima
        direction *= -1; // Muda direção
        setBack(100); // Se afasta
    }

    // == Métodos auxiliares == //

    private double calculateAdaptiveFirePower(double distance) {
        distance = Math.min(distance, MAX_FIRE_DISTANCE); // Limita distância máxima
        double power = 4.0 - (2.9 * (distance / MAX_FIRE_DISTANCE)); // Fórmula da força do tiro
        return Math.max(0.1, Math.min(3.0, power)); // Garante que a força esteja no intervalo válido
    }

    private void scanForClosestEnemy() {
        double closestDistance = Double.MAX_VALUE;
        String closestEnemy = null;

        Iterator<Map.Entry<String, EnemyData>> it = enemies.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EnemyData> entry = it.next();
            EnemyData e = entry.getValue();

            if (getTime() - e.lastSeen > 10) { // Remove inimigos não vistos há muito tempo
                it.remove();
                continue;
            }

            if (e.distance < closestDistance) { // Encontra o inimigo mais próximo
                closestDistance = e.distance;
                closestEnemy = entry.getKey();
            }
        }
        targetName = closestEnemy; // Define o alvo mais próximo
    }
	
// == Utilitários == //

private static double normalRelativeAngle(double angle) {
    // Ajusta o ângulo para que ele fique no intervalo de -PI a PI (radianos)
    while (angle > Math.PI) angle -= 2 * Math.PI;
    while (angle < -Math.PI) angle += 2 * Math.PI;
    return angle; // Retorna o ângulo normalizado
}

private static double normalRelativeAngleDegrees(double angle) {
    // Ajusta o ângulo para que ele fique no intervalo de -180 a 180 graus
    while (angle > 180) angle -= 360;
    while (angle < -180) angle += 360;
    return angle; // Retorna o ângulo normalizado
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
        for (int i = 0; i < 50; i++) {
            setBodyColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f)); // Troca cor do corpo
            setGunColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f)); // Troca cor da arma
            setRadarColor(Color.getHSBColor((float)Math.random(), 1.0f, 1.0f)); // Troca cor do radar
            turnRight(25); // "Dança" girando pra direita
            turnLeft(25);  // "Dança" girando pra esquerda
            execute(); // Executa os comandos
        }
    }
}
