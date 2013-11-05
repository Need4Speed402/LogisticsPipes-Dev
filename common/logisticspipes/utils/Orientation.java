package logisticspipes.utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraftforge.common.ForgeDirection;

@Data
@EqualsAndHashCode(callSuper = true)
public class Orientation extends Coordinates {
	public Orientation(double pX, double pY, double pZ, ForgeDirection pDir) {
		super(pX, pY, pZ);
		dir = pDir;
	}
	
	private ForgeDirection	dir;
	
	public void moveForward(double d) {
		switch(dir) {
			case UP:
				y = y + d;
				break;
			case DOWN:
				y = y - d;
				break;
			case SOUTH:
				z = z + d;
				break;
			case NORTH:
				z = z - d;
				break;
			case EAST:
				x = x + d;
				break;
			case WEST:
				x = x - d;
				break;
			default:
		}
	}
	
	public void moveBackwards(double d) {
		moveForward(-d);
	}
	
	public void moveUp(double d) {
		switch(dir) {
			case SOUTH:
			case NORTH:
			case EAST:
			case WEST:
				y = y + d;
				break;
			default:
		}
		
	}
	
	public void moveDown(double d) {
		moveUp(-d);
	}
	
	public void moveRight(double d) {
		switch(dir) {
			case SOUTH:
				x = x - d;
				break;
			case NORTH:
				x = x + d;
				break;
			case EAST:
				z = z + d;
				break;
			case WEST:
				z = z - d;
				break;
			default:
		}
	}
	
	public void moveLeft(double d) {
		moveRight(-d);
	}
}
