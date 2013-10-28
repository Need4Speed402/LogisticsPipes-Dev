package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Icon;
import buildcraft.api.core.IIconProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LPPipeIconProvider implements IIconProvider {
	public ArrayList<Icon> icons = new ArrayList<Icon>();
	
	@Override
	public Icon getIcon(int iconIndex) {
		return icons.get(iconIndex);
	}
	
	public void setIcon(int index, Icon icon) {
		if(icon instanceof TextureAtlasSprite) {
			if(((TextureAtlasSprite)icon).getIconName().contains("block")) {
				System.out.println();
			}
		}
		while(icons.size() < index + 1) {
			icons.add(null);
		}
		icons.set(index, icon);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister) {}
}
