package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Icon;

public class LPPipeIconProvider {
	public ArrayList<Icon> icons = new ArrayList<Icon>();
	
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
}
