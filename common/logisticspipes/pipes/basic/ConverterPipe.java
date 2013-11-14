package logisticspipes.pipes.basic;

import logisticspipes.proxy.MainProxy;
import logisticspipes.ticks.WorldTickHandler;
import logisticspipes.transport.PipeTransportLogistics;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.api.core.IIconProvider;
import buildcraft.transport.Pipe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ConverterPipe extends Pipe<PipeTransportLogistics> {
	
	public final String classId;
	public NBTTagCompound nbtSettings;
	
	public ConverterPipe(int itemID, String realClassId) {
		super(new PipeTransportLogistics(), itemID);
		classId = realClassId;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIconProvider getIconProvider() {
		return new IIconProvider() {
			@Override @SideOnly(Side.CLIENT) public void registerIcons(IconRegister iconRegister) {}
			@Override @SideOnly(Side.CLIENT) public Icon getIcon(int iconIndex) {return null;}
		};
	}

	@Override
	public int getIconIndex(ForgeDirection direction) {
		return 0;
	}

	@Override
	public void updateEntity() {
		TileEntity tile = getWorld().getBlockTileEntity(this.container.xCoord, this.container.yCoord, this.container.zCoord);
		if(tile != this.container) {
			new UnsupportedOperationException("These have to be the same").printStackTrace();
		}
		if(MainProxy.isServer(getWorld())) {
			WorldTickHandler.serverTilesToReplace.add(this.container);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		nbtSettings = (NBTTagCompound) data.copy();
		super.readFromNBT(data);
	}
}
