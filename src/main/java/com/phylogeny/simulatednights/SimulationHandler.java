package com.phylogeny.simulatednights;

import com.phylogeny.simulatednights.integration.IntegrationMorpheus;
import com.phylogeny.simulatednights.packet.PacketDeepSleep;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.Config.SleepExecution;
import com.phylogeny.simulatednights.reference.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import java.lang.reflect.Field;
import java.util.*;

public class SimulationHandler
{
	public static final Map<Integer, TickCount> WORLD_SIMULATED_TICK_MAP = new HashMap<Integer, TickCount>();
	public static final Map<Integer, TickCountCommand> SERVER_SIMULATED_TICK_MAP = new HashMap<Integer, TickCountCommand>();
	public static Pair<ICommandSender, List<ITextComponent>> commandCompletionMessages = new MutablePair<ICommandSender, List<ITextComponent>>();
	private static Field sleepTimer, updateLCG;

	public static void initReflectionFields()
	{
		sleepTimer = ReflectionHelper.findField(EntityPlayer.class, "field_71076_b", "sleepTimer");
		updateLCG = ReflectionHelper.findField(World.class, "field_73005_l", "updateLCG");
	}

	private static int getInt(Field field, Object instance)
	{
		try
		{
			return field.getInt(instance);
		}
		catch (IllegalArgumentException e) {}
		catch (IllegalAccessException e) {}
		return 0;
	}

	private static void setInt(Field field, Object instance, int value)
	{
		try
		{
			field.setInt(instance, value);
		}
		catch (IllegalArgumentException e) {}
		catch (IllegalAccessException e) {}
	}

	@SubscribeEvent
	public void simulateNight(TickEvent.WorldTickEvent event)
	{
		if (event.phase != Phase.START)
			return;

		WorldServer worldServer = DimensionManager.getWorld(event.world.provider.getDimension());
		if (worldServer == null)
			return;

		int dimensionId = worldServer.provider.getDimension();
		if (SERVER_SIMULATED_TICK_MAP.containsKey(dimensionId))
		{
			TickCountCommand tickCount = SERVER_SIMULATED_TICK_MAP.get(dimensionId);
			int simulatedTicks = Math.min(tickCount.getCount(), tickCount.getSimulatedTicksPerServerTick());
			int remainder = tickCount.getCount() - simulatedTicks;
			if (remainder != 0)
				tickCount.setCount(remainder);

			tickCount.executeSimulation(worldServer, simulatedTicks, remainder == 0);
			if (remainder == 0)
			{
				ICommandSender sender = commandCompletionMessages.getLeft();
				if (sender != null)
				{
					List<ITextComponent> messages = commandCompletionMessages.getRight();
					for (int i = 0; i < messages.size(); i++)
						sender.sendMessage(messages.get(i));

					commandCompletionMessages.setValue(new ArrayList<ITextComponent>());
				}
				SERVER_SIMULATED_TICK_MAP.remove(dimensionId);
			}
		}
		if (Config.disableNightSimulation || !worldServer.getGameRules().getBoolean("doDaylightCycle"))
			return;

		boolean allAsleep = worldServer.areAllPlayersAsleep();
		if (allAsleep || (IntegrationMorpheus.isMorpheusLoaded
				&& event.world.getWorldTime() % 20L == 9 && IntegrationMorpheus.areEnoughPlayersAsleep(event.world)))
		{
			if (Config.sleepExecution == SleepExecution.SINGLE)
			{
				if (Config.sleepDelay > 0)
				{
					if (!WORLD_SIMULATED_TICK_MAP.containsKey(dimensionId))
					{
						WORLD_SIMULATED_TICK_MAP.put(dimensionId, new TickCount(Config.sleepDelay));
						delaySleep(worldServer, true);
						return;
					}
					TickCount sleepDelay = WORLD_SIMULATED_TICK_MAP.get(dimensionId);
					int newDelay = sleepDelay.getCount() - (allAsleep ? 1 : 20);
					if (newDelay <= 0)
						WORLD_SIMULATED_TICK_MAP.remove(dimensionId);
					else
					{
						delaySleep(worldServer, !sleepDelay.wasRecentlySet());
						sleepDelay.setCount(newDelay);
						return;
					}
				}
				simulateTicks(worldServer, getTimeUntilMourning(worldServer), true, true, true, true);
				return;
			}
			boolean wasNew = false;
			if (!WORLD_SIMULATED_TICK_MAP.containsKey(dimensionId))
			{
				WORLD_SIMULATED_TICK_MAP.put(dimensionId, new TickCount(getTimeUntilMourning(worldServer)));
				wasNew = true;
			}
			TickCount tickCount = WORLD_SIMULATED_TICK_MAP.get(dimensionId);
			int simulatedTicks = Math.min(tickCount.getCount(), Config.simulatedTicksPerServerTick * (allAsleep ? 1 : 20));
			int remainder = tickCount.getCount() - simulatedTicks;
			boolean starting = wasNew || !tickCount.wasRecentlySet();
			if (remainder == 0)
				WORLD_SIMULATED_TICK_MAP.remove(dimensionId);
			else
			{
				tickCount.setCount(remainder);
				delaySleep(worldServer, starting);
			}
			simulateTicks(worldServer, simulatedTicks, true, true, starting, remainder == 0);
		}
		else if (Config.enterDeepSleep && WORLD_SIMULATED_TICK_MAP.containsKey(dimensionId) && (!IntegrationMorpheus.isMorpheusLoaded || event.world.getWorldTime() % 20L == 9))
		{
			TickCount tickCount = WORLD_SIMULATED_TICK_MAP.get(dimensionId);
			if (!tickCount.wasRecentlySet())
				return;

			int sleepTimer;
			for (EntityPlayer entityPlayer : worldServer.playerEntities)
			{
				sleepTimer = getInt(this.sleepTimer, entityPlayer);
				if (sleepTimer >= 100)
					SimulatedNights.packetNetwork.sendTo(new PacketDeepSleep(true), (EntityPlayerMP) entityPlayer);
			}
			tickCount.setNotRecentlySet();
			if (Config.sleepExecution == SleepExecution.MULTIPLE)
				endMessage(dimensionId);
		}
	}

	private int getTimeUntilMourning(WorldServer worldServer)
	{
		long time24Hr = 24000L;
		long timeOld = worldServer.provider.getWorldTime();
		long timeNew = timeOld + time24Hr;
		timeNew -= timeNew % time24Hr;
		return (int) ((timeNew - timeOld) * Config.timeTickPercentage);
	}

	private void delaySleep(WorldServer worldServer, boolean enterDeepSleep)
	{
		int sleepTimer;
		for (EntityPlayer entityPlayer : worldServer.playerEntities)
		{
			sleepTimer = getInt(this.sleepTimer, entityPlayer);
			if (sleepTimer >= 100)
			{
				setInt(this.sleepTimer, entityPlayer, 99);
				if (Config.enterDeepSleep && enterDeepSleep)
					SimulatedNights.packetNetwork.sendTo(new PacketDeepSleep(false), (EntityPlayerMP) entityPlayer);
			}
		}
		IntegrationMorpheus.preventWakeUpAlert(worldServer);
	}

	public static void simulateTicks(WorldServer worldServer, int simulatedTicks, boolean tickTileEntities, boolean tickBlocks, boolean notifyStart, boolean notifyEnd)
	{
		simulateTicks(worldServer, simulatedTicks, false, false, Config.sleepTickAllEntities, tickTileEntities, tickBlocks, notifyStart, notifyEnd);
	}

	public static void simulateTicks(final WorldServer worldServer, final int simulatedTicks, final boolean affectTime, final boolean setMode,
			final boolean tickAllEntities, final boolean tickTileEntities, final boolean tickBlocks, final boolean notifyStart, final boolean notifyEnd)
	{
		final int dimensionId = worldServer.provider.getDimension();
		final int time = affectTime ? simulatedTicks : (int) (simulatedTicks * Config.timeTickPercentage);
		worldServer.addScheduledTask(new Runnable()
		{
			@Override
			public void run()
			{
				if (notifyStart)
					startMessage(dimensionId);

				int updateSeed = getInt(updateLCG, worldServer);
				int randomTickSpeed = worldServer.getGameRules().getInt("randomTickSpeed");
				int chunkX, chunkZ, randomPos, x, y, z, i, j;
				IBlockState iblockstate;
				Block block;
				List<TileEntity> tileEntities = worldServer.tickableTileEntities;
				TileEntity tileEntity;
				ResourceLocation tileEntityBlockName;
				Chunk chunk;
				BlockPos blockpos;
				List<Chunk> chunks = new ArrayList<Chunk>();
				Iterator<Chunk> iterator = worldServer.getPersistentChunkIterable(worldServer.getPlayerChunkMap().getChunkIterator());
				ArrayList<TileEntity> removedTileEntities = new ArrayList<TileEntity>();
				while (iterator.hasNext())
					chunks.add(iterator.next());

				for (int n = 0; n < simulatedTicks; n++)
				{
					if (tickBlocks && randomTickSpeed > 0)
					{
						for (i = 0; i < chunks.size(); i++)
						{
							chunk = chunks.get(i);
							if (chunk == null)
								continue;

							chunkX = chunk.x * 16;
							chunkZ = chunk.z * 16;
							for (ExtendedBlockStorage extendedblockstorage : chunk.getBlockStorageArray())
							{
								if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE || !extendedblockstorage.needsRandomTick())
									continue;

								for (j = 0; j < randomTickSpeed; j++)
								{
									updateSeed = updateSeed * 3 + 1013904223;
									randomPos = updateSeed >> 2;
									x = randomPos & 15;
									z = randomPos >> 8 & 15;
									y = randomPos >> 16 & 15;
									iblockstate = extendedblockstorage.get(x, y, z);
									block = iblockstate.getBlock();
									if (block.getTickRandomly())
										block.randomTick(worldServer, new BlockPos(x + chunkX, y + extendedblockstorage.getYLocation(), z + chunkZ), iblockstate, worldServer.rand);
								}
							}
						}
					}
					if (tickAllEntities)
					{
						try
						{
							if (Config.checkBlacklistTileEntities)
							{
								for (i = 0; i < tileEntities.size(); i++)
								{
									tileEntity = tileEntities.get(i);
									tileEntityBlockName = worldServer.getBlockState(tileEntities.get(i).getPos()).getBlock().getRegistryName();
									if (tileEntityBlockName != null && Config.blacklistTileEntities.contains(tileEntityBlockName.toString()))
									{
										removedTileEntities.add(tileEntity);
										tileEntities.remove(i);
										i--;
									}
								}
							}
							worldServer.updateEntities();
							if (Config.checkBlacklistTileEntities)
							{
								for (i = 0; i < removedTileEntities.size(); i++)
									tileEntities.add(removedTileEntities.get(i));
							}
						}
						catch (Throwable throwable)
						{
							CrashReport crashReport = CrashReport.makeCrashReport(throwable, "A fatal error has occurred as a result of ticking all world entities " +
									"in the process of simulating server ticks:");
							FMLLog.log(Reference.MOD_NAME, Level.FATAL, crashReport.getCompleteReport());
							throw new ReportedException(crashReport);
						}
						continue;
					}
					if (!tickTileEntities)
						continue;

					for (i = 0; i < tileEntities.size(); i++)
					{
						tileEntity = tileEntities.get(i);
						if (tileEntity.isInvalid() || !tileEntity.hasWorld())
							continue;

						if (Config.checkBlacklistTileEntities)
						{
							tileEntityBlockName = worldServer.getBlockState(tileEntities.get(i).getPos()).getBlock().getRegistryName();
							if (tileEntityBlockName != null && Config.blacklistTileEntities.contains(tileEntityBlockName.toString()))
								continue;
						}
						blockpos = tileEntity.getPos();
						if (worldServer.isBlockLoaded(blockpos, false) && worldServer.getWorldBorder().contains(blockpos))
						{
							try
							{
								((ITickable) tileEntity).update();
							}
							catch (Throwable throwable)
							{
								CrashReport crashReport = CrashReport.makeCrashReport(throwable, "A fatal error has occurred as a result of ticking a tile entity " +
										"in the process of simulating server ticks:");
								CrashReportCategory crashReportCategory = crashReport.makeCategory("Block entity being ticked");
								tileEntity.addInfoToCrashReport(crashReportCategory);
								FMLLog.log(Reference.MOD_NAME, Level.FATAL, crashReport.getCompleteReport());
								throw new ReportedException(crashReport);
							}
						}
					}
				}
				setInt(updateLCG, worldServer, updateSeed);
				if (notifyEnd)
					endMessage(dimensionId);

				if (affectTime)
					worldServer.setWorldTime(time + (setMode ? 0 : worldServer.getWorldTime()));
			}
		});
	}

	public static void startMessage(int dimensionId)
	{
		FMLLog.log(Reference.MOD_NAME, Level.INFO, "Begin server tick simulation in dimension " + dimensionId + ".");
	}

	public static void endMessage(int dimensionId)
	{
		FMLLog.log(Reference.MOD_NAME, Level.INFO, "End server tick simulation in dimension " + dimensionId + ".");
	}

	public static class TickCount
	{
		private int count;
		private boolean recentlySet = true;

		public TickCount(int count)
		{
			this.count = count;
		}

		public int getCount()
		{
			return count;
		}

		public void setCount(int count)
		{
			this.count = count;
			recentlySet = true;
		}

		public void setNotRecentlySet()
		{
			recentlySet = false;
		}

		public boolean wasRecentlySet()
		{
			return recentlySet;
		}

	}

	public static class TickCountCommand extends TickCount
	{
		private boolean affectTime, setMode, tickAllEntities, tickTileEntities, tickBlocks;
		private int simulatedTicksPerServerTick;

		public TickCountCommand(int count, boolean affectTime, boolean setMode, boolean tickAllEntities,
				boolean tickTileEntities, boolean tickBlocks, int simulatedTicksPerServerTick)
		{
			super(count);
			this.affectTime = affectTime;
			this.setMode = setMode;
			this.tickAllEntities = tickAllEntities;
			this.tickTileEntities = tickTileEntities;
			this.tickBlocks = tickBlocks;
			this.simulatedTicksPerServerTick = simulatedTicksPerServerTick;
		}

		public void executeSimulation(WorldServer worldServer, int simulatedTicks, boolean notifyEnd)
		{
			simulateTicks(worldServer, simulatedTicks, affectTime, setMode, tickAllEntities, tickTileEntities, tickBlocks, false, notifyEnd);
		}

		public int getSimulatedTicksPerServerTick()
		{
			return simulatedTicksPerServerTick;
		}

	}

}
