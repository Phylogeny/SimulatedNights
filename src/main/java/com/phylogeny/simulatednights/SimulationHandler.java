package com.phylogeny.simulatednights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;

import com.phylogeny.simulatednights.integration.IntegrationMorpheus;
import com.phylogeny.simulatednights.packet.PacketDeepSleep;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
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

public class SimulationHandler
{
	public static final Map<Integer, SleepDelay> WORLD_SLEEP_DELAY_MAP = new HashMap<Integer, SleepDelay>();
	
	@SubscribeEvent
	public void simulateNight(TickEvent.WorldTickEvent event)
	{
		if (event.phase != Phase.START)
			return;
		
		WorldServer worldServer = DimensionManager.getWorld(event.world.provider.getDimension());
		if (Config.disableNightSimulation || worldServer == null || !worldServer.getGameRules().getBoolean("doDaylightCycle"))
			return;
		
		boolean allAsleep = worldServer.areAllPlayersAsleep();
		int dimensionId = worldServer.provider.getDimension();
		if (allAsleep || (IntegrationMorpheus.isMorpheusLoaded
				&& event.world.getWorldTime() % 20L == 9 && IntegrationMorpheus.areEnoughPlayersAsleep(event.world)))
		{
			if (Config.sleepDelay > 0)
			{
				if (!WORLD_SLEEP_DELAY_MAP.containsKey(dimensionId))
				{
					WORLD_SLEEP_DELAY_MAP.put(dimensionId, new SleepDelay(Config.sleepDelay));
					delaySleep(worldServer, true);
					return;
				}
				SleepDelay sleepDelay = WORLD_SLEEP_DELAY_MAP.get(dimensionId);
				int newDelay = sleepDelay.getDelay() - (allAsleep ? 1 : 20);
				if (newDelay <= 0)
					WORLD_SLEEP_DELAY_MAP.remove(dimensionId);
				else
				{
					delaySleep(worldServer, !sleepDelay.wasRecentlySet());
					sleepDelay.setDelay(newDelay);
					return;
				}
			}
			long time24Hr = 24000L;
			long timeOld = worldServer.provider.getWorldTime();
			long timeNew = timeOld + time24Hr;
			timeNew -= timeNew % time24Hr;
			simulateTicks(worldServer, (int) (timeNew - timeOld), true, true);
		}
		else if (Config.enterDeepSleep && WORLD_SLEEP_DELAY_MAP.containsKey(dimensionId) && (!IntegrationMorpheus.isMorpheusLoaded || event.world.getWorldTime() % 20L == 9))
		{
			SleepDelay sleepDelay = WORLD_SLEEP_DELAY_MAP.get(dimensionId);
			if (!sleepDelay.wasRecentlySet())
				return;
			
			int sleepTimer;
			for (EntityPlayer entityPlayer : worldServer.playerEntities)
			{
				sleepTimer = ReflectionHelper.getPrivateValue(EntityPlayer.class, entityPlayer, "field_71076_b", "sleepTimer");
				if (sleepTimer >= 100)
					SimulatedNights.packetNetwork.sendTo(new PacketDeepSleep(true), (EntityPlayerMP) entityPlayer);
			}
			sleepDelay.setNotRecentlySet();
		}
	}
	
	private void delaySleep(WorldServer worldServer, boolean enterDeepSleep)
	{
		int sleepTimer;
		for (EntityPlayer entityPlayer : worldServer.playerEntities)
		{
			sleepTimer = ReflectionHelper.getPrivateValue(EntityPlayer.class, entityPlayer, "field_71076_b", "sleepTimer");
			if (sleepTimer >= 100)
			{
				ReflectionHelper.setPrivateValue(EntityPlayer.class, entityPlayer, 99, "field_71076_b", "sleepTimer");
				if (Config.enterDeepSleep && enterDeepSleep)
					SimulatedNights.packetNetwork.sendTo(new PacketDeepSleep(false), (EntityPlayerMP) entityPlayer);
			}
		}
		IntegrationMorpheus.preventWakeUpAlert(worldServer);
	}
	
	public static void simulateTicks(WorldServer worldServer, int simulatedTicks, boolean tickTileEntities, boolean tickBlocks)
	{
		simulateTicks(worldServer, simulatedTicks, false, false, tickTileEntities, tickBlocks);
	}
	
	public static void simulateTicks(final WorldServer worldServer, final int time, final boolean affectTime,
			final boolean setMode, final boolean tickTileEntities, final boolean tickBlocks)
	{
		final int simulatedTicks = (int) (time * Config.timeTickPercentage);
		worldServer.addScheduledTask(new Runnable()
		{
			@Override
			public void run()
			{
				FMLLog.log(Reference.MOD_NAME, Level.INFO, "Begin game tick simulation.");
				int updateSeed = ReflectionHelper.getPrivateValue(World.class, worldServer, "field_73005_l", "updateLCG");
				int randomTickSpeed = worldServer.getGameRules().getInt("randomTickSpeed");
				int chunkX, chunkZ, randomPos, x, y, z, i, j;
				IBlockState iblockstate;
				Block block;
				List<TileEntity> tileEntities = worldServer.tickableTileEntities;
				TileEntity tileEntity;
				Chunk chunk;
				BlockPos blockpos;
				List<Chunk> chunks = new ArrayList<Chunk>();
				Iterator<Chunk> iterator = worldServer.getPersistentChunkIterable(worldServer.getPlayerChunkMap().getChunkIterator());
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
							
							chunkX = chunk.xPosition * 16;
							chunkZ = chunk.zPosition * 16;
							for (ExtendedBlockStorage extendedblockstorage : chunk.getBlockStorageArray())
							{
								if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE || !extendedblockstorage.getNeedsRandomTick())
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
					if (!tickTileEntities)
						continue;
					
					for (i = 0; i < tileEntities.size(); i++)
					{
						tileEntity = tileEntities.get(i);
						if (tileEntity.isInvalid() || !tileEntity.hasWorldObj())
							continue;
						
						blockpos = tileEntity.getPos();
						if (worldServer.isBlockLoaded(blockpos, false) && worldServer.getWorldBorder().contains(blockpos))
						{
							try
							{
								((ITickable) tileEntity).update();
							}
							catch (Throwable throwable)
							{
								CrashReport crashReport = CrashReport.makeCrashReport(throwable, "Ticking block entity");
								CrashReportCategory crashReportCategory = crashReport.makeCategory("Block entity being ticked");
								tileEntity.addInfoToCrashReport(crashReportCategory);
								FMLLog.log(Reference.MOD_NAME, Level.FATAL, "A fatal error has occurred as a result of ticking a tile entity " +
										"in the process of simulating the skipped game ticks of the night:");
								FMLLog.log(Reference.MOD_NAME, Level.FATAL, crashReport.getCompleteReport());
								throw new ReportedException(crashReport);
							}
						}
					}
				}
				ReflectionHelper.setPrivateValue(World.class, worldServer, updateSeed, "field_73005_l", "updateLCG");
				FMLLog.log(Reference.MOD_NAME, Level.INFO, "End game tick simulation.");
				if (affectTime)
					worldServer.setWorldTime(time + (setMode ? 0 : worldServer.getWorldTime()));
			}
		});
	}
	
	public static class SleepDelay
	{
		private int delay;
		private boolean recentlySet = true;
		
		public SleepDelay(int delay)
		{
			this.delay = delay;
		}
		
		public int getDelay()
		{
			return delay;
		}
		
		public void setDelay(int delay)
		{
			this.delay = delay;
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
	
}