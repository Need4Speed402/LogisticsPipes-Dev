/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.interfaces.ISpecialInsertion;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.upgrades.UpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.RoutedEntityItem;
import logisticspipes.utils.InventoryHelper;
import logisticspipes.utils.ItemIdentifierStack;
import logisticspipes.utils.SidedInventoryMinecraftAdapter;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.BuildCraftTransport;
import buildcraft.api.core.Position;
import buildcraft.api.gates.ITrigger;
import buildcraft.api.inventory.ISpecialInventory;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.IPipeTile.PipeType;
import buildcraft.core.CoreConstants;
import buildcraft.core.DefaultProps;
import buildcraft.core.IMachine;
import buildcraft.core.inventory.Transactor;
import buildcraft.core.network.PacketIds;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.core.utils.BCLog;
import buildcraft.core.utils.BlockUtil;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.IItemTravelingHook;
import buildcraft.transport.IPipeTransportItemsHook;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.TransportConstants;
import buildcraft.transport.TravelingItem;
import buildcraft.transport.PipeTransportItems.TravelerSet;
import buildcraft.transport.network.PacketPipeTransportContent;
import buildcraft.transport.network.PacketPipeTransportNBT;
import buildcraft.transport.network.PacketSimpleId;
import buildcraft.transport.utils.TransportUtils;

public class PipeTransportLogistics {

	private final int _bufferTimeOut = 20 * 2; //2 Seconds
	private final HashMap<ItemStack,Pair<Integer /* Time */, Integer /* BufferCounter */>> _itemBuffer = new HashMap<ItemStack, Pair<Integer, Integer>>(); 

	public PipeTransportLogistics() {
		allowBouncing = true;
	}

	private CoreRoutedPipe getPipe() {
		return (CoreRoutedPipe)container.pipe;
	}
	
	public void updateEntity() {
		moveSolids();
		if (!_itemBuffer.isEmpty()){
			List<IRoutedItem> toAdd = new LinkedList<IRoutedItem>();
			Iterator<Entry<ItemStack, Pair<Integer, Integer>>> iterator = _itemBuffer.entrySet().iterator();
			while (iterator.hasNext()){
				Entry<ItemStack, Pair<Integer, Integer>> next = iterator.next();
				int currentTimeOut = next.getValue().getValue1();
				if (currentTimeOut > 0){
					next.getValue().setValue1(currentTimeOut - 1);
				} else {
					TravelingItem item = new TravelingItem(this.getPipe().getX()+ 0.5F, this.getPipe().getY() + CoreConstants.PIPE_MIN_POS - 0.1, this.getPipe().getZ() + 0.5, next.getKey());
					IRoutedItem routedItem = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(item);
					routedItem.setDoNotBuffer(true);
					routedItem.setBufferCounter(next.getValue().getValue2() + 1);
					toAdd.add(routedItem);
					iterator.remove();
				}
			}
			for(IRoutedItem item:toAdd) {
				this.injectItem(item.getTravelingItem(), ForgeDirection.UP);
			}
		}
	}
	
	public void dropBuffer(){
		Iterator<ItemStack> iterator = _itemBuffer.keySet().iterator();
		while (iterator.hasNext()){
			ItemStack next = iterator.next();
			SimpleServiceLocator.buildCraftProxy.dropItems(getWorld(), next, this.getPipe().getX(), this.getPipe().getY(), this.getPipe().getZ());
			iterator.remove();
		}
	}
	
	@Override
	public ForgeDirection resolveDestination(TravelingItem data) {
		
		if(data != null && data.getItemStack() != null) {
			getPipe().relayedItem(data.getItemStack().stackSize);
		if(this.container.pipe instanceof PipeBlockRequestTable) {
			System.out.print("");
		}
		}
		
		ForgeDirection blocked = null;
		
		if(!(data instanceof IRoutedItem) && data != null) {
			TravelingItem result = getPipe().getQueuedForItemStack(data.getItemStack());
			if(result != null) {
				IRoutedItem routedItem = SimpleServiceLocator.buildCraftProxy.GetOrCreateRoutedItem(data);
				if(routedItem instanceof RoutedEntityItem && result instanceof RoutedEntityItem) {
					((RoutedEntityItem)routedItem).useInformationFrom((RoutedEntityItem)result);
					blocked = data.input.getOpposite();
				} else {
					LogisticsPipes.log.warning("Unable to transfer information from ont Item to another. (" + routedItem.getClass().getName() + ", " + result.getClass().getName() + ")");
				}
			}
		}
		
		IRoutedItem routedItem = SimpleServiceLocator.buildCraftProxy.GetOrCreateRoutedItem(data);
		ForgeDirection value;
		value = getPipe().getRouteLayer().getOrientationForItem(routedItem, blocked);
		if (value == null && MainProxy.isClient(getWorld())) {
			routedItem.getItemStack().stackSize = 0;
			items.scheduleRemoval(data);
			return ForgeDirection.UNKNOWN;
		} else if (value == null) {
			LogisticsPipes.log.severe("THIS IS NOT SUPPOSED TO HAPPEN!");
			return ForgeDirection.UNKNOWN;
		}
		if (value == ForgeDirection.UNKNOWN && !routedItem.getDoNotBuffer() && routedItem.getBufferCounter() < 5) {
			_itemBuffer.put(routedItem.getItemStack().copy(), new Pair<Integer,Integer>(20 * 2, routedItem.getBufferCounter()));
			routedItem.getItemStack().stackSize = 0;	//Hack to make the item disappear
			items.scheduleRemoval(data);
			return ForgeDirection.UNKNOWN;
		}
		
		if(value != ForgeDirection.UNKNOWN && !getPipe().getRouter().isRoutedExit(value)) {
			if(!isItemExitable(routedItem.getItemStack())) {
				routedItem.getItemStack().stackSize = 0;	//Hack to make the item disappear
				items.scheduleRemoval(data);
				return ForgeDirection.UNKNOWN;
			}
		}
		
		readjustSpeed(routedItem.getTravelingItem());
		
		return value;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);


		_itemBuffer.clear();
		
        NBTTagList nbttaglist = nbttagcompound.getTagList("buffercontents");
        for(int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = (NBTTagCompound)nbttaglist.tagAt(i);
            _itemBuffer.put(ItemStack.loadItemStackFromNBT(nbttagcompound1), new Pair<Integer, Integer>(_bufferTimeOut, 0));
        }
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);

		NBTTagList nbttaglist = new NBTTagList();
        //ItemStack[] offspring = spawn.toArray(new ItemStack[spawn.size()]);

		for (ItemStack stack : _itemBuffer.keySet()){
			NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            stack.writeToNBT(nbttagcompound1);
            nbttaglist.appendTag(nbttagcompound1);
		}
        nbttagcompound.setTag("buffercontents", nbttaglist);
		
	}
	
	public void readjustSpeed(TravelingItem item) {	
		if (SimpleServiceLocator.buildCraftProxy.isRoutedItem(item)){
			
			IRoutedItem routedItem = SimpleServiceLocator.buildCraftProxy.GetRoutedItem(item); 
			float defaultBoost = 1F;
			
			switch (routedItem.getTransportMode()){
			case Default:
				defaultBoost = 10F;
				break;
			case Passive:
				defaultBoost = 20F;
				break;
			case Active:
				defaultBoost = 30F;
				break;
			case Unknown:
				break;
			default:
				break;
			
			}

			float multiplyerSpeed = 1.0F + (0.2F * getPipe().getUpgradeManager().getSpeedUpgradeCount());
			float multiplyerPower = 1.0F + (0.3F * getPipe().getUpgradeManager().getSpeedUpgradeCount());
			
			float add = Math.max(item.getSpeed(), TransportConstants.PIPE_NORMAL_SPEED * defaultBoost * multiplyerPower) - item.getSpeed();
			if(getPipe().useEnergy((int)(add * 25+0.5))) {
				item.setSpeed(Math.min(Math.max(item.getSpeed(), TransportConstants.PIPE_NORMAL_SPEED * defaultBoost * multiplyerSpeed), 1.0F));
			}
		}
		if (MainProxy.isClient(getWorld())) {
			MainProxy.spawnParticle(Particles.GoldParticle, getPipe().getX(), getPipe().getY(), getPipe().getZ(), 1);
		}
	}
	
	//called from endReached, return false to let BC transport handle the item.
	protected boolean handleTileReached(TravelingItem arrivingItem, TileEntity tile) {
		//((PipeTransportLogistics)pipe).markChunkModified(tile);
		if (MainProxy.isServer(getWorld()) && (arrivingItem instanceof RoutedEntityItem) && ((RoutedEntityItem)arrivingItem).getArrived()) {
			getPipe().notifyOfItemArival((RoutedEntityItem) arrivingItem);
		}
		boolean isSpecialConnectionInformationTransition = false;
		if (!CoreProxy.proxy.isRenderWorld(getWorld())) {
			if(SimpleServiceLocator.specialtileconnection.needsInformationTransition(tile)) {
				isSpecialConnectionInformationTransition = true;
				SimpleServiceLocator.specialtileconnection.transmit(tile, arrivingItem);
			}
		}
		if (tile instanceof IPipeTile){
			return false; // let the normal BC pipe passing mechanism run
		} else if (tile instanceof IInventory) {
			items.scheduleRemoval(arrivingItem);
			if (!CoreProxy.proxy.isRenderWorld(getWorld())) {
				//LogisticsPipes start
				
				// destroy the item on exit if it isn't exitable
				if(!isSpecialConnectionInformationTransition && !isItemExitable(arrivingItem.getItemStack())) {
					return true;
				}
				//last chance for chassi to back out
				if(arrivingItem instanceof IRoutedItem) {
					IRoutedItem routed = (IRoutedItem) arrivingItem;
					if (!getPipe().getTransportLayer().stillWantItem(routed)) {
						reverseItem(arrivingItem);
						return true;
					}
				}
				UpgradeManager manager = getPipe().getUpgradeManager();
				boolean tookSome = false;
				if(manager.hasPatternUpgrade()) {
					if(getPipe() instanceof PipeItemsSupplierLogistics) {
						IInventory inv = (IInventory) tile;
						if (inv instanceof ISidedInventory) inv = new SidedInventoryMinecraftAdapter((ISidedInventory) inv, ForgeDirection.UNKNOWN, false);
						IInventoryUtil util = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(inv);
						if(util instanceof ISpecialInsertion) {
							PipeItemsSupplierLogistics pipe = (PipeItemsSupplierLogistics) getPipe();
							ItemIdentifierStack stack = ItemIdentifierStack.getFromStack(arrivingItem.getItemStack());
							int[] slots = pipe.getSlotsForItemIdentifier(stack.getItem());
							for(int i:slots) {
								if(util.getSizeInventory() > pipe.getInvSlotForSlot(i)) {
									ItemStack content = util.getStackInSlot(pipe.getInvSlotForSlot(i));
									ItemStack toAdd = arrivingItem.getItemStack().copy();
									toAdd.stackSize = Math.min(toAdd.stackSize, Math.max(0, pipe.getAmountForSlot(i) - (content != null ? content.stackSize : 0)));
									if(toAdd.stackSize > 0) {
										if(util.getSizeInventory() > pipe.getInvSlotForSlot(i)) {
											int added = ((ISpecialInsertion) util).addToSlot(toAdd, pipe.getInvSlotForSlot(i));
											arrivingItem.getItemStack().stackSize -= added;
											if(added > 0) {
												tookSome = true;
											}
										}
									}
								}
							}
							if(pipe.isLimited()) {
								if(arrivingItem.getItemStack().stackSize > 0) {
									reverseItem(arrivingItem);
								}
								return true;
							}
						}
					}
				}
				//sneaky insertion
				if(!manager.hasCombinedSneakyUpgrade()) {
					ForgeDirection insertion = arrivingItem.output.getOpposite();
					if(manager.hasSneakyUpgrade()) {
						insertion = manager.getSneakyOrientation();
					}
					ItemStack added = InventoryHelper.getTransactorFor(tile).add(arrivingItem.getItemStack(), insertion, true);
					
					arrivingItem.getItemStack().stackSize -= added.stackSize;
					if(added.stackSize > 0)
						tookSome = true;
					
					if(arrivingItem instanceof IRoutedItem) {
						IRoutedItem routed = (IRoutedItem) arrivingItem;
						TravelingItem newItem = (TravelingItem) routed.getCopy();
						newItem.setItemStack(added);
						insertedItemStack(newItem, tile);
					}
				} else {
					ForgeDirection[] dirs = manager.getCombinedSneakyOrientation();
					for(int i=0;i<dirs.length;i++) {
						ForgeDirection insertion = dirs[i];
						if(insertion == null) continue;
						ItemStack added = InventoryHelper.getTransactorFor(tile).add(arrivingItem.getItemStack(), insertion, true);
						
						arrivingItem.getItemStack().stackSize -= added.stackSize;
						if(added.stackSize > 0)
							tookSome = true;
						
						//For InvSysCon
						if(arrivingItem instanceof IRoutedItem) {
							IRoutedItem routed = (IRoutedItem) arrivingItem;
							TravelingItem newItem = (TravelingItem) routed.getCopy();
							newItem.setItemStack(added);
							insertedItemStack(newItem, tile);
						}
						if(arrivingItem.getItemStack().stackSize <= 0) break;
					}
				}
				if(arrivingItem.getItemStack().stackSize > 0 && tookSome && arrivingItem instanceof IRoutedItem) {
					((IRoutedItem)arrivingItem).setBufferCounter(0);
				}
				
				//LogisticsPipes end

				if(arrivingItem.getItemStack().stackSize > 0) {
					reverseItem(arrivingItem);
				}
			}
			return true;// the item is handled
		} else {
			return false; //bounce, drop, according to normal rules
		}
	}
	//BC copy end
	
	protected boolean isItemExitable(ItemStack stack) {
		if(stack != null && stack.getItem() instanceof IItemAdvancedExistance) {
			return ((IItemAdvancedExistance)stack.getItem()).canExistInNormalInventory(stack);
		}
		return true;
	}
	
	protected void insertedItemStack(TravelingItem data, TileEntity tile) {}
	
	@Override
	public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {
		return super.canPipeConnect(tile, side) || SimpleServiceLocator.betterStorageProxy.isBetterStorageCrate(tile);
	}

	/* --- IItemTravelHook --- */
	@Override
	public boolean endReached(PipeTransportItems pipe, TravelingItem data, TileEntity tile) {
		return handleTileReached(data, tile);
	}

	@Override
	public void drop(PipeTransportItems pipe, TravelingItem data) {
		data.setSpeed(0.0F);
	}

	public static final int MAX_PIPE_STACKS = 64;
	public static final int MAX_PIPE_ITEMS = 1024;
	public boolean allowBouncing = false;
	// TODO: generalize the use of this hook in particular for obsidian pipe
	public IItemTravelingHook travelHook;
	public final TravelerSet items = new TravelerSet();

	public class TravelerSet extends ForwardingSet<TravelingItem> {

		private final BiMap<Integer, TravelingItem> delegate = HashBiMap.create();
		private final Set<TravelingItem> toLoad = new HashSet<TravelingItem>();
		private final Set<TravelingItem> toRemove = new HashSet<TravelingItem>();
		private int delay = 0;

		@Override
		protected Set<TravelingItem> delegate() {
			return delegate.values();
		}

		@Override
		public boolean add(TravelingItem item) {
			if (delegate.containsValue(item))
				return false;
			item.setContainer(container);
			delegate.put(item.id, item);
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends TravelingItem> collection) {
			boolean changed = false;
			for (TravelingItem item : collection) {
				changed |= add(item);
			}
			return changed;
		}

		public TravelingItem get(int id) {
			return delegate.get(id);
		}

		private void scheduleLoad(TravelingItem item) {
			delay = 10;
			toLoad.add(item);
		}

		private void performLoad() {
			if (delay > 0) {
				delay--;
				return;
			}
			addAll(toLoad);
			toLoad.clear();
		}

		public boolean scheduleRemoval(TravelingItem item) {
			return toRemove.add(item);
		}

		public boolean unscheduleRemoval(TravelingItem item) {
			return toRemove.remove(item);
		}

		private void performRemoval() {
			removeAll(toRemove);
			toRemove.clear();
		}
	};

	public void defaultReajustSpeed(TravelingItem item) {
		float speed = item.getSpeed();

		if (speed > TransportConstants.PIPE_NORMAL_SPEED) {
			speed -= TransportConstants.PIPE_NORMAL_SPEED;
		}

		if (speed < TransportConstants.PIPE_NORMAL_SPEED) {
			speed = TransportConstants.PIPE_NORMAL_SPEED;
		}

		item.setSpeed(speed);
	}

	private void readjustPosition(TravelingItem item) {
		double x = item.xCoord;
		double y = item.yCoord;
		double z = item.zCoord;

		x = Math.max(x, container.xCoord + 0.01);
		y = Math.max(y, container.yCoord + 0.01);
		z = Math.max(z, container.zCoord + 0.01);

		x = Math.min(x, container.xCoord + 0.99);
		y = Math.min(y, container.yCoord + 0.99);
		z = Math.min(z, container.zCoord + 0.99);

		if (item.input != ForgeDirection.UP && item.input != ForgeDirection.DOWN) {
			y = container.yCoord + TransportUtils.getPipeFloorOf(item.getItemStack());
		}

		item.setPosition(x, y, z);
	}

	public void injectItem(TravelingItem item, ForgeDirection inputOrientation) {
		if (item.isCorrupted())
			// Safe guard - if for any reason the item is corrupted at this
			// stage, avoid adding it to the pipe to avoid further exceptions.
			return;

		item.reset();
		item.input = inputOrientation;

		items.add(item);

		readjustSpeed(item);
		readjustPosition(item);


		if (!container.worldObj.isRemote) {
			item.output = resolveDestination(item);
		}

		if (container.pipe instanceof IPipeTransportItemsHook) {
			((IPipeTransportItemsHook) container.pipe).entityEntered(item, inputOrientation);
		}

		if (!container.worldObj.isRemote) {
			sendItemPacket(item);

			if (items.size() > BuildCraftTransport.groupItemsTrigger) {
				groupEntities();
			}

			if (items.size() > MAX_PIPE_STACKS) {
				BCLog.logger.log(Level.WARNING, String.format("Pipe exploded at %d,%d,%d because it had too many stacks: %d", container.xCoord, container.yCoord, container.zCoord, items.size()));
				destroyPipe();
				return;
			}

			int numItems = 0;
			for (TravelingItem travellingItem : items) {
				ItemStack stack = travellingItem.getItemStack();
				if (stack != null && stack.stackSize > 0)
					numItems += stack.stackSize;
			}

			if (numItems > MAX_PIPE_ITEMS) {
				BCLog.logger.log(Level.WARNING, String.format("Pipe exploded at %d,%d,%d because it had too many items: %d", container.xCoord, container.yCoord, container.zCoord, numItems));
				destroyPipe();
			}
		}
	}

	private void destroyPipe() {
		BlockUtil.explodeBlock(container.worldObj, container.xCoord, container.yCoord, container.zCoord);
		container.worldObj.setBlockToAir(container.xCoord, container.yCoord, container.zCoord);
	}

	/**
	 * Bounces the item back into the pipe without changing the items map.
	 *
	 * @param item
	 */
	protected void reverseItem(TravelingItem item) {
		if (item.isCorrupted())
			// Safe guard - if for any reason the item is corrupted at this
			// stage, avoid adding it to the pipe to avoid further exceptions.
			return;

		items.unscheduleRemoval(item);

		item.toCenter = true;
		item.input = item.output.getOpposite();

		readjustSpeed(item);
		readjustPosition(item);

		if (!container.worldObj.isRemote) {
			item.output = resolveDestination(item);
		}

		if (container.pipe instanceof IPipeTransportItemsHook) {
			((IPipeTransportItemsHook) container.pipe).entityEntered(item, item.input);
		}

		if (!container.worldObj.isRemote) {
			sendItemPacket(item);
		}
	}

	/**
	 * Returns a list of all possible movements, that is to say adjacent
	 * implementers of IPipeEntry or TileEntityChest.
	 */
	public LinkedList<ForgeDirection> getPossibleMovements(TravelingItem item) {
		LinkedList<ForgeDirection> result = new LinkedList<ForgeDirection>();

		item.blacklist.add(item.input.getOpposite());

		for (ForgeDirection o : ForgeDirection.VALID_DIRECTIONS) {
			if (item.blacklist.contains(o))
				continue;
			if (container.pipe.outputOpen(o) && canReceivePipeObjects(o, item))
				result.add(o);
		}

		if (this.container.pipe instanceof IPipeTransportItemsHook) {
			Position pos = new Position(container.xCoord, container.yCoord, container.zCoord, item.input);
			result = ((IPipeTransportItemsHook) this.container.pipe).filterPossibleMovements(result, pos, item);
		}

		if (allowBouncing && result.isEmpty()) {
			if (canReceivePipeObjects(item.input.getOpposite(), item)) {
				result.add(item.input.getOpposite());
			}
		}

		return result;
	}

	public boolean canReceivePipeObjects(ForgeDirection o, TravelingItem item) {
		TileEntity entity = container.getTile(o);

		if (!container.isPipeConnected(o))
			return false;

		if (entity instanceof TileGenericPipe) {
			TileGenericPipe pipe = (TileGenericPipe) entity;

			return pipe.pipe.transport instanceof PipeTransportItems;
		} else if (entity instanceof IInventory && item.getInsertionHandler().canInsertItem(item, (IInventory) entity))
			if (Transactor.getTransactorFor(entity).add(item.getItemStack(), o.getOpposite(), false).stackSize > 0)
				return true;

		return false;
	}

	private void moveSolids() {
		items.performLoad();
		items.performRemoval();

		for (TravelingItem item : items) {
			if (item.isCorrupted()) {
				items.scheduleRemoval(item);
				continue;
			}

			if (item.getContainer() != this.container) {
				items.scheduleRemoval(item);
				continue;
			}

			Position motion = new Position(0, 0, 0, item.toCenter ? item.input : item.output);
			motion.moveForwards(item.getSpeed());

			item.movePosition(motion.x, motion.y, motion.z);

			if ((item.toCenter && middleReached(item)) || outOfBounds(item)) {
				item.toCenter = false;

				// Reajusting to the middle
				item.setPosition(container.xCoord + 0.5, container.yCoord + TransportUtils.getPipeFloorOf(item.getItemStack()), container.zCoord + 0.5);

				if (item.output == ForgeDirection.UNKNOWN) {
					if (travelHook != null) {
						travelHook.drop(this, item);
					}

					EntityItem dropped = null;

					if (items.scheduleRemoval(item)) {
						dropped = item.toEntityItem(item.input);
					}

					if (dropped != null) {
						onDropped(dropped);
					}
				} else {
					if (travelHook != null) {
						travelHook.centerReached(this, item);
					}
				}

			} else if (!item.toCenter && endReached(item)) {
				TileEntity tile = container.getTile(item.output);

				boolean handleItem = true;
				if (travelHook != null) {
					handleItem = !travelHook.endReached(this, item, tile);
				}

				// If the item has not been scheduled to removal by the hook
				if (handleItem && items.scheduleRemoval(item)) {
					handleTileReached(item, tile);
				}

			}
		}

		items.performRemoval();
	}

	private boolean passToNextPipe(TravelingItem item, TileEntity tile) {
		if (tile instanceof TileGenericPipe) {
			TileGenericPipe pipe = (TileGenericPipe) tile;
			if (BlockGenericPipe.isValid(pipe.pipe) && pipe.pipe.transport instanceof PipeTransportItems) {
				((PipeTransportItems) pipe.pipe.transport).injectItem(item, item.output);
				return true;
			}
		}
		return false;
	}

	private void handleTileReached(TravelingItem item, TileEntity tile) {
		if (passToNextPipe(item, tile)) {
			// NOOP
		} else if (tile instanceof IInventory) {
			if (CoreProxy.proxy.isSimulating(container.worldObj)) {
				if (item.getInsertionHandler().canInsertItem(item, (IInventory) tile)) {
					ItemStack added = Transactor.getTransactorFor(tile).add(item.getItemStack(), item.output.getOpposite(), true);
					item.getItemStack().stackSize -= added.stackSize;
				}

				if (item.getItemStack().stackSize > 0) {
					reverseItem(item);
				}
			}
		} else {
			if (travelHook != null) {
				travelHook.drop(this, item);
			}

			EntityItem dropped = item.toEntityItem(item.output);

			if (dropped != null) {
				// On SMP, the client side doesn't actually drops
				// items
				onDropped(dropped);
			}
		}
	}

	protected boolean middleReached(TravelingItem item) {
		float middleLimit = item.getSpeed() * 1.01F;
		return (Math.abs(container.xCoord + 0.5 - item.xCoord) < middleLimit && Math.abs(container.yCoord + TransportUtils.getPipeFloorOf(item.getItemStack()) - item.yCoord) < middleLimit && Math
				.abs(container.zCoord + 0.5 - item.zCoord) < middleLimit);
	}

	protected boolean endReached(TravelingItem item) {
		return item.xCoord > container.xCoord + 1 || item.xCoord < container.xCoord || item.yCoord > container.yCoord + 1 || item.yCoord < container.yCoord || item.zCoord > container.zCoord + 1 || item.zCoord < container.zCoord;
	}

	protected boolean outOfBounds(TravelingItem item) {
		return item.xCoord > container.xCoord + 2 || item.xCoord < container.xCoord - 1 || item.yCoord > container.yCoord + 2 || item.yCoord < container.yCoord - 1 || item.zCoord > container.zCoord + 2 || item.zCoord < container.zCoord - 1;
	}

	public Position getPosition() {
		return new Position(container.xCoord, container.yCoord, container.zCoord);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		NBTTagList nbttaglist = nbt.getTagList("travelingEntities");

		for (int j = 0; j < nbttaglist.tagCount(); ++j) {
			try {
				NBTTagCompound dataTag = (NBTTagCompound) nbttaglist.tagAt(j);

				TravelingItem item = new TravelingItem();
				item.readFromNBT(dataTag);

				if (item.isCorrupted()) {
					continue;
				}

				items.scheduleLoad(item);
			} catch (Throwable t) {
				// It may be the case that entities cannot be reloaded between
				// two versions - ignore these errors.
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		NBTTagList nbttaglist = new NBTTagList();

		for (TravelingItem item : items) {
			NBTTagCompound dataTag = new NBTTagCompound();
			nbttaglist.appendTag(dataTag);
			item.writeToNBT(dataTag);
		}

		nbt.setTag("travelingEntities", nbttaglist);
	}

	protected void doWork() {
	}

	/**
	 * Handles a packet describing a stack of items inside a pipe.
	 *
	 * @param packet
	 */
	public void handleItemPacket(PacketPipeTransportContent packet) {

		if (packet.getID() != PacketIds.PIPE_CONTENTS)
			return;

		TravelingItem item = items.get(packet.getTravellingItemId());
		if (item == null) {
			item = new TravelingItem(packet.getTravellingItemId());
			items.add(item);
		}

		if (item.getItemStack() == null) {
			item.setItemStack(new ItemStack(packet.getItemId(), packet.getStackSize(), packet.getItemDamage()));
			if (packet.hasNBT()) {
				PacketDispatcher.sendPacketToServer(new PacketSimpleId(PacketIds.REQUEST_ITEM_NBT, container.xCoord, container.yCoord, container.zCoord, packet.getTravellingItemId()).getPacket());
			}
		} else {
			if (item.getItemStack().itemID != packet.getItemId() || item.getItemStack().stackSize != packet.getStackSize() || item.getItemStack().getItemDamage() != packet.getItemDamage() || item.getItemStack().hasTagCompound() != packet.hasNBT()) {
				item.setItemStack(new ItemStack(packet.getItemId(), packet.getStackSize(), packet.getItemDamage()));
				if (packet.hasNBT()) {
					PacketDispatcher.sendPacketToServer(new PacketSimpleId(PacketIds.REQUEST_ITEM_NBT, container.xCoord, container.yCoord, container.zCoord, packet.getTravellingItemId()).getPacket());
				}
			}
		}

		item.setPosition(packet.getItemX(), packet.getItemY(), packet.getItemZ());

		item.setSpeed(packet.getSpeed());

		item.input = packet.getInputOrientation();
		item.output = packet.getOutputOrientation();
		item.color = packet.getColor();
	}

	/**
	 * Handles the NBT tag Request from player of the id
	 */
	public void handleNBTRequestPacket(EntityPlayer player, int entityId) {
		TravelingItem item = items.get(entityId);
		if (item == null || item.item == null || item.getItemStack() == null)
			return;
		PacketDispatcher.sendPacketToPlayer(new PacketPipeTransportNBT(PacketIds.PIPE_ITEM_NBT, container.xCoord, container.yCoord, container.zCoord, entityId, item.getItemStack().getTagCompound()).getPacket(), (Player) player);
	}

	/**
	 * Handles the Item NBT tag information of the packet
	 */
	public void handleNBTPacket(PacketPipeTransportNBT packet) {
		TravelingItem item = items.get(packet.getEntityId());
		if (item == null || item.item == null || item.getItemStack() == null)
			return;
		item.getItemStack().setTagCompound(packet.getTagCompound());
	}

	/**
	 * Creates a packet describing a stack of items inside a pipe.
	 *
	 * @param data
	 * @return
	 */
	public Packet createItemPacket(TravelingItem data) {
		PacketPipeTransportContent packet = new PacketPipeTransportContent(data);
		return packet.getPacket();
	}

	private void sendItemPacket(TravelingItem data) {
		int dimension = container.worldObj.provider.dimensionId;
		PacketDispatcher.sendPacketToAllAround(container.xCoord, container.yCoord, container.zCoord, DefaultProps.PIPE_CONTENTS_RENDER_DIST, dimension, createItemPacket(data));
	}

	public int getNumberOfStacks() {
		return items.size();
	}

	public int getNumberOfItems() {
		int num = 0;
		for (TravelingItem item : items) {
			if (item.getItemStack() == null)
				continue;
			num += item.getItemStack().stackSize;
		}
		return num;
	}

	public void onDropped(EntityItem item) {
		this.container.pipe.onDropped(item);
	}

	protected void neighborChange() {
	}

	public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {
		if (tile instanceof TileGenericPipe) {
			Pipe pipe2 = ((TileGenericPipe) tile).pipe;
			if (BlockGenericPipe.isValid(pipe2) && !(pipe2.transport instanceof PipeTransportItems))
				return false;
		}

		if (tile instanceof ISidedInventory) {
			int[] slots = ((ISidedInventory) tile).getAccessibleSlotsFromSide(side.getOpposite().ordinal());
			return slots != null && slots.length > 0;
		}

		return tile instanceof TileGenericPipe || tile instanceof ISpecialInventory || (tile instanceof IInventory && ((IInventory) tile).getSizeInventory() > 0)
				|| (tile instanceof IMachine && ((IMachine) tile).manageSolids());
	}

	public boolean isTriggerActive(ITrigger trigger) {
		return false;
	}

	/**
	 * Group all items that are similar, that is to say same dmg, same id, same
	 * nbt and no contribution controlling them
	 */
	public void groupEntities() {
		// determine groupable entities
		List<TravelingItem> entities = new ArrayList<TravelingItem>();

		for (TravelingItem item : items) {
			if (!item.hasExtraData() && item.getItemStack().stackSize < item.getItemStack().getMaxStackSize()) {
				entities.add(item);
			}
		}

		if (entities.isEmpty())
			return; // nothing groupable

		// sort the groupable entities to have all entities with the same id:dmg next to each other (contiguous range)
		Collections.sort(entities, new Comparator<TravelingItem>() {
			@Override
			public int compare(TravelingItem a, TravelingItem b) {
				// the item id is always less than 2^15 so the int won't overflow
				int itemA = (a.getItemStack().itemID << 16) | a.getItemStack().getItemDamage();
				int itemB = (b.getItemStack().itemID << 16) | b.getItemStack().getItemDamage();

				return itemA - itemB;
			}
		});

		// group the entities
		int matchStart = 0;
		int lastId = (entities.get(0).getItemStack().itemID << 16) | entities.get(0).getItemStack().getItemDamage();

		for (int i = 1; i < entities.size(); i++) {
			int id = (entities.get(i).getItemStack().itemID << 16) | entities.get(i).getItemStack().getItemDamage();

			if (id != lastId) {
				// merge within the last matching ID range
				groupEntityRange(entities, matchStart, i);

				// start of the next matching ID range
				matchStart = i;
				lastId = id;
			}
		}

		// merge last matching ID range
		groupEntityRange(entities, matchStart, entities.size());
	}

	/**
	 * Group a range of items with matching IDs (item id + meta/dmg)
	 *
	 * @param entities entity list to group
	 * @param start start index (inclusive)
	 * @param end end index (exclusive)
	 */
	private void groupEntityRange(List<TravelingItem> entities, int start, int end) {
		for (int j = start; j < end; j++) {
			TravelingItem target = entities.get(j);
			if (target == null)
				continue;

			for (int k = j + 1; k < end; k++) {
				TravelingItem source = entities.get(k);
				if (source == null)
					continue;

				// only merge if the ItemStack tags match
				if (ItemStack.areItemStackTagsEqual(source.getItemStack(), target.getItemStack())) {
					// merge source to target
					int amount = source.getItemStack().stackSize;
					int space = target.getItemStack().getMaxStackSize() - target.getItemStack().stackSize;

					if (amount <= space) {
						// source fits completely into target
						target.getItemStack().stackSize += amount;

						items.remove(source);
						entities.set(k, null);
					} else {
						target.getItemStack().stackSize += space;

						source.getItemStack().stackSize -= space;
					}

					if (amount >= space) {
						// target not usable for further additions, no need to check more sources
						break;
					}
				}
			}
		}
	}

	public void dropContents() {
		groupEntities();

		for (TravelingItem item : items) {
			container.pipe.dropItem(item.getItemStack());
		}

		items.clear();
	}

	public boolean delveIntoUnloadedChunks() {
		return true;
	}
	public LogisticsTileGenericPipe container;

	public World getWorld() {
		return container.worldObj;
	}

	public void setTile(LogisticsTileGenericPipe tile) {
		this.container = tile;
	}

	public boolean inputOpen(ForgeDirection from) {
		return true;
	}

	public boolean outputOpen(ForgeDirection to) {
		return true;
	}

}
