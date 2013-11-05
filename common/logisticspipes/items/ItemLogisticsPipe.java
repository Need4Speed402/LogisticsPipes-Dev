/**
 * Copyright (c) Krapht, 2011
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.items;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.renderer.LogisticsPipeItemRenderer;
import logisticspipes.textures.Textures;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A logistics pipe Item
 */
public class ItemLogisticsPipe extends LogisticsItem {
	
	public ItemLogisticsPipe(int key) {
		super(key);
		setCreativeTab(LogisticsPipes.LPCreativeTab);
	}

	private int pipeIconIndex;

	@Override
	public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int side, float par8, float par9, float par10) {
		int blockID = LogisticsPipes.LogisticsBlockGenericPipe.blockID;
		Block block = LogisticsPipes.LogisticsBlockGenericPipe;

		int id = world.getBlockId(i, j, k);

		if (id == Block.snow.blockID) {
			side = 1;
		} else if (id != Block.vine.blockID && id != Block.tallGrass.blockID && id != Block.deadBush.blockID && (Block.blocksList[id] == null || !Block.blocksList[id].isBlockReplaceable(world, i, j, k))) {
			if (side == 0) {
				j--;
			}
			if (side == 1) {
				j++;
			}
			if (side == 2) {
				k--;
			}
			if (side == 3) {
				k++;
			}
			if (side == 4) {
				i--;
			}
			if (side == 5) {
				i++;
			}
		}

		if (itemstack.stackSize == 0)
			return false;
		if (world.canPlaceEntityOnSide(blockID, i, j, k, false, side, entityplayer, itemstack)) {
			CoreRoutedPipe pipe = LogisticsBlockGenericPipe.createPipe(itemID);
			if (pipe == null) {
				LogisticsPipes.log.log(Level.WARNING, "Pipe failed to create during placement at {0},{1},{2}", new Object[]{i, j, k});
				return true;
			}
			if (LogisticsBlockGenericPipe.placePipe(pipe, world, i, j, k, blockID, 0)) {

				Block.blocksList[blockID].onBlockPlacedBy(world, i, j, k, entityplayer, itemstack);
				world.playSoundEffect(i + 0.5F, j + 0.5F, k + 0.5F, block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
				itemstack.stackSize--;
			}
			return true;
		} else
			return false;
	}

	public void setPipeIconIndex(int index) {
		this.pipeIconIndex = index;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Icon getIconFromDamage(int par1) {
		if (Textures.LPpipeIconProvider != null) { // invalid pipes won't have this set
			return Textures.LPpipeIconProvider.getIcon(pipeIconIndex);
		} else {
			return null;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getSpriteNumber() {
		return 0;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
			String baseKey = MessageFormat.format("{0}.tip", getUnlocalizedName());
			String key = baseKey + 1;
			String translation = Localization.get(key);
			int i = 1;
			while(translation != key) {
				list.add(translation);
				key = baseKey + ++i;
				translation = Localization.get(key);
			}
		}
	}
}
