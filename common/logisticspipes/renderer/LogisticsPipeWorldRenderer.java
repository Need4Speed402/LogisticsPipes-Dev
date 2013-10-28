package logisticspipes.renderer;

import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.api.core.IIconProvider;
import buildcraft.core.utils.Utils;
import buildcraft.transport.IPipeRenderState;
import buildcraft.transport.PipeRenderState;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public class LogisticsPipeWorldRenderer implements ISimpleBlockRenderingHandler {

	private void renderAllFaceExeptAxe(RenderBlocks renderblocks, LogisticsBlockGenericPipe block, Icon icon, int x, int y, int z, char axe) {
		float minX = (float) renderblocks.renderMinX;
		float minY = (float) renderblocks.renderMinY;
		float minZ = (float) renderblocks.renderMinZ;
		float maxX = (float) renderblocks.renderMaxX;
		float maxY = (float) renderblocks.renderMaxY;
		float maxZ = (float) renderblocks.renderMaxZ;
		if (axe != 'x') {
			renderTwoWayXFace(renderblocks, block, icon, x, y, z, minY, minZ, maxY, maxZ, minX);
			renderTwoWayXFace(renderblocks, block, icon, x, y, z, minY, minZ, maxY, maxZ, maxX);
		}
		if (axe != 'y') {
			renderTwoWayYFace(renderblocks, block, icon, x, y, z, minX, minZ, maxX, maxZ, minY);
			renderTwoWayYFace(renderblocks, block, icon, x, y, z, minX, minZ, maxX, maxZ, maxY);
		}
		if (axe != 'z') {
			renderTwoWayZFace(renderblocks, block, icon, x, y, z, minX, minY, maxX, maxY, minZ);
			renderTwoWayZFace(renderblocks, block, icon, x, y, z, minX, minY, maxX, maxY, maxZ);
		}
	}

	private void renderTwoWayXFace(RenderBlocks renderblocks, LogisticsBlockGenericPipe block, Icon icon, int xCoord, int yCoord, int zCoord, float minY, float minZ, float maxY, float maxZ, float x) {
		renderblocks.setRenderBounds(x, minY, minZ, x, maxY, maxZ);
		block.setRenderAxis('x');
		renderblocks.renderStandardBlock(block, xCoord, yCoord, zCoord);
		block.setRenderAxis('a');
	}

	private void renderTwoWayYFace(RenderBlocks renderblocks, LogisticsBlockGenericPipe block, Icon icon, int xCoord, int yCoord, int zCoord, float minX, float minZ, float maxX, float maxZ, float y) {
		renderblocks.setRenderBounds(minX, y, minZ, maxX, y, maxZ);
		block.setRenderAxis('y');
		renderblocks.renderStandardBlock(block, xCoord, yCoord, zCoord);
		block.setRenderAxis('a');
	}

	private void renderTwoWayZFace(RenderBlocks renderblocks, LogisticsBlockGenericPipe block, Icon icon, int xCoord, int yCoord, int zCoord, float minX, float minY, float maxX, float maxY, float z) {
		renderblocks.setRenderBounds(minX, minY, z, maxX, maxY, z);
		block.setRenderAxis('z');
		renderblocks.renderStandardBlock(block, xCoord, yCoord, zCoord);
		block.setRenderAxis('a');
	}

	public void renderPipe(RenderBlocks renderblocks, IBlockAccess iblockaccess, LogisticsBlockGenericPipe block, IPipeRenderState renderState, int x, int y, int z) {
		if(renderState instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe)renderState).pipe instanceof PipeBlockRequestTable) {
			PipeRenderState state = renderState.getRenderState();
			IIconProvider icons = renderState.getPipeIcons();
			if (icons == null) return;
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.UNKNOWN));
			block.setBlockBounds(0, 0, 0, 1, 1, 1);
			renderblocks.setRenderBoundsFromBlock(block);
			renderblocks.renderStandardBlock(block, x, y, z);
			return;
		}

		float minSize = Utils.pipeMinPos;
		float maxSize = Utils.pipeMaxPos;

		PipeRenderState state = renderState.getRenderState();
		IIconProvider icons = renderState.getPipeIcons();
		if (icons == null)
			return;

		boolean west = false;
		boolean east = false;
		boolean down = false;
		boolean up = false;
		boolean north = false;
		boolean south = false;

		if (state.pipeConnectionMatrix.isConnected(ForgeDirection.WEST)) {
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.WEST));
			renderblocks.setRenderBounds(0.0F, minSize, minSize, minSize, maxSize, maxSize);
			renderAllFaceExeptAxe(renderblocks, block, state.currentTexture, x, y, z, 'x');
			west = true;
		}

		if (state.pipeConnectionMatrix.isConnected(ForgeDirection.EAST)) {
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.EAST));
			renderblocks.setRenderBounds(maxSize, minSize, minSize, 1.0F, maxSize, maxSize);
			renderAllFaceExeptAxe(renderblocks, block, state.currentTexture, x, y, z, 'x');
			east = true;
		}

		if (state.pipeConnectionMatrix.isConnected(ForgeDirection.DOWN)) {
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.DOWN));
			renderblocks.setRenderBounds(minSize, 0.0F, minSize, maxSize, minSize, maxSize);
			renderAllFaceExeptAxe(renderblocks, block, state.currentTexture, x, y, z, 'y');
			down = true;
		}

		if (state.pipeConnectionMatrix.isConnected(ForgeDirection.UP)) {
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.UP));
			renderblocks.setRenderBounds(minSize, maxSize, minSize, maxSize, 1.0F, maxSize);
			renderAllFaceExeptAxe(renderblocks, block, state.currentTexture, x, y, z, 'y');
			up = true;
		}

		if (state.pipeConnectionMatrix.isConnected(ForgeDirection.NORTH)) {
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.NORTH));
			renderblocks.setRenderBounds(minSize, minSize, 0.0F, maxSize, maxSize, minSize);
			renderAllFaceExeptAxe(renderblocks, block, state.currentTexture, x, y, z, 'z');
			north = true;
		}

		if (state.pipeConnectionMatrix.isConnected(ForgeDirection.SOUTH)) {
			state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.SOUTH));
			renderblocks.setRenderBounds(minSize, minSize, maxSize, maxSize, maxSize, 1.0F);
			renderAllFaceExeptAxe(renderblocks, block, state.currentTexture, x, y, z, 'z');
			south = true;
		}

		state.currentTexture = icons.getIcon(state.textureMatrix.getTextureIndex(ForgeDirection.UNKNOWN));
		renderblocks.setRenderBounds(minSize, minSize, minSize, maxSize, maxSize, maxSize);
		if (!west)
			renderTwoWayXFace(renderblocks, block, state.currentTexture, x, y, z, minSize, minSize, maxSize, maxSize, minSize);
		if (!east)
			renderTwoWayXFace(renderblocks, block, state.currentTexture, x, y, z, minSize, minSize, maxSize, maxSize, maxSize);
		if (!down)
			renderTwoWayYFace(renderblocks, block, state.currentTexture, x, y, z, minSize, minSize, maxSize, maxSize, minSize);
		if (!up)
			renderTwoWayYFace(renderblocks, block, state.currentTexture, x, y, z, minSize, minSize, maxSize, maxSize, maxSize);
		if (!north)
			renderTwoWayZFace(renderblocks, block, state.currentTexture, x, y, z, minSize, minSize, maxSize, maxSize, minSize);
		if (!south)
			renderTwoWayZFace(renderblocks, block, state.currentTexture, x, y, z, minSize, minSize, maxSize, maxSize, maxSize);

		renderblocks.setRenderBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);

		/*
		if (state.wireMatrix.hasWire(WireColor.Red)) {
			state.currentTexture = BuildCraftTransport.instance.wireIconProvider.getIcon(state.wireMatrix.getWireIconIndex(WireColor.Red));

			pipeWireRender(renderblocks, block, state, Utils.pipeMinPos, Utils.pipeMaxPos, Utils.pipeMinPos, IPipe.WireColor.Red, x, y, z);
		}

		if (state.wireMatrix.hasWire(WireColor.Blue)) {
			state.currentTexture = BuildCraftTransport.instance.wireIconProvider.getIcon(state.wireMatrix.getWireIconIndex(WireColor.Blue));
			pipeWireRender(renderblocks, block, state, Utils.pipeMaxPos, Utils.pipeMaxPos, Utils.pipeMaxPos, IPipe.WireColor.Blue, x, y, z);
		}

		if (state.wireMatrix.hasWire(WireColor.Green)) {
			state.currentTexture = BuildCraftTransport.instance.wireIconProvider.getIcon(state.wireMatrix.getWireIconIndex(WireColor.Green));
			pipeWireRender(renderblocks, block, state, Utils.pipeMaxPos, Utils.pipeMinPos, Utils.pipeMinPos, IPipe.WireColor.Green, x, y, z);
		}

		if (state.wireMatrix.hasWire(WireColor.Yellow)) {
			state.currentTexture = BuildCraftTransport.instance.wireIconProvider.getIcon(state.wireMatrix.getWireIconIndex(WireColor.Yellow));
			pipeWireRender(renderblocks, block, state, Utils.pipeMinPos, Utils.pipeMinPos, Utils.pipeMaxPos, IPipe.WireColor.Yellow, x, y, z);
		}

		if (state.hasGate()) {
			pipeGateRender(renderblocks, block, state, x, y, z);
		}

		pipeFacadeRenderer(renderblocks, block, state, x, y, z);
		pipePlugRenderer(renderblocks, block, state, x, y, z);
		*/
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		TileEntity tile = world.getBlockTileEntity(x, y, z);
		
		if (tile instanceof IPipeRenderState) {
			IPipeRenderState pipeTile = (IPipeRenderState) tile;
			renderPipe(renderer, world, (LogisticsBlockGenericPipe) block, pipeTile, x, y, z);
		}
		return true;
	}

	@Override
	public boolean shouldRender3DInInventory() {
		return false;
	}

	@Override
	public int getRenderId() {
		return MainProxy.proxy.getPipeRenderId();
	}
}
