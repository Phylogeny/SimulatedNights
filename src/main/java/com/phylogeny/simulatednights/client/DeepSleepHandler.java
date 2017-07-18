package com.phylogeny.simulatednights.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import com.phylogeny.simulatednights.SimulatedNights;
import com.phylogeny.simulatednights.integration.IntegrationMorpheus;
import com.phylogeny.simulatednights.packet.PacketDeepSleepCheck;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.Config.SleepSoundsFadeRange;
import com.phylogeny.simulatednights.reference.Reference;

public class DeepSleepHandler
{
	private File extendedOptionsFile;
	private float savedMasterVolume = -1.0F;
	
	public DeepSleepHandler()
	{
		extendedOptionsFile = new File(Minecraft.getMinecraft().mcDataDir, Reference.MOD_NAME.replaceAll(" ", "") + ".txt");
		loadMasterVolumeCopy();
	}
	
	@SubscribeEvent
	public void openSleepGui(GuiOpenEvent event)
	{
		if (IntegrationMorpheus.isMorpheusLoaded && event.getGui() != null && event.getGui() instanceof GuiSleepMP && Minecraft.getMinecraft().player.isPlayerSleeping())
			SimulatedNights.packetNetwork.sendToServer(new PacketDeepSleepCheck());
	}
	
	@SubscribeEvent
	public void setMasterVolumeCopy(GuiScreenEvent.MouseInputEvent.Post event)
	{
		if (event.getGui() != null && event.getGui() instanceof GuiScreenOptionsSounds)
		{
			float masterVolume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER);
			if (savedMasterVolume != masterVolume)
			{
				savedMasterVolume = masterVolume;
				saveMasterVolumeCopy();
			}
		}
	}
	
	@SubscribeEvent
	public void fadeSound(TickEvent.PlayerTickEvent event)
	{
		if (event.phase != Phase.START || event.side != Side.CLIENT)
			return;
		
		GuiScreen gui = Minecraft.getMinecraft().currentScreen;
		if (gui != null && Minecraft.getMinecraft().player != null)
		{
			int sleepTimer = Minecraft.getMinecraft().player.getSleepTimer();
			if (sleepTimer > 0)
			{
				if (Config.sleepSoundsFadeRange == SleepSoundsFadeRange.ALL)
				{
					setMasterVolume((Math.min(100, sleepTimer) * 2.8F + (gui instanceof GuiDeepSleep ? ((GuiDeepSleep) gui).getDeepSleepTimer() : 0) / 6.0F) / 315.83F);
					return;
				}
				if (gui instanceof GuiDeepSleep && ((GuiDeepSleep) gui).isPlayerFadingIntoDeepSleep() && Config.sleepSoundsFadeRange == SleepSoundsFadeRange.DEEP)
				{
					setMasterVolume(((GuiDeepSleep) gui).getPercentDeeplyAsleep());
					return;
				}
				if (Config.sleepSoundsFadeRange == SleepSoundsFadeRange.NORMAL)
				{
					setMasterVolume(Math.min(100, sleepTimer) / 100.0F);
					return;
				}
			}
		}
		if (Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER) != savedMasterVolume)
			Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.MASTER, savedMasterVolume);
	}
	
	private void setMasterVolume(float soundPercentage)
	{
		Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.MASTER, (1 - Math.min(1.0F, soundPercentage)) * savedMasterVolume);
	}
	
	private void loadMasterVolumeCopy()
	{
		@SuppressWarnings("resource")
		FileInputStream stream = null;
		try
		{
			if (extendedOptionsFile.exists())
			{
				stream = new FileInputStream(extendedOptionsFile);
				List<String> list = IOUtils.readLines(stream);
				if (list.size() > 0)
				{
					String[] data = list.get(0).split(":");
					try
					{
						if (data.length == 2 && data[0].equals("soundCategory_master_copy"))
							savedMasterVolume = parseFloat(data[1]);
					}
					catch (Exception e)
					{
						FMLLog.log(Reference.MOD_NAME, Level.WARN, "Skipping bad extended option: {}", data[0]);
					}
				}
			}
		}
		catch (Exception e)
		{
			FMLLog.log(Reference.MOD_NAME, Level.ERROR, "Failed to load master volume copy.", (Throwable) e);
		}
		finally
		{
			IOUtils.closeQuietly(stream);
		}
		if (savedMasterVolume < 0)
			savedMasterVolume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER);
	}
	
	public void saveMasterVolumeCopy()
	{
		@SuppressWarnings("resource")
		PrintWriter printWriter = null;
		try
		{
			printWriter = new PrintWriter(new FileWriter(extendedOptionsFile));
			printWriter.println("soundCategory_master_copy:" + savedMasterVolume);
			printWriter.println("Your master volume will be set to this copy of it when you wake from sleep. Do not alter this manually.");
		}
		catch (Exception e)
		{
			FMLLog.log(Reference.MOD_NAME, Level.ERROR, "Failed to save extended options", (Throwable) e);
		}
		finally
		{
			IOUtils.closeQuietly(printWriter);
		}
	}
	
	private float parseFloat(String string)
	{
		return "true".equals(string) ? 1.0F : ("false".equals(string) ? 0.0F : Float.parseFloat(string));
	}
	
}
