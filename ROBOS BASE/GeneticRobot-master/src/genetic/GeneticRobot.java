package genetic;

import java.awt.Color;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.MoveCompleteCondition;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

/**
 * GeneticRobot - A robot with genetic movement.
 */
public class GeneticRobot extends AdvancedRobot {
	
	private int move;
	
	Chromosome chromosome;
	
	public void run() {
		
		setBodyColor(Color.red);
		setGunColor(Color.blue);
		setRadarColor(Color.green);
		
		GeneticManager.loadRobot(this);
		
		move = 0;
		
		while (true) {
			doMove();
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		fire(chromosome.getBulletPower());
	}
	
	public void onWin(WinEvent e) {
		GeneticManager.onRobotWin(chromosome);
	}
	
	public void onRoundEnded(RoundEndedEvent e) {
		GeneticManager.onRoundEnded(chromosome);
	}
	
	public void onBattleEnded(BattleEndedEvent e) {
		GeneticManager.onBattleEnded(chromosome);
	}
	
	private void doMove() {
		setAhead(chromosome.getMoveDistance().get(move));
		setTurnRight(chromosome.getMoveRotation().get(move));
		waitFor(new MoveCompleteCondition(this));
		
		move++;
		if(move >= chromosome.getMoveRotation().size()) {
			move = 0;
		}
	}
}
