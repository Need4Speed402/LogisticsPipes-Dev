package logisticspipes.utils;

import lombok.Data;

@Data
public class Coordinates {
	public Coordinates(double pX, double pY, double pZ) {
		x = pX;
		y = pY;
		z = pZ;
	}
	protected double x;
	protected double y;
	protected double z;
}
