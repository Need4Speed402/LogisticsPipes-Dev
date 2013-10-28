package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import logisticspipes.LogisticsPipes;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.proxy.MainProxy;
import logisticspipes.textures.Textures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.BuildCraftCore;
import buildcraft.BuildCraftTransport;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.ISolidSideTile;
import buildcraft.core.BlockIndex;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.core.utils.Utils;
import buildcraft.transport.Gate;
import buildcraft.transport.IPipeRenderState;
import buildcraft.transport.ItemPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.render.PipeWorldRenderer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LogisticsBlockGenericPipe extends BlockContainer {

	public LogisticsBlockGenericPipe(int i) {
		super(i, Material.glass);
	}

	@Override
	public ArrayList<ItemStack> getBlockDropped(World world, int x, int y, int z, int metadata, int fortune) {
		if (MainProxy.isClient(world)) return null;
		ArrayList<ItemStack> result = new ArrayList<ItemStack>();
		int count = quantityDropped(metadata, fortune, world.rand);
		for (int i = 0; i < count; i++) {
			Pipe<?> pipe = getPipe(world, x, y, z);

			if (pipe == null) {
				pipe = pipeRemoved.get(new BlockIndex(x, y, z));
			}

			if (pipe != null) {
				if (pipe.itemID > 0 && pipe.itemID != LogisticsPipes.LogisticsBrokenItem.itemID) {
					pipe.dropContents();
					result.add(new ItemStack(pipe.itemID, 1, damageDropped(metadata)));
				}
			}
		}
		return result;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Icon getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int l) {
		TileEntity tile = iblockaccess.getBlockTileEntity(i, j, k);
		if(tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe)tile).pipe instanceof PipeBlockRequestTable) {
			PipeBlockRequestTable table = (PipeBlockRequestTable) ((LogisticsTileGenericPipe)tile).pipe;
			return table.getTextureFor(l);
		}
		if (!(tile instanceof IPipeRenderState))
			return null;
		return ((IPipeRenderState) tile).getRenderState().currentTexture;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addCollisionBoxesToList(World world, int i, int j, int k, AxisAlignedBB axisalignedbb, List arraylist, Entity par7Entity) {
		TileEntity tile1 = world.getBlockTileEntity(i, j, k);
		if (!(tile1 instanceof LogisticsTileGenericPipe)) return;
		LogisticsTileGenericPipe tileG = (LogisticsTileGenericPipe) tile1;
		if(tileG instanceof LogisticsTileGenericPipe && tileG.pipe instanceof PipeBlockRequestTable) {
			setBlockBounds(0, 0, 0, 1, 1, 1);
			AxisAlignedBB axisalignedbb1 = this.getCollisionBoundingBoxFromPool(world, i, j, k);
			if(axisalignedbb1 != null && axisalignedbb.intersectsWith(axisalignedbb1)) {
				arraylist.add(axisalignedbb1);
			}
			return;
		}
		setBlockBounds(Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMaxPos, Utils.pipeMaxPos, Utils.pipeMaxPos);
		super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		
		if (tileG.isPipeConnected(ForgeDirection.WEST)) {
			setBlockBounds(0.0F, Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMaxPos, Utils.pipeMaxPos, Utils.pipeMaxPos);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.isPipeConnected(ForgeDirection.EAST)) {
			setBlockBounds(Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMinPos, 1.0F, Utils.pipeMaxPos, Utils.pipeMaxPos);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.isPipeConnected(ForgeDirection.DOWN)) {
			setBlockBounds(Utils.pipeMinPos, 0.0F, Utils.pipeMinPos, Utils.pipeMaxPos, Utils.pipeMaxPos, Utils.pipeMaxPos);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.isPipeConnected(ForgeDirection.UP)) {
			setBlockBounds(Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMaxPos, 1.0F, Utils.pipeMaxPos);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.isPipeConnected(ForgeDirection.NORTH)) {
			setBlockBounds(Utils.pipeMinPos, Utils.pipeMinPos, 0.0F, Utils.pipeMaxPos, Utils.pipeMaxPos, Utils.pipeMaxPos);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.isPipeConnected(ForgeDirection.SOUTH)) {
			setBlockBounds(Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMaxPos, Utils.pipeMaxPos, 1.0F);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		float facadeThickness = PipeWorldRenderer.facadeThickness;

		if (tileG.hasFacade(ForgeDirection.EAST)) {
			setBlockBounds(1 - facadeThickness, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.hasFacade(ForgeDirection.WEST)) {
			setBlockBounds(0.0F, 0.0F, 0.0F, facadeThickness, 1.0F, 1.0F);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.hasFacade(ForgeDirection.UP)) {
			setBlockBounds(0.0F, 1 - facadeThickness, 0.0F, 1.0F, 1.0F, 1.0F);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.hasFacade(ForgeDirection.DOWN)) {
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, facadeThickness, 1.0F);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.hasFacade(ForgeDirection.SOUTH)) {
			setBlockBounds(0.0F, 0.0F, 1 - facadeThickness, 1.0F, 1.0F, 1.0F);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}

		if (tileG.hasFacade(ForgeDirection.NORTH)) {
			setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, facadeThickness);
			super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, arraylist, par7Entity);
		}
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}
	
	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int i, int j, int k) {
		TileEntity tile1 = world.getBlockTileEntity(i, j, k);
		if (!(tile1 instanceof LogisticsTileGenericPipe)) return AxisAlignedBB.getBoundingBox(i, j, k, i + 1, j + 1, k + 1);
		LogisticsTileGenericPipe tileG = (LogisticsTileGenericPipe) tile1;
		if(tileG instanceof LogisticsTileGenericPipe && tileG.pipe instanceof PipeBlockRequestTable) {
			return AxisAlignedBB.getBoundingBox((double) i + 0, (double) j + 0, (double) k + 0, (double) i + 1, (double) j + 1, (double) k + 1);
		}
		float xMin = Utils.pipeMinPos, xMax = Utils.pipeMaxPos, yMin = Utils.pipeMinPos, yMax = Utils.pipeMaxPos, zMin = Utils.pipeMinPos, zMax = Utils.pipeMaxPos;

		if (tileG.isPipeConnected(ForgeDirection.WEST) || tileG.hasFacade(ForgeDirection.WEST)) {
			xMin = 0.0F;
		}

		if (tileG.isPipeConnected(ForgeDirection.EAST) || tileG.hasFacade(ForgeDirection.EAST)) {
			xMax = 1.0F;
		}

		if (tileG.isPipeConnected(ForgeDirection.DOWN) || tileG.hasFacade(ForgeDirection.DOWN)) {
			yMin = 0.0F;
		}

		if (tileG.isPipeConnected(ForgeDirection.UP) || tileG.hasFacade(ForgeDirection.UP)) {
			yMax = 1.0F;
		}

		if (tileG.isPipeConnected(ForgeDirection.NORTH) || tileG.hasFacade(ForgeDirection.NORTH)) {
			zMin = 0.0F;
		}

		if (tileG.isPipeConnected(ForgeDirection.SOUTH) || tileG.hasFacade(ForgeDirection.SOUTH)) {
			zMax = 1.0F;
		}

		if (tileG.hasFacade(ForgeDirection.EAST) || tileG.hasFacade(ForgeDirection.WEST)) {
			yMin = 0.0F;
			yMax = 1.0F;
			zMin = 0.0F;
			zMax = 1.0F;
		}

		if (tileG.hasFacade(ForgeDirection.UP) || tileG.hasFacade(ForgeDirection.DOWN)) {
			xMin = 0.0F;
			xMax = 1.0F;
			zMin = 0.0F;
			zMax = 1.0F;
		}

		if (tileG.hasFacade(ForgeDirection.SOUTH) || tileG.hasFacade(ForgeDirection.NORTH)) {
			xMin = 0.0F;
			xMax = 1.0F;
			yMin = 0.0F;
			yMax = 1.0F;
		}
		return AxisAlignedBB.getBoundingBox((double) i + xMin, (double) j + yMin, (double) k + zMin, (double) i + xMax, (double) j + yMax, (double) k + zMax);
	}
	
	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 origin, Vec3 direction) {
		TileEntity tile1 = world.getBlockTileEntity(x, y, z);
		LogisticsTileGenericPipe tileG = (LogisticsTileGenericPipe) tile1;
		if(tileG instanceof LogisticsTileGenericPipe && tileG.pipe instanceof PipeBlockRequestTable) {
			this.setBlockBoundsBasedOnState(world, x, y, z);
			origin = origin.addVector(( -x), ( -y), ( -z));
			direction = direction.addVector(( -x), ( -y), ( -z));
			Vec3 vec32 = origin.getIntermediateWithXValue(direction, this.minX);
			Vec3 vec33 = origin.getIntermediateWithXValue(direction, this.maxX);
			Vec3 vec34 = origin.getIntermediateWithYValue(direction, this.minY);
			Vec3 vec35 = origin.getIntermediateWithYValue(direction, this.maxY);
			Vec3 vec36 = origin.getIntermediateWithZValue(direction, this.minZ);
			Vec3 vec37 = origin.getIntermediateWithZValue(direction, this.maxZ);
			if( !this.isVecInsideYZBounds(vec32)) {
				vec32 = null;
			}
			if( !this.isVecInsideYZBounds(vec33)) {
				vec33 = null;
			}
			if( !this.isVecInsideXZBounds(vec34)) {
				vec34 = null;
			}
			if( !this.isVecInsideXZBounds(vec35)) {
				vec35 = null;
			}
			if( !this.isVecInsideXYBounds(vec36)) {
				vec36 = null;
			}
			if( !this.isVecInsideXYBounds(vec37)) {
				vec37 = null;
			}
			Vec3 vec38 = null;
			if(vec32 != null && (vec38 == null || origin.squareDistanceTo(vec32) < origin.squareDistanceTo(vec38))) {
				vec38 = vec32;
			}
			if(vec33 != null && (vec38 == null || origin.squareDistanceTo(vec33) < origin.squareDistanceTo(vec38))) {
				vec38 = vec33;
			}
			if(vec34 != null && (vec38 == null || origin.squareDistanceTo(vec34) < origin.squareDistanceTo(vec38))) {
				vec38 = vec34;
			}
			if(vec35 != null && (vec38 == null || origin.squareDistanceTo(vec35) < origin.squareDistanceTo(vec38))) {
				vec38 = vec35;
			}
			if(vec36 != null && (vec38 == null || origin.squareDistanceTo(vec36) < origin.squareDistanceTo(vec38))) {
				vec38 = vec36;
			}
			if(vec37 != null && (vec38 == null || origin.squareDistanceTo(vec37) < origin.squareDistanceTo(vec38))) {
				vec38 = vec37;
			}
			if(vec38 == null) {
				return null;
			} else {
				byte b0 = -1;
				if(vec38 == vec32) {
					b0 = 4;
				}
				if(vec38 == vec33) {
					b0 = 5;
				}
				if(vec38 == vec34) {
					b0 = 0;
				}
				if(vec38 == vec35) {
					b0 = 1;
				}
				if(vec38 == vec36) {
					b0 = 2;
				}
				if(vec38 == vec37) {
					b0 = 3;
				}
				return new MovingObjectPosition(x, y, z, b0, vec38.addVector(x, y, z));
			}
		}
		RaytraceResult raytraceResult = doRayTrace(world, x, y, z, origin, direction);
		
		if (raytraceResult == null) {
			return null;
		} else {
			return raytraceResult.movingObjectPosition;
		}
	}
	
	private boolean isVecInsideYZBounds(Vec3 par1Vec3) {
		return par1Vec3 == null ? false : par1Vec3.yCoord >= this.minY && par1Vec3.yCoord <= this.maxY && par1Vec3.zCoord >= this.minZ && par1Vec3.zCoord <= this.maxZ;
	}
	
	private boolean isVecInsideXZBounds(Vec3 par1Vec3) {
		return par1Vec3 == null ? false : par1Vec3.xCoord >= this.minX && par1Vec3.xCoord <= this.maxX && par1Vec3.zCoord >= this.minZ && par1Vec3.zCoord <= this.maxZ;
	}
	
	private boolean isVecInsideXYBounds(Vec3 par1Vec3) {
		return par1Vec3 == null ? false : par1Vec3.xCoord >= this.minX && par1Vec3.xCoord <= this.maxX && par1Vec3.yCoord >= this.minY && par1Vec3.yCoord <= this.maxY;
	}

    public static Icon getRequestTableTextureFromSide(int l) {
    	ForgeDirection dir = ForgeDirection.getOrientation(l);
		switch(dir) {
			case UP:
				return Textures.LOGISTICS_REQUEST_TABLE[0];
			case DOWN:
				return Textures.LOGISTICS_REQUEST_TABLE[1];
			default:
				return Textures.LOGISTICS_REQUEST_TABLE[4];
		}
    }

	static enum Part {

		Pipe,
		Gate
	}

	static class RaytraceResult {

		RaytraceResult(Part hitPart, MovingObjectPosition movingObjectPosition) {
			this.hitPart = hitPart;
			this.movingObjectPosition = movingObjectPosition;
		}
		public Part hitPart;
		public MovingObjectPosition movingObjectPosition;
	}
	
	private static Random rand = new Random();
	private boolean skippedFirstIconRegister;
	private char renderAxis = 'a';

	@Override
	public float getBlockHardness(World par1World, int par2, int par3, int par4) {
		return BuildCraftTransport.pipeDurability;
	}

	@Override
	public int getRenderType() {
		return MainProxy.proxy.getPipeRenderId();
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean canBeReplacedByLeaves(World world, int x, int y, int z) {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	public void setRenderAxis(char axis) {
		this.renderAxis = axis;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
		if (renderAxis == 'x')
			return side == 4 || side == 5;
		if (renderAxis == 'y')
			return side == 0 || side == 1;
		if (renderAxis == 'z')
			return side == 2 || side == 3;
		return true;
	}

	@Override
	public boolean isBlockSolidOnSide(World world, int x, int y, int z, ForgeDirection side) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if (tile instanceof ISolidSideTile) {
			return ((ISolidSideTile) tile).isSolidOnSide(side);
		}
		return false;
	}

	public boolean isACube() {
		return false;
	}

	private RaytraceResult doRayTrace(World world, int x, int y, int z, EntityPlayer entityPlayer) {
		double pitch = Math.toRadians(entityPlayer.rotationPitch);
		double yaw = Math.toRadians(entityPlayer.rotationYaw);

		double dirX = -Math.sin(yaw) * Math.cos(pitch);
		double dirY = -Math.sin(pitch);
		double dirZ = Math.cos(yaw) * Math.cos(pitch);

		double reachDistance = 5;

		if (entityPlayer instanceof EntityPlayerMP) {
			reachDistance = ((EntityPlayerMP) entityPlayer).theItemInWorldManager.getBlockReachDistance();
		}

		Vec3 origin = Vec3.fakePool.getVecFromPool(entityPlayer.posX, entityPlayer.posY + 1.62 - entityPlayer.yOffset, entityPlayer.posZ);
		Vec3 direction = origin.addVector(dirX * reachDistance, dirY * reachDistance, dirZ * reachDistance);

		return doRayTrace(world, x, y, z, origin, direction);
	}

	private RaytraceResult doRayTrace(World world, int x, int y, int z, Vec3 origin, Vec3 direction) {
		float xMin = Utils.pipeMinPos, xMax = Utils.pipeMaxPos, yMin = Utils.pipeMinPos, yMax = Utils.pipeMaxPos, zMin = Utils.pipeMinPos, zMax = Utils.pipeMaxPos;

		TileEntity pipeTileEntity = world.getBlockTileEntity(x, y, z);

		LogisticsTileGenericPipe tileG = null;
		if (pipeTileEntity instanceof LogisticsTileGenericPipe)
			tileG = (LogisticsTileGenericPipe) pipeTileEntity;

		if (tileG == null)
			return null;

		Pipe<?> pipe = tileG.pipe;

		if (!isValid(pipe))
			return null;

		/**
		 * pipe hits along x, y, and z axis, gate (all 6 sides) [and
		 * wires+facades]
		 */
		MovingObjectPosition[] hits = new MovingObjectPosition[9];

		boolean needAxisCheck = false;
		boolean needCenterCheck = true;

		// check along the x axis

		if (tileG.isPipeConnected(ForgeDirection.WEST)) {
			xMin = 0.0F;
			needAxisCheck = true;
		}

		if (tileG.isPipeConnected(ForgeDirection.WEST)) {
			xMax = 1.0F;
			needAxisCheck = true;
		}

		if (needAxisCheck) {
			setBlockBounds(xMin, yMin, zMin, xMax, yMax, zMax);

			hits[0] = super.collisionRayTrace(world, x, y, z, origin, direction);
			xMin = Utils.pipeMinPos;
			xMax = Utils.pipeMaxPos;
			needAxisCheck = false;
			needCenterCheck = false; // center already checked through this axis
		}

		// check along the y axis

		if (tileG.isPipeConnected(ForgeDirection.DOWN)) {
			yMin = 0.0F;
			needAxisCheck = true;
		}

		if (tileG.isPipeConnected(ForgeDirection.UP)) {
			yMax = 1.0F;
			needAxisCheck = true;
		}

		if (needAxisCheck) {
			setBlockBounds(xMin, yMin, zMin, xMax, yMax, zMax);

			hits[1] = super.collisionRayTrace(world, x, y, z, origin, direction);
			yMin = Utils.pipeMinPos;
			yMax = Utils.pipeMaxPos;
			needAxisCheck = false;
			needCenterCheck = false; // center already checked through this axis
		}

		// check along the z axis

		if (tileG.isPipeConnected(ForgeDirection.NORTH)) {
			zMin = 0.0F;
			needAxisCheck = true;
		}

		if (tileG.isPipeConnected(ForgeDirection.SOUTH)) {
			zMax = 1.0F;
			needAxisCheck = true;
		}

		if (needAxisCheck) {
			setBlockBounds(xMin, yMin, zMin, xMax, yMax, zMax);

			hits[2] = super.collisionRayTrace(world, x, y, z, origin, direction);
			zMin = Utils.pipeMinPos;
			zMax = Utils.pipeMaxPos;
			needAxisCheck = false;
			needCenterCheck = false; // center already checked through this axis
		}

		// check center (only if no axis were checked/the pipe has no connections)

		if (needCenterCheck) {
			setBlockBounds(xMin, yMin, zMin, xMax, yMax, zMax);

			hits[0] = super.collisionRayTrace(world, x, y, z, origin, direction);
		}

		// gates

		if (pipe.hasGate()) {
			for (int side = 0; side < 6; side++) {
				setBlockBoundsToGate(ForgeDirection.VALID_DIRECTIONS[side]);

				hits[3 + side] = super.collisionRayTrace(world, x, y, z, origin, direction);
			}
		}

		// TODO: check wires, facades

		// get closest hit

		double minLengthSquared = Double.POSITIVE_INFINITY;
		int minIndex = -1;

		for (int i = 0; i < hits.length; i++) {
			MovingObjectPosition hit = hits[i];
			if (hit == null)
				continue;

			double lengthSquared = hit.hitVec.squareDistanceTo(origin);

			if (lengthSquared < minLengthSquared) {
				minLengthSquared = lengthSquared;
				minIndex = i;
			}
		}

		// reset bounds

		setBlockBounds(0, 0, 0, 1, 1, 1);

		if (minIndex == -1) {
			return null;
		} else {
			Part hitPart;

			if (minIndex < 3) {
				hitPart = Part.Pipe;
			} else {
				hitPart = Part.Gate;
			}

			return new RaytraceResult(hitPart, hits[minIndex]);
		}
	}

	private void setBlockBoundsToGate(ForgeDirection dir) {
		float min = Utils.pipeMinPos + 0.05F;
		float max = Utils.pipeMaxPos - 0.05F;

		switch (dir) {
			case DOWN:
				setBlockBounds(min, Utils.pipeMinPos - 0.10F, min, max, Utils.pipeMinPos, max);
				break;
			case UP:
				setBlockBounds(min, Utils.pipeMaxPos, min, max, Utils.pipeMaxPos + 0.10F, max);
				break;
			case NORTH:
				setBlockBounds(min, min, Utils.pipeMinPos - 0.10F, max, max, Utils.pipeMinPos);
				break;
			case SOUTH:
				setBlockBounds(min, min, Utils.pipeMaxPos, max, max, Utils.pipeMaxPos + 0.10F);
				break;
			case WEST:
				setBlockBounds(Utils.pipeMinPos - 0.10F, min, min, Utils.pipeMinPos, max, max);
				break;
			default:
			case EAST:
				setBlockBounds(Utils.pipeMaxPos, min, min, Utils.pipeMaxPos + 0.10F, max, max);
				break;
		}
	}

	public static void removePipe(Pipe<?> pipe) {
		if (pipe == null)
			return;

		if (isValid(pipe)) {
			pipe.onBlockRemoval();
		}

		World world = pipe.container.worldObj;

		if (world == null)
			return;

		int x = pipe.container.xCoord;
		int y = pipe.container.yCoord;
		int z = pipe.container.zCoord;

		if (lastRemovedDate != world.getTotalWorldTime()) {
			lastRemovedDate = world.getTotalWorldTime();
			pipeRemoved.clear();
		}

		pipeRemoved.put(new BlockIndex(x, y, z), pipe);
		world.removeBlockTileEntity(x, y, z);
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, int par5, int par6) {
		Utils.preDestroyBlock(world, x, y, z);
		removePipe(getPipe(world, x, y, z));
		super.breakBlock(world, x, y, z, par5, par6);
	}

	@Override
	public TileEntity createNewTileEntity(World var1) {
		return new LogisticsTileGenericPipe();
	}

	@Override
	public void dropBlockAsItemWithChance(World world, int i, int j, int k, int l, float f, int dmg) {

		if (CoreProxy.proxy.isRenderWorld(world))
			return;

		int i1 = quantityDropped(world.rand);
		for (int j1 = 0; j1 < i1; j1++) {
			if (world.rand.nextFloat() > f) {
				continue;
			}

			Pipe<?> pipe = getPipe(world, i, j, k);

			if (pipe == null) {
				pipe = pipeRemoved.get(new BlockIndex(i, j, k));
			}

			if (pipe != null) {
				int k1 = pipe.itemID;

				if (k1 > 0) {
					pipe.dropContents();
					dropBlockAsItem_do(world, i, j, k, new ItemStack(k1, 1, damageDropped(l)));
				}
			}
		}
	}

	@Override
	public int idDropped(int meta, Random rand, int dmg) {
		// Returns 0 to be safe - the id does not depend on the meta
		return 0;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public int idPicked(World world, int i, int j, int k) {
		Pipe<?> pipe = getPipe(world, i, j, k);

		if (pipe == null)
			return 0;
		else
			return pipe.itemID;
	}

	/* Wrappers ************************************************************ */
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int id) {
		super.onNeighborBlockChange(world, x, y, z, id);

		Pipe<?> pipe = getPipe(world, x, y, z);

		if (isValid(pipe)) {
			pipe.container.scheduleNeighborChange();
		}
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float par6, float par7, float par8, int meta) {
		super.onBlockPlaced(world, x, y, z, side, par6, par7, par8, meta);
		Pipe<?> pipe = getPipe(world, x, y, z);

		if (isValid(pipe)) {
			pipe.onBlockPlaced();
		}

		return meta;
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, x, y, z, placer, stack);
		Pipe<?> pipe = getPipe(world, x, y, z);

		if (isValid(pipe)) {
			pipe.onBlockPlacedBy(placer);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset) {
		super.onBlockActivated(world, x, y, z, player, side, xOffset, yOffset, zOffset);

		world.notifyBlocksOfNeighborChange(x, y, z, BuildCraftTransport.genericPipeBlock.blockID);

		Pipe<?> pipe = getPipe(world, x, y, z);

		if (isValid(pipe)) {

			// / Right click while sneaking without wrench to strip equipment
			// from the pipe.
			if (player.isSneaking()
					&& (player.getCurrentEquippedItem() == null || !(player.getCurrentEquippedItem().getItem() instanceof IToolWrench))) {

				if (pipe.hasGate() || pipe.isWired())
					return stripEquipment(pipe);

			} else if (player.getCurrentEquippedItem() == null) {
				// Fall through the end of the test
			} else if (player.getCurrentEquippedItem().itemID == Item.sign.itemID)
				// Sign will be placed anyway, so lets show the sign gui
				return false;
			else if (player.getCurrentEquippedItem().getItem() instanceof ItemPipe)
				return false;
			else if (player.getCurrentEquippedItem().getItem() instanceof IToolWrench)
				// Only check the instance at this point. Call the IToolWrench
				// interface callbacks for the individual pipe/logic calls
				return pipe.blockActivated(player);
			else if (player.getCurrentEquippedItem().getItem() == BuildCraftTransport.redPipeWire) {
				if (!pipe.wireSet[IPipe.WireColor.Red.ordinal()]) {
					pipe.wireSet[IPipe.WireColor.Red.ordinal()] = true;
					if (!player.capabilities.isCreativeMode) {
						player.getCurrentEquippedItem().splitStack(1);
					}
					pipe.signalStrength[IPipe.WireColor.Red.ordinal()] = 0;
					pipe.container.scheduleNeighborChange();
					return true;
				}
			} else if (player.getCurrentEquippedItem().getItem() == BuildCraftTransport.bluePipeWire) {
				if (!pipe.wireSet[IPipe.WireColor.Blue.ordinal()]) {
					pipe.wireSet[IPipe.WireColor.Blue.ordinal()] = true;
					if (!player.capabilities.isCreativeMode) {
						player.getCurrentEquippedItem().splitStack(1);
					}
					pipe.signalStrength[IPipe.WireColor.Blue.ordinal()] = 0;
					pipe.container.scheduleNeighborChange();
					return true;
				}
			} else if (player.getCurrentEquippedItem().getItem() == BuildCraftTransport.greenPipeWire) {
				if (!pipe.wireSet[IPipe.WireColor.Green.ordinal()]) {
					pipe.wireSet[IPipe.WireColor.Green.ordinal()] = true;
					if (!player.capabilities.isCreativeMode) {
						player.getCurrentEquippedItem().splitStack(1);
					}
					pipe.signalStrength[IPipe.WireColor.Green.ordinal()] = 0;
					pipe.container.scheduleNeighborChange();
					return true;
				}
			} else if (player.getCurrentEquippedItem().getItem() == BuildCraftTransport.yellowPipeWire) {
				if (!pipe.wireSet[IPipe.WireColor.Yellow.ordinal()]) {
					pipe.wireSet[IPipe.WireColor.Yellow.ordinal()] = true;
					if (!player.capabilities.isCreativeMode) {
						player.getCurrentEquippedItem().splitStack(1);
					}
					pipe.signalStrength[IPipe.WireColor.Yellow.ordinal()] = 0;
					pipe.container.scheduleNeighborChange();
					return true;
				}
			} else if (player.getCurrentEquippedItem().itemID == BuildCraftTransport.pipeGate.itemID
					|| player.getCurrentEquippedItem().itemID == BuildCraftTransport.pipeGateAutarchic.itemID)
				if (!pipe.hasGate()) {

					pipe.gate = Gate.makeGate(pipe, player.getCurrentEquippedItem());
					if (!player.capabilities.isCreativeMode) {
						player.getCurrentEquippedItem().splitStack(1);
					}
					pipe.container.scheduleRenderUpdate();
					return true;
				}

			boolean openGateGui = false;

			if (pipe.hasGate()) {
				RaytraceResult rayTraceResult = doRayTrace(world, x, y, z, player);

				if (rayTraceResult != null && rayTraceResult.hitPart == Part.Gate) {
					openGateGui = true;
				}
			}

			if (openGateGui) {
				pipe.gate.openGui(player);

				return true;
			} else
				return pipe.blockActivated(player);
		}

		return false;
	}

	private boolean stripEquipment(Pipe<?> pipe) {

		// Try to strip wires first, starting with yellow.
		for (IPipe.WireColor color : IPipe.WireColor.values()) {
			if (pipe.wireSet[color.reverse().ordinal()]) {
				if (!CoreProxy.proxy.isRenderWorld(pipe.container.worldObj)) {
					dropWire(color.reverse(), pipe);
				}
				pipe.wireSet[color.reverse().ordinal()] = false;
				// pipe.worldObj.markBlockNeedsUpdate(pipe.xCoord, pipe.yCoord, pipe.zCoord);
				pipe.container.scheduleRenderUpdate();
				return true;
			}
		}

		// Try to strip gate next
		if (pipe.hasGate()) {
			if (!CoreProxy.proxy.isRenderWorld(pipe.container.worldObj)) {
				pipe.gate.dropGate();
			}
			pipe.resetGate();
			return true;
		}

		return false;
	}

	/**
	 * Drops a pipe wire item of the passed color.
	 *
	 * @param color
	 */
	private void dropWire(IPipe.WireColor color, Pipe<?> pipe) {

		Item wireItem;
		switch (color) {
			case Red:
				wireItem = BuildCraftTransport.redPipeWire;
				break;
			case Blue:
				wireItem = BuildCraftTransport.bluePipeWire;
				break;
			case Green:
				wireItem = BuildCraftTransport.greenPipeWire;
				break;
			default:
				wireItem = BuildCraftTransport.yellowPipeWire;
		}
		pipe.dropItem(new ItemStack(wireItem));

	}

	@Override
	public void onEntityCollidedWithBlock(World world, int i, int j, int k, Entity entity) {
		super.onEntityCollidedWithBlock(world, i, j, k, entity);

		Pipe<?> pipe = getPipe(world, i, j, k);

		if (isValid(pipe)) {
			pipe.onEntityCollidedWithBlock(entity);
		}
	}

	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
		Pipe<?> pipe = getPipe(world, x, y, z);

		if (isValid(pipe))
			return pipe.canConnectRedstone();
		else
			return false;
	}

	@Override
	public int isProvidingStrongPower(IBlockAccess iblockaccess, int x, int y, int z, int l) {
		Pipe<?> pipe = getPipe(iblockaccess, x, y, z);

		if (isValid(pipe))
			return pipe.isPoweringTo(l);
		else
			return 0;
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public int isProvidingWeakPower(IBlockAccess world, int i, int j, int k, int l) {
		Pipe<?> pipe = getPipe(world, i, j, k);

		if (isValid(pipe))
			return pipe.isIndirectlyPoweringTo(l);
		else
			return 0;
	}

	@SuppressWarnings({"all"})
	@Override
	public void randomDisplayTick(World world, int i, int j, int k, Random random) {
		Pipe pipe = getPipe(world, i, j, k);

		if (isValid(pipe)) {
			pipe.randomDisplayTick(random);
		}
	}

	/* Registration ******************************************************** */
	public static Map<Integer, Class<? extends Pipe<?>>> pipes = new HashMap<Integer, Class<? extends Pipe<?>>>();
	static long lastRemovedDate = -1;
	public static Map<BlockIndex, Pipe<?>> pipeRemoved = new HashMap<BlockIndex, Pipe<?>>();

	public static ItemLogisticsPipe registerPipe(int key, Class<? extends Pipe<?>> clas) {
		ItemLogisticsPipe item = new ItemLogisticsPipe(key);
		item.setUnlocalizedName("logisticsPipe." + clas.getSimpleName().toLowerCase(Locale.ENGLISH));
		GameRegistry.registerItem(item, item.getUnlocalizedName());

		pipes.put(item.itemID, clas);

		Pipe<?> dummyPipe = createPipe(item.itemID);
		if (dummyPipe != null) {
			item.setPipeIconIndex(dummyPipe.getIconIndexForItem());
			MainProxy.proxy.setIconProviderFromPipe(item, dummyPipe);
		}
		return item;
	}

	public static boolean isPipeRegistered(int key) {
		return pipes.containsKey(key);
	}

	public static Pipe<?> createPipe(int key) {

		try {
			Class<? extends Pipe<?>> pipe = pipes.get(key);
			if (pipe != null)
				return pipe.getConstructor(int.class).newInstance(key);
			else {
				BuildCraftCore.bcLog.warning("Detected pipe with unknown key (" + key + "). Did you remove a buildcraft addon?");
			}

		} catch (Throwable t) {
			BuildCraftCore.bcLog.warning("Failed to create pipe with (" + key + "). No valid constructor found. Possibly a item ID conflit.");
		}

		return null;
	}

	public static boolean placePipe(Pipe<?> pipe, World world, int i, int j, int k, int blockId, int meta) {
		if (world.isRemote)
			return true;

		boolean placed = world.setBlock(i, j, k, blockId, meta, 1);

		if (placed) {

			LogisticsTileGenericPipe tile = (LogisticsTileGenericPipe) world.getBlockTileEntity(i, j, k);
			tile.initialize(pipe);
		}

		return placed;
	}

	public static Pipe<?> getPipe(IBlockAccess blockAccess, int i, int j, int k) {

		TileEntity tile = blockAccess.getBlockTileEntity(i, j, k);

		if (!(tile instanceof LogisticsTileGenericPipe) || tile.isInvalid())
			return null;

		return ((LogisticsTileGenericPipe) tile).pipe;
	}

	public static boolean isFullyDefined(Pipe<?> pipe) {
		return pipe != null && pipe.transport != null;
	}

	public static boolean isValid(Pipe<?> pipe) {
		return isFullyDefined(pipe);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister) {
		/*if (!skippedFirstIconRegister) {
			skippedFirstIconRegister = true;
			return;
		}
		*/
		for (int i : pipes.keySet()) {
			Pipe<?> dummyPipe = createPipe(i);
			if (dummyPipe != null) {
				dummyPipe.getIconProvider().registerIcons(iconRegister);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Icon getIcon(int par1, int par2) {
		return BuildCraftTransport.instance.pipeIconProvider.getIcon(PipeIconProvider.TYPE.Stripes.ordinal());
	}

	/**
	 * Spawn a digging particle effect in the world, this is a wrapper around
	 * EffectRenderer.addBlockHitEffects to allow the block more control over
	 * the particles. Useful when you have entirely different texture sheets for
	 * different sides/locations in the world.
	 *
	 * @param world The current world
	 * @param target The target the player is looking at {x/y/z/side/sub}
	 * @param effectRenderer A reference to the current effect renderer.
	 * @return True to prevent vanilla digging particles form spawning.
	 */
	@SideOnly(Side.CLIENT)
	@Override
	public boolean addBlockHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer) {
		int x = target.blockX;
		int y = target.blockY;
		int z = target.blockZ;

		Pipe<?> pipe = getPipe(worldObj, x, y, z);
		if (pipe == null)
			return false;

		Icon icon = pipe.getIconProvider().getIcon(pipe.getIconIndexForItem());

		int sideHit = target.sideHit;

		Block block = BuildCraftTransport.genericPipeBlock;
		float b = 0.1F;
		double px = x + rand.nextDouble() * (block.getBlockBoundsMaxX() - block.getBlockBoundsMinX() - (b * 2.0F)) + b + block.getBlockBoundsMinX();
		double py = y + rand.nextDouble() * (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() - (b * 2.0F)) + b + block.getBlockBoundsMinY();
		double pz = z + rand.nextDouble() * (block.getBlockBoundsMaxZ() - block.getBlockBoundsMinZ() - (b * 2.0F)) + b + block.getBlockBoundsMinZ();

		if (sideHit == 0) {
			py = (double) y + block.getBlockBoundsMinY() - (double) b;
		}

		if (sideHit == 1) {
			py = (double) y + block.getBlockBoundsMaxY() + (double) b;
		}

		if (sideHit == 2) {
			pz = (double) z + block.getBlockBoundsMinZ() - (double) b;
		}

		if (sideHit == 3) {
			pz = (double) z + block.getBlockBoundsMaxZ() + (double) b;
		}

		if (sideHit == 4) {
			px = (double) x + block.getBlockBoundsMinX() - (double) b;
		}

		if (sideHit == 5) {
			px = (double) x + block.getBlockBoundsMaxX() + (double) b;
		}

		EntityDiggingFX fx = new EntityDiggingFX(worldObj, px, py, pz, 0.0D, 0.0D, 0.0D, block, sideHit, worldObj.getBlockMetadata(x, y, z));
		fx.setParticleIcon(icon);
		effectRenderer.addEffect(fx.applyColourMultiplier(x, y, z).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
		return true;
	}

	/**
	 * Spawn particles for when the block is destroyed. Due to the nature of how
	 * this is invoked, the x/y/z locations are not always guaranteed to host
	 * your block. So be sure to do proper sanity checks before assuming that
	 * the location is this block.
	 *
	 * @param world The current world
	 * @param x X position to spawn the particle
	 * @param y Y position to spawn the particle
	 * @param z Z position to spawn the particle
	 * @param meta The metadata for the block before it was destroyed.
	 * @param effectRenderer A reference to the current effect renderer.
	 * @return True to prevent vanilla break particles from spawning.
	 */
	@SideOnly(Side.CLIENT)
	@Override
	public boolean addBlockDestroyEffects(World worldObj, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
		Pipe<?> pipe = getPipe(worldObj, x, y, z);
		if (pipe == null)
			return false;

		Icon icon = pipe.getIconProvider().getIcon(pipe.getIconIndexForItem());

		byte its = 4;
		for (int i = 0; i < its; ++i) {
			for (int j = 0; j < its; ++j) {
				for (int k = 0; k < its; ++k) {
					double px = x + (i + 0.5D) / (double) its;
					double py = y + (j + 0.5D) / (double) its;
					double pz = z + (k + 0.5D) / (double) its;
					int random = rand.nextInt(6);
					EntityDiggingFX fx = new EntityDiggingFX(worldObj, px, py, pz, px - x - 0.5D, py - y - 0.5D, pz - z - 0.5D, BuildCraftTransport.genericPipeBlock, random, meta);
					fx.setParticleIcon(icon);
					effectRenderer.addEffect(fx.applyColourMultiplier(x, y, z));
				}
			}
		}
		return true;
	}
	public static int facadeRenderColor = -1;

	@Override
	public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
		if (facadeRenderColor != -1) {
			return facadeRenderColor;
		}
		return super.colorMultiplier(world, x, y, z);
	}
}
