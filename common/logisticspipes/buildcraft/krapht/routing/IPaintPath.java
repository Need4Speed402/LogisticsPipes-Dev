/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.buildcraft.krapht.routing;

import net.minecraft.src.World;
import buildcraft.api.core.Orientations;
import buildcraft.api.core.Position;

public interface IPaintPath {
	public void addLaser(World worldObj, Position start, Orientations o);
}