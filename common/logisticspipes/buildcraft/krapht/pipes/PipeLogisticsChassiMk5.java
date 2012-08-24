package logisticspipes.buildcraft.krapht.pipes;

import logisticspipes.mod_LogisticsPipes;

public class PipeLogisticsChassiMk5 extends PipeLogisticsChassi{

	public PipeLogisticsChassiMk5(int itemID) {
		super(itemID);
	}

	@Override
	public int getCenterTexture() {
		return mod_LogisticsPipes.LOGISTICSPIPE_CHASSI5_TEXTURE;
	}

	@Override
	public int getChassiSize() {
		return 8;
	}


}