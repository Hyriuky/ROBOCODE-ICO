package OptimusPrime;

import robocode.*;

public class OptimusPrime extends Robot {
    private boolean bateuNaParede = false; // Variável para rastrear se bateu na parede
    private double energiaAnterior = 100.0; // Rastreia a energia anterior do robô

    public void run() {
        // Define a cor do robô como preto
        setColors(java.awt.Color.black, java.awt.Color.black, java.awt.Color.black);

        while (true) {
            // Evita bater nas bordas da arena
            if (bateuNaParede) {
                // Se bateu na parede, vire para longe dela
                turnRight(90);
                ahead(100);
                bateuNaParede = false;
            }

            // Gira a arma para procurar inimigos
            turnGunRight(360);

            // Move-se para frente
            ahead(100);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // Desvia das balas inimigas
        if (getEnergy() < energiaAnterior) {
            turnRight(e.getBearing());
            ahead(100);
        }

        // Atira com precisão
        fire(2); // Ajuste a potência do tiro conforme necessário
        energiaAnterior = getEnergy(); // Atualiza a energia anterior

        // Implemente a lógica de cura aqui (substitua com sua própria lógica)
        if (getEnergy() < 50) {
            // Se a energia estiver abaixo de 50, tente se curar
            heal();
        }
    }

    public void onHitWall(HitWallEvent e) {
        // Quando bate na parede, ativa a variável bateuNaParede
        bateuNaParede = true;
    }

    private void heal() {
        // Implemente a lógica de cura aqui (substitua com sua própria lógica)
        if (getEnergy() >= 30) {
            // Se a energia for maior ou igual a 30, vire para a esquerda e ande para trás
            turnLeft(90);
            back(30);
        }
    }
}
