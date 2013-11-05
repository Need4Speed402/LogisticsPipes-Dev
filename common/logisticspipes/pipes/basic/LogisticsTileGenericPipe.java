package logisticspipes.pipes.basic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;

import logisticspipes.LogisticsPipes;
import logisticspipes.asm.ModDependentField;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.cc.CCHelper;
import logisticspipes.proxy.cc.interfaces.CCCommand;
import logisticspipes.proxy.cc.interfaces.CCQueued;
import logisticspipes.proxy.cc.interfaces.CCType;
import logisticspipes.security.PermissionException;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.ticks.WorldTickHandler;
import logisticspipes.utils.AdjacentTile;
import logisticspipes.utils.Orientation;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;

@ModDependentInterface(modId={"ComputerCraft", "BuildCraft|Core", "BuildCraft|Transport"}, interfacePath={"dan200.computer.api.IPeripheral", "buildcraft.api.power.IPowerReceptor", "buildcraft.api.transport.IPipeTile"})
public class LogisticsTileGenericPipe extends TileEntity implements IPeripheral, IPowerReceptor, IFluidHandler, IPipeTile {

	public boolean turtleConnect[] = new boolean[7];
	
	@ModDependentField(modId="ComputerCraft")
	public HashMap<IComputerAccess, ForgeDirection> connections;
	
	private boolean init = false;
	private HashMap<Integer, String> commandMap = new HashMap<Integer, String>();
	private Map<Integer, Method> commands = new LinkedHashMap<Integer, Method>();
	private String typeName = "";

	@ModDependentField(modId="ComputerCraft")
	public IComputerAccess lastPC;
	
	public LogisticsTileGenericPipe() {
		if(SimpleServiceLocator.ccProxy.isCC()) {
			connections = new HashMap<IComputerAccess, ForgeDirection>();
		}
	}
	
	protected CoreRoutedPipe getCPipe() {
		if(pipe instanceof CoreRoutedPipe) {
			return (CoreRoutedPipe) pipe;
		}
		return null;
	}

	@Override
	public void invalidate() {
		if(!getCPipe().blockRemove()) {
			this.tileEntityInvalid = true;
			initialized = false;
			if (pipe != null)
				pipe.invalidate();
			super.invalidate();
		}
	}
	
	@Override
	public void func_85027_a(CrashReportCategory par1CrashReportCategory) {
		super.func_85027_a(par1CrashReportCategory);
		par1CrashReportCategory.addCrashSection("LP-Version", LogisticsPipes.VERSION);
		if(this.pipe != null) {
			par1CrashReportCategory.addCrashSection("Pipe", this.pipe.getClass().getCanonicalName());
			if(this.pipe.transport != null) {
				par1CrashReportCategory.addCrashSection("Transport", this.pipe.transport.getClass().getCanonicalName());
			} else {
				par1CrashReportCategory.addCrashSection("Transport", "null");
			}

			if(this.pipe instanceof CoreRoutedPipe) {
				try {
					((CoreRoutedPipe)this.pipe).addCrashReport(par1CrashReportCategory);
				} catch(Exception e) {
					par1CrashReportCategory.addCrashSectionThrowable("Internal LogisticsPipes Error", e);
				}
			}
		}
	}

	private CCType getType(Class<?> clazz) {
		while(true) {
			CCType type = clazz.getAnnotation(CCType.class);
			if(type != null) return type;
			if(clazz.getSuperclass() == Object.class) return null;
			clazz = clazz.getSuperclass();
		}
	}
	
	private void init() {
		if(!init) {
			init = true;
			CoreRoutedPipe pipe = getCPipe();
			if(pipe == null) return;
			CCType type = getType(pipe.getClass());
			if(type == null) return;
			typeName = type.name();
			int i = 0;
			Class<?> clazz = pipe.getClass();
			while(true) {
				for(Method method:clazz.getDeclaredMethods()) {
					if(!method.isAnnotationPresent(CCCommand.class)) continue;
					for(Class<?> param:method.getParameterTypes()) {
						if(!param.getName().startsWith("java")) {
							throw new InternalError("Internal Excption (Code: 2)");
						}
					}
					commandMap.put(i, method.getName());
					commands.put(i, method);
					i++;
				}
				if(clazz.getSuperclass() == Object.class) break;
				clazz = clazz.getSuperclass();
			}
		}
	}
	
	private boolean argumentsMatch(Method method, Object[] arguments) {
		Class<?> args[] = method.getParameterTypes();
		if(arguments.length != args.length) return false;
		for(int i=0; i<arguments.length; i++) {
			if(!arguments[i].getClass().equals(args[i])) return false;
		}
		return true;
	}

	/**
	 * Checks if this tile can connect to another tile
	 *
	 * @param with - The other Tile
	 * @param side - The orientation to get to the other tile ('with')
	 * @return true if pipes are considered connected
	 */
	public boolean canPipeConnect(TileEntity with, ForgeDirection side) {
		if(SimpleServiceLocator.ccProxy.isTurtle(with) && !turtleConnect[OrientationsUtil.getOrientationOfTilewithTile(this, with).ordinal()]) return false;
		if (with == null)
			return false;

		if (hasPlug(side))
			return false;

		if (!LogisticsBlockGenericPipe.isValid(pipe))
			return false;

		//TODO Proxy Implementation
		if (with instanceof LogisticsTileGenericPipe) {
			if (((LogisticsTileGenericPipe) with).hasPlug(side.getOpposite()))
				return false;
			CoreRoutedPipe otherPipe = ((LogisticsTileGenericPipe) with).pipe;

			if (!LogisticsBlockGenericPipe.isValid(otherPipe))
				return false;

			if (!otherPipe.canPipeConnect(this, side.getOpposite()))
				return false;
		}

		return pipe.canPipeConnect(with, side);
	}
	
	@Override
	@ModDependentMethod(modId="ComputerCraft")
	public String getType() {
		init();
		return typeName;
	}
	
	@Override
	@ModDependentMethod(modId="ComputerCraft")
	public String[] getMethodNames() {
		init();
		LinkedList<String> list = new LinkedList<String>();
		list.add("help");
		list.add("commandHelp");
		list.add("getType");
		for(int i=0;i<commandMap.size();i++) {
			list.add(commandMap.get(i));
		}
		return list.toArray(new String[list.size()]);
	}

	@Override
	@ModDependentMethod(modId="ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int methodId, Object[] arguments) throws Exception {
		if(getCPipe() == null) throw new InternalError("Pipe is not a LogisticsPipe");
		init();
		lastPC = computer;
		if(methodId == 0) {
			StringBuilder help = new StringBuilder();
			StringBuilder head = new StringBuilder();
			StringBuilder head2 = new StringBuilder();
			head.append("PipeType: ");
			head.append(typeName);
			head.append("\n");
			head2.append("Commands: \n");
			for(Integer num:commands.keySet()) {
				Method method = commands.get(num);
				StringBuilder command = new StringBuilder();
				if(help.length() != 0) {
					command.append("\n");
				}
				int number = num.intValue();
				if(number < 10) {
					command.append(" ");
				}
				command.append(number);
				if(method.isAnnotationPresent(CCQueued.class)) {
					command.append(" Q");
				} else {
					command.append("  ");
				}
				command.append(": ");
				command.append(method.getName());
				StringBuilder param = new StringBuilder();
				param.append("(");
				boolean a = false;
				for(Class<?> clazz:method.getParameterTypes()) {
					if(a) {
						param.append(", ");
					}
					param.append(clazz.getSimpleName());
					a = true;
				}
				param.append(")");
				if(param.toString().length() + command.length() > 36) {
					command.append("\n      ---");
				}
				command.append(param.toString());
				help.append(command.toString());
			}
			String commands = help.toString();
			String[] lines = commands.split("\n");
			if(lines.length > 16) {
				int pageNumber = 1;
				if(arguments.length > 0) {
					if(arguments[0] instanceof Double) {
						pageNumber = (int) Math.floor((Double)arguments[0]);
						if(pageNumber < 1) {
							pageNumber = 1;
						}
					}
				}
				StringBuilder page = new StringBuilder();
				page.append(head.toString());
				page.append("Page ");
				page.append(pageNumber);
				page.append(" of ");
				page.append((int)(Math.floor(lines.length / 10) + (lines.length % 10 == 0 ? 0:1)));
				page.append("\n");
				page.append(head2.toString());
				pageNumber--;
				int from = pageNumber * 11;
				int to = pageNumber * 11 + 10;
				for(int i=from;i<to;i++) {
					if(i < lines.length) {
						page.append(lines[i]);
					}
					if(i < to - 1) {
						page.append("\n");
					}
				}
				return new Object[]{page.toString()};
			} else {
				for(int i=0;i<16-lines.length;i++) {
					String buffer = head.toString();
					head = new StringBuilder();
					head.append("\n").append(buffer);
				}
			}
			return new Object[]{new StringBuilder().append(head).append(head2).append(help).toString()};
		}
		methodId--;
		if(methodId == 0) {
			if(arguments.length != 1) return new Object[]{"Wrong Argument Count"};
			if(!(arguments[0] instanceof Double)) return new Object[]{"Wrong Argument Type"};
			Integer number = (int) Math.floor(((Double)arguments[0]));
			if(!commands.containsKey(number)) return new Object[]{"No command with that index"};
			Method method = commands.get(number);
			StringBuilder help = new StringBuilder();
			help.append("---------------------------------\n");
			help.append("Command: ");
			help.append(method.getName());
			help.append("\n");
			help.append("Parameter: ");
			if(method.getParameterTypes().length > 0) {
				help.append("\n");
				boolean a = false;
				for(Class<?> clazz:method.getParameterTypes()) {
					if(a) {
						help.append(", ");
					}
					help.append(clazz.getSimpleName());
					a = true;
				}
				help.append("\n");
			} else {
				help.append("NONE\n");
			}
			help.append("Return Type: ");
			help.append(method.getReturnType().getName());
			help.append("\n");
			help.append("Description: \n");
			help.append(method.getAnnotation(CCCommand.class).description());
			return new Object[]{help.toString()};
		}

		methodId--;
		if(methodId == 0) {
			return CCHelper.createArray(CCHelper.getAnswer(getType()));
		}
		methodId--;
		String name = commandMap.get(methodId);
		
		Method match = null;
		
		for(Method method:commands.values()) {
			if(!method.getName().equalsIgnoreCase(name)) continue;
			if(!argumentsMatch(method, arguments)) continue;
			match = method;
			break;
		}
		
		if(match == null) {
			StringBuilder error = new StringBuilder();
			error.append("No such method.");
			boolean handled = false;
			for(Method method:commands.values()) {
				if(!method.getName().equalsIgnoreCase(name)) continue;
				if(handled) {
					error.append("\n");
				}
				handled = true;
				error.append(method.getName());
				error.append("(");
				boolean a = false;
				for(Class<?> clazz:method.getParameterTypes()) {
					if(a) {
						error.append(", ");
					}
					error.append(clazz.getName());
					a = true;
				}
				error.append(")");
			}
			if(!handled) {
				error = new StringBuilder();
				error.append("Internal Excption (Code: 1, ");
				error.append(name);
				error.append(")");
			}
			throw new UnsupportedOperationException(error.toString());
		}
		
		if(match.getAnnotation(CCCommand.class).needPermission()) {
			getCPipe().checkCCAccess();
		}
		
		if(match.getAnnotation(CCQueued.class) != null) {
			final Method m = match;
			String prefunction = null;
			if(!(prefunction = match.getAnnotation(CCQueued.class).prefunction()).equals("")) {
				//CoreRoutedPipe pipe = getCPipe();
				if(pipe != null) {
					Class<?> clazz = pipe.getClass();
					while(true) {
						for(Method method:clazz.getDeclaredMethods()) {
							if(method.getName().equals(prefunction)) {
								if(method.getParameterTypes().length > 0) {
									throw new InternalError("Internal Excption (Code: 3)");
								}
								try {
									method.invoke(pipe, new Object[]{});
								} catch(InvocationTargetException e) {
									if(e.getTargetException() instanceof Exception) {
										throw (Exception) e.getTargetException();
									}
									throw e;
								}
								break;
							}
						}
						if(clazz.getSuperclass() == Object.class) break;
						clazz = clazz.getSuperclass();
					}
				}
			}
			final Object[] a = arguments;
			final Object[] resultArray = new Object[1];
			final Boolean[] booleans = new Boolean[2];
			booleans[0] = false;
			booleans[1] = false;
			QueuedTasks.queueTask(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					try {
						Object result = m.invoke(pipe, a);
						if(result != null) {
							resultArray[0] = result;
						}
					} catch (InvocationTargetException e) {
						if(e.getTargetException() instanceof PermissionException) {
							booleans[1] = true;
							resultArray[0] = e.getTargetException();
						} else {
							booleans[0] = true;
							throw e;
						}
					}
					booleans[0] = true;
					return null;
				}
			});
			int count = 0;
			while(!booleans[0] && count < 200) {
				Thread.sleep(10);
				count++;
			}
			if(count >= 199) {
				CoreRoutedPipe pipe = getCPipe();
				LogisticsPipes.log.warning("CC call " + m.getName() + " on " + pipe.getClass().getName() + " at (" + this.xCoord + "," + this.yCoord + "," + this.zCoord + ") took too long.");
				throw new Exception("Took too long");
			}
			if(m.getReturnType().equals(Void.class)) {
				return null;
			}
			if(booleans[1]) {
				//PermissionException
				throw ((Exception)resultArray[0]);
			}
			return CCHelper.createArray(CCHelper.getAnswer(resultArray[0]));
		}
		Object result;
		try {
			result = match.invoke(pipe, arguments);
		} catch(InvocationTargetException e) {
			if(e.getTargetException() instanceof Exception) {
				throw (Exception) e.getTargetException();
			}
			throw e;
		}
		return CCHelper.createArray(CCHelper.getAnswer(result));
	}

	public void scheduleNeighborChange() {
		blockNeighborChange = true;
		boolean connected[] = new boolean[6];
		WorldUtil world = new WorldUtil(this.getWorldObj(), this.xCoord, this.yCoord, this.zCoord);
		LinkedList<AdjacentTile> adjacent = world.getAdjacentTileEntities(false);
		for(AdjacentTile aTile: adjacent) {
			if(SimpleServiceLocator.ccProxy.isTurtle(aTile.tile)) {
				connected[aTile.orientation.ordinal()] = true;
			}
		}
		for(int i=0; i<6;i++) {
			if(!connected[i]) {
				turtleConnect[i] = false;
			}
		}
	}

	@Override
	public void updateEntity() {
		if(MainProxy.isServer(getWorldObj())) {
			if(getWorldObj().getBlockId(xCoord, yCoord, zCoord) != LogisticsPipes.LogisticsBlockGenericPipe.blockID) {
				WorldTickHandler.serverPipesToReplace.add(this);
				return;
			}
		}
		if (!worldObj.isRemote) {
			if (deletePipe)
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);

			if (pipe == null)
				return;

			if (!initialized)
				initialize(pipe);
		}

		if (!LogisticsBlockGenericPipe.isValid(pipe))
			return;

		pipe.updateEntity();

		if (worldObj.isRemote)
			return;

		if (blockNeighborChange) {
			computeConnections();
			pipe.onNeighborBlockChange(0);
			blockNeighborChange = false;
			refreshRenderState = true;
		}

		if (refreshRenderState) {
			//TODO
			refreshRenderState = false;
		}

		PowerReceiver provider = getPowerReceiver(null);
		if (provider != null)
			provider.update();

		if (sendClientUpdate) {
			sendClientUpdate = false;
			if (worldObj instanceof WorldServer) {
				WorldServer world = (WorldServer) worldObj;
				PlayerInstance playerInstance = world.getPlayerManager().getOrCreateChunkWatcher(xCoord >> 4, zCoord >> 4, false);
				if (playerInstance != null) {
					playerInstance.sendToAllPlayersWatchingChunk(getDescriptionPacket());
				}
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		if (pipe != null) {
			nbt.setInteger("pipeId", pipe.itemID);
			pipe.writeToNBT(nbt);
		} else
			nbt.setInteger("pipeId", coreState.pipeId);

		for (int i = 0; i < ForgeDirection.VALID_DIRECTIONS.length; i++) {
			nbt.setInteger("facadeBlocks[" + i + "]", facadeBlocks[i]);
			nbt.setInteger("facadeMeta[" + i + "]", facadeMeta[i]);
			nbt.setBoolean("plug[" + i + "]", plugs[i]);
		}
		for(int i=0;i<turtleConnect.length;i++) {
			nbt.setBoolean("turtleConnect_" + i, turtleConnect[i]);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		coreState.pipeId = nbt.getInteger("pipeId");
		pipe = LogisticsBlockGenericPipe.createPipe(coreState.pipeId);

		if (pipe != null)
			pipe.readFromNBT(nbt);
		else {
			BCLog.logger.log(Level.WARNING, "Pipe failed to load from NBT at {0},{1},{2}", new Object[]{xCoord, yCoord, zCoord});
			deletePipe = true;
		}

		for (int i = 0; i < ForgeDirection.VALID_DIRECTIONS.length; i++) {
			facadeBlocks[i] = nbt.getInteger("facadeBlocks[" + i + "]");
			facadeMeta[i] = nbt.getInteger("facadeMeta[" + i + "]");
			plugs[i] = nbt.getBoolean("plug[" + i + "]");
		}
		for(int i=0;i<turtleConnect.length;i++) {
			turtleConnect[i] = nbt.getBoolean("turtleConnect_" + i);
		}
		int pipeId = nbt.getInteger("pipeId");
		pipe = LogisticsBlockGenericPipe.createPipe(pipeId);
	}

	@Override
	@ModDependentMethod(modId="ComputerCraft")
	public boolean canAttachToSide(int side) {
		//All Sides are valid
		return true;
	}

	@Override
	@ModDependentMethod(modId="ComputerCraft")
	public void attach(IComputerAccess computer) {
		ForgeDirection ori = SimpleServiceLocator.ccProxy.getOrientation(computer, this);
		connections.put(computer, ori);
		this.scheduleNeighborChange();
	}

	@Override
	@ModDependentMethod(modId="ComputerCraft")
	public void detach(IComputerAccess computer) {
		connections.remove(computer);
	}
	
	public void queueEvent(String event, Object[] arguments) {
		SimpleServiceLocator.ccProxy.queueEvent(event, arguments, this);
	}
	
	public void setTurtleConnect(boolean flag) {
		SimpleServiceLocator.ccProxy.setTurtleConnect(flag, this);
	}

	public boolean getTurtleConnect() {
		return SimpleServiceLocator.ccProxy.getTurtleConnect(this);
	}

	public int getLastCCID() {
		return SimpleServiceLocator.ccProxy.getLastCCID(this);
	}
	
	private boolean deletePipe = false;
	public boolean[] pipeConnectionsBuffer = new boolean[6];
	public CoreRoutedPipe pipe;
	private boolean sendClientUpdate = false;
	private boolean blockNeighborChange = false;
	private boolean refreshRenderState = false;
	private int[] facadeBlocks = new int[ForgeDirection.VALID_DIRECTIONS.length];
	private int[] facadeMeta = new int[ForgeDirection.VALID_DIRECTIONS.length];
	private boolean[] plugs = new boolean[ForgeDirection.VALID_DIRECTIONS.length];

	@Override
	public void validate() {
		super.validate();
		initialized = false;
		bindPipe();
		if (pipe != null)
			pipe.validate();
	}
	public boolean initialized = false;

	@SideOnly(Side.CLIENT)
	public Icon	currentTexture;

	public void initialize(CoreRoutedPipe pipe) {

		this.blockType = getBlockType();

		if (pipe == null) {
			LogisticsPipes.log.log(Level.WARNING, "Pipe failed to initialize at {0},{1},{2}, deleting", new Object[]{xCoord, yCoord, zCoord});
			worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			return;
		}

		this.pipe = pipe;

		for (ForgeDirection o : ForgeDirection.VALID_DIRECTIONS) {
			TileEntity tile = getTile(o);

			//TODO Implement Proxys
			/*if (tile instanceof ITileBufferHolder)
				((ITileBufferHolder) tile).blockCreated(o, BuildCraftTransport.genericPipeBlock.blockID, this);
			if (tile instanceof TileGenericPipe)
				((TileGenericPipe) tile).scheduleNeighborChange();
				*/
		}

		bindPipe();

		computeConnections();
		scheduleRenderUpdate();

		if (pipe.needsInit())
			pipe.initialize();

		initialized = true;
	}

	private void bindPipe() {
		if (!pipeBound && pipe != null) {

			pipe.setTile(this);

			coreState.pipeId = pipe.itemID;
			pipeBound = true;
		}
	}

	@Override
	@ModDependentMethod(modId = "BuildCraft|Transport")
	public IPipe getPipe() {
		new UnsupportedOperationException().printStackTrace();
		return pipe;
	}

	public boolean isInitialized() {
		return initialized;
	}

	@Override
	@ModDependentMethod(modId = "BuildCraft|Core")
	public PowerHandler.PowerReceiver getPowerReceiver(ForgeDirection side) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe instanceof IPowerReceptor)
			return ((IPowerReceptor) pipe).getPowerReceiver(null);
		else
			return null;
	}

	@Override
	@ModDependentMethod(modId = "BuildCraft|Core")
	public void doWork(PowerHandler workProvider) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe instanceof IPowerReceptor)
			((IPowerReceptor) pipe).doWork(workProvider);
	}

	/* IPIPEENTRY */
	@Override
	public int injectItem(ItemStack payload, boolean doAdd, ForgeDirection from) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof PipeTransportItems && isPipeConnected(from)) {
			if (doAdd) {
				Orientation itemPos = new Orientation(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, from.getOpposite());
				itemPos.moveBackwards(0.4);

				TravelingItem pipedItem = new TravelingItem(itemPos.x, itemPos.y, itemPos.z, payload);
				((PipeTransportItems) pipe.transport).injectItem(pipedItem, itemPos.orientation);
			}
			return payload.stackSize;
		}
		return 0;
	}

	@Override
	public PipeType getPipeType() {
		if (LogisticsBlockGenericPipe.isValid(pipe))
			return pipe.transport.getPipeType();
		return null;
	}

	/* SMP */
	@Override
	public Packet getDescriptionPacket() {
		//TODO generate General Information
		// Get rid of client side triggered Update requests
		bindPipe();

		PacketTileState packet = new PacketTileState(this.xCoord, this.yCoord, this.zCoord);
		if (pipe != null && pipe.gate != null)
			coreState.gateKind = pipe.gate.kind.ordinal();
		else
			coreState.gateKind = 0;

		if (pipe != null && pipe.transport != null)
			pipe.transport.sendDescriptionPacket();

		packet.addStateForSerialization((byte) 0, coreState);
		packet.addStateForSerialization((byte) 1, renderState);
		if (pipe instanceof IClientState)
			packet.addStateForSerialization((byte) 2, (IClientState) pipe);
		return packet.getPacket();
	}

	public void sendUpdateToClient() {
		sendClientUpdate = true;
	}

	@Override
	public LinkedList<ITrigger> getTriggers() {
		//TODO Implement own Gate trigger version
		LinkedList<ITrigger> result = new LinkedList<ITrigger>();

		if (LogisticsBlockGenericPipe.isFullyDefined(pipe) && pipe.hasGate()) {
			result.add(BuildCraftCore.triggerRedstoneActive);
			result.add(BuildCraftCore.triggerRedstoneInactive);
		}

		return result;
	}

	public TileBuffer[] getTileCache() {
		if (tileBuffer == null && pipe != null)
			tileBuffer = TileBuffer.makeBuffer(worldObj, xCoord, yCoord, zCoord, pipe.transport.delveIntoUnloadedChunks());
		return tileBuffer;
	}

	@Override
	public void blockCreated(ForgeDirection from, int blockID, TileEntity tile) {
		TileBuffer[] cache = getTileCache();
		if (cache != null)
			cache[from.getOpposite().ordinal()].set(blockID, tile);
	}

	@Override
	public int getBlockId(ForgeDirection to) {
		TileBuffer[] cache = getTileCache();
		if (cache != null)
			return cache[to.ordinal()].getBlockID();
		else
			return 0;
	}

	@Override
	public TileEntity getTile(ForgeDirection to) {
		TileBuffer[] cache = getTileCache();
		if (cache != null)
			return cache[to.ordinal()].getTile();
		else
			return null;
	}

	private void computeConnections() {
		TileBuffer[] cache = getTileCache();
		if (cache == null)
			return;

		for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
			TileBuffer t = cache[side.ordinal()];
			t.refresh();

			pipeConnectionsBuffer[side.ordinal()] = canPipeConnect(t.getTile(), side);
		}
	}

	@Override
	public boolean isPipeConnected(ForgeDirection with) {
		if (worldObj.isRemote)
			return renderState.pipeConnectionMatrix.isConnected(with);
		return pipeConnectionsBuffer[with.ordinal()];
	}

	@Override
	public void onChunkUnload() {
		if (pipe != null)
			pipe.onChunkUnload();
	}

	/**
	 * ITankContainer implementation *
	 */
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasPlug(from))
			return ((IFluidHandler) pipe.transport).fill(from, resource, doFill);
		else
			return 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasPlug(from))
			return ((IFluidHandler) pipe.transport).drain(from, maxDrain, doDrain);
		else
			return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasPlug(from))
			return ((IFluidHandler) pipe.transport).drain(from, resource, doDrain);
		else
			return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasPlug(from))
			return ((IFluidHandler) pipe.transport).canFill(from, fluid);
		else
			return false;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasPlug(from))
			return ((IFluidHandler) pipe.transport).canDrain(from, fluid);
		else
			return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return null;
	}

	public void scheduleRenderUpdate() {
		refreshRenderState = true;
	}

	public boolean addFacade(ForgeDirection direction, int blockid, int meta) {
		if (this.worldObj.isRemote)
			return false;
		if (this.facadeBlocks[direction.ordinal()] == blockid)
			return false;

		if (hasFacade(direction))
			dropFacadeItem(direction);

		this.facadeBlocks[direction.ordinal()] = blockid;
		this.facadeMeta[direction.ordinal()] = meta;
		worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, getBlockId());
		scheduleRenderUpdate();
		return true;
	}

	public boolean hasFacade(ForgeDirection direction) {
		if (direction == null || direction == ForgeDirection.UNKNOWN)
			return false;
		if (this.worldObj.isRemote)
			return renderState.facadeMatrix.getFacadeBlockId(direction) != 0;
		return (this.facadeBlocks[direction.ordinal()] != 0);
	}

	private void dropFacadeItem(ForgeDirection direction) {
		InvUtils.dropItems(worldObj, ItemFacade.getStack(this.facadeBlocks[direction.ordinal()], this.facadeMeta[direction.ordinal()]), this.xCoord, this.yCoord, this.zCoord);
	}

	public boolean dropFacade(ForgeDirection direction) {
		if (!hasFacade(direction))
			return false;
		if (!worldObj.isRemote) {
			dropFacadeItem(direction);
			this.facadeBlocks[direction.ordinal()] = 0;
			this.facadeMeta[direction.ordinal()] = 0;
			worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, getBlockId());
			scheduleRenderUpdate();
		}
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return DefaultProps.PIPE_CONTENTS_RENDER_DIST * DefaultProps.PIPE_CONTENTS_RENDER_DIST;
	}

	@Override
	public boolean shouldRefresh(int oldID, int newID, int oldMeta, int newMeta, World world, int x, int y, int z) {
		return oldID != newID;
	}

	@Override
	public boolean isSolidOnSide(ForgeDirection side) {
		if (hasFacade(side))
			return true;

		if (LogisticsBlockGenericPipe.isValid(pipe) && pipe instanceof ISolidSideTile)
			if (((ISolidSideTile) pipe).isSolidOnSide(side))
				return true;
		return false;
	}

	public boolean hasPlug(ForgeDirection side) {
		if (side == null || side == ForgeDirection.UNKNOWN)
			return false;
		if (this.worldObj.isRemote)
			return renderState.plugMatrix.isConnected(side);
		return plugs[side.ordinal()];
	}

	public boolean removeAndDropPlug(ForgeDirection side) {
		if (!hasPlug(side))
			return false;
		if (!worldObj.isRemote) {
			plugs[side.ordinal()] = false;
			InvUtils.dropItems(worldObj, new ItemStack(BuildCraftTransport.plugItem), this.xCoord, this.yCoord, this.zCoord);
			worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, getBlockId());
			scheduleNeighborChange(); //To force recalculation of connections
			scheduleRenderUpdate();
		}
		return true;
	}

	public boolean addPlug(ForgeDirection forgeDirection) {
		if (hasPlug(forgeDirection))
			return false;

		plugs[forgeDirection.ordinal()] = true;
		worldObj.notifyBlockChange(this.xCoord, this.yCoord, this.zCoord, getBlockId());
		scheduleNeighborChange(); //To force recalculation of connections
		scheduleRenderUpdate();
		return true;
	}

	public int getBlockId() {
		Block block = getBlockType();
		if (block != null)
			return block.blockID;
		return 0;
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this;
	}

	public void doDrop() {
		if(pipe != null) {
			pipe.doDrop();
		}
	}

	@Override
	public World getWorld() {
		return this.getWorldObj();
	}
}
