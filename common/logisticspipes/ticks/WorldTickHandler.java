package logisticspipes.ticks;

import java.util.EnumSet;
import java.util.LinkedList;

import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.basic.ConverterPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.ItemIdentifier;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class WorldTickHandler implements ITickHandler {

	public static LinkedList<TileEntity> serverTilesToReplace = new LinkedList<TileEntity>();
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		if(type.contains(TickType.SERVER)) {
			while(serverTilesToReplace.size() > 0) {
				TileEntity tile = serverTilesToReplace.get(0);
				int x = tile.xCoord;
				int y = tile.yCoord;
				int z = tile.zCoord;
				World world = tile.worldObj;
				//TE or its chunk might've gone away while we weren't looking
				TileEntity tilecheck = world.getBlockTileEntity(x, y, z);
				if(tilecheck != tile) {
					serverTilesToReplace.remove(0);
					continue;
				}
				if(tile instanceof TileGenericPipe) {
					ConverterPipe cPipe = (ConverterPipe) ((TileGenericPipe)tile).pipe;
					((TileGenericPipe)tile).pipe = null;
					int newId = Integer.valueOf(cPipe.classId);
					world.setBlock(x, y, z, 0);
					CoreRoutedPipe newPipe = LogisticsBlockGenericPipe.createPipe(newId);
					LogisticsBlockGenericPipe.placePipe(newPipe, world, x, y, z, LogisticsPipes.LogisticsBlockGenericPipe.blockID, 0);
					TileEntity newTileT = world.getBlockTileEntity(x, y, z);
					if(!(newTileT instanceof LogisticsTileGenericPipe)) {
						throw new UnsupportedOperationException();
					}
					LogisticsTileGenericPipe newTile = (LogisticsTileGenericPipe) newTileT;
					newTile.readFromNBT(cPipe.nbtSettings);
				} else {
					LogisticsBlockGenericPipe.placePipe(null, world, x, y, z, LogisticsPipes.LogisticsBlockGenericPipe.blockID, 0);
					world.setBlockTileEntity(x, y, z, tilecheck);
				}
				serverTilesToReplace.remove(0);
			}
		}
		ItemIdentifier.tick();
		FluidIdentifier.initFromForge(true);
		if(type.contains(TickType.SERVER)) {
			HudUpdateTick.tick();
			SimpleServiceLocator.craftingPermissionManager.tick();
			if(LogisticsPipes.WATCHDOG) {
				Watchdog.tickServer();
			}
		} else {
			if(LogisticsPipes.WATCHDOG) {
				Watchdog.tickClient();
			}
		}
	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT, TickType.SERVER);
	}

	@Override
	public String getLabel() {
		return "LogisticsPipes WorldTick";
	}
}
