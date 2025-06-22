package genetic;

import java.util.List;

public class Chromosome implements Comparable<Chromosome> {
	
	private int id;
	private int fitness;
	
	private List<Double> moveRotation;
	private List<Double> moveDistance;
	private double bulletPower;
	
	public Chromosome(int id) {
		this.id = id;
		fitness = 0;
	}
	
	public int getId() {
		return id;
	}
	
	public int getFitness() {
		return fitness;
	}
	
	public void increaseFitness() {
		fitness += 1;
	}
	
	public void setFitness(int fitness) {
		this.fitness = fitness;
	}
	
	public double getBulletPower() {
		return bulletPower;
	}

	public void setBulletPower(double bulletPower) {
		this.bulletPower = bulletPower;
	}
	
	public List<Double> getMoveRotation() {
		return moveRotation;
	}
	
	public void setMoveRotation(List<Double> moveRotation) {
		this.moveRotation = moveRotation;
	}
	
	public List<Double> getMoveDistance() {
		return moveDistance;
	}
	
	public void setMoveDistance(List<Double> moveDistance) {
		this.moveDistance = moveDistance;
	}

	@Override
	public int compareTo(Chromosome c) {
		if (fitness < c.fitness)
			return -1;
		if (fitness > c.fitness)
			return 1;
		return 0;
	}
}
