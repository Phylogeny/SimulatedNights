package com.phylogeny.simulatednights;

import java.io.File;

import org.apache.logging.log4j.Level;

import com.phylogeny.simulatednights.command.CommandSimulate;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.LangKey;
import com.phylogeny.simulatednights.reference.Reference;

import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

public class ConfigHandler
{
	public static Configuration configFile;
	
	public static void setUpConfigs(File file)
	{
		configFile = new Configuration(file);
		updateConfigs();
	}
	
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.getModID().equalsIgnoreCase(Reference.MOD_ID))
			updateConfigs();
	}
	
	private static void updateConfigs()
	{
		try
		{
			Config.disableNightSimulation = configFile.getBoolean("Disable Night Simulation", Configuration.CATEGORY_GENERAL, false, 
					"If set to true, the game ticks skipped by sleeping will not be simulated. The ability to simulate ticks/time with the /simulate" +
					"command will remain. If set to false, nothing in this mod will be disabled. (default = enabled)",
					LangKey.CONFIG_PREFIX + "mod.disable");
			
			Config.timeTickPercentage = configFile.getFloat("Time Tick Percentage", Configuration.CATEGORY_GENERAL, 1.0F, 0.0F, Float.MAX_VALUE, 
					"Percentage of the game ticks skipped by sleeping (or skipped by setting/adding time with the /simulate command) to simulate. (default = 100%)",
					LangKey.CONFIG_PREFIX + "tick.percentage");
			
			Config.sleepDelay = configFile.getInt("Sleep Delay", Configuration.CATEGORY_GENERAL, 200, 0, Integer.MAX_VALUE, 
					"Number of ticks players must stay in bed (after falling asleep normally) before the night is simulated and morning arrives. (default = 10 seconds)",
					LangKey.CONFIG_PREFIX + "sleep.delay");
			
			Config.enterDeepSleep = configFile.getBoolean("Enter Deep Sleep", Configuration.CATEGORY_GENERAL, true, 
					"If set to true, when night begins to be simulated (or if there is a sleep delay, when the delay begins) all sleeping players will fade into " +
					"a deep sleep that they will not be able to leave until 1) morning arrives; 2) they forcibly close their client; or 3) there is sleep delay with " +
					"remaining time and the sleeping conditions cease to be met (due to someone entering the dimension, for example). In the case of the " +
					"latter, all players in a deep sleep will fade back out of it into a normal sleep until the conditions are one again met (causing them " +
					"to yet again fade in). If set to false, players will always remain in the default sleep GUI (where they can leave the bed at any point " +
					"during a sleep delay, if there is one). (default = yes)",
					LangKey.CONFIG_PREFIX + "sleep.deep");
			
			Config.commandPermissionLevel = configFile.getInt("Command Permission Level", Configuration.CATEGORY_GENERAL, 4, 0, 4, 
					"The minimum permission level a user must be to use the " + CommandSimulate.NAME + " command. (default = max level)",
					LangKey.CONFIG_PREFIX + "command.permissionlevel");
			
			Config.commandMessageLocalization = configFile.getBoolean("Command Messages Translate on Client", Configuration.CATEGORY_GENERAL, false, 
					"If set to true, command messages will be sent to clients as lang keys to be translated. The down-side is that if the client does not have this " +
					"mod installed, the messages will be useless raw lang key strings. If set to false, the hard-coded strings (in English) will be sent. The " +
					"down-side is that even if the client has this mod installed and has a non-English file, it will not translate to their language. " +
					"(default = send hard-coded English strings)",
					LangKey.CONFIG_PREFIX + "command.client.localization");
			
			Config.allowClientsWithMissingMod = configFile.getBoolean("Allow Clients With Missing Mod", Configuration.CATEGORY_GENERAL, true, 
					"If set to true, clients that do not have this mod installed can connect to and play on servers that do. If set to false, clients will not " +
					"be allowed to connect without this mod installed. (default = allow)",
					LangKey.CONFIG_PREFIX + "client.missingmod");
		}
		catch (Exception e)
		{
			FMLLog.log(Reference.MOD_NAME, Level.ERROR, " configurations failed to update.");
			e.printStackTrace();
		}
		finally
		{
			if (configFile.hasChanged())
				configFile.save();
		}
	}
	
}