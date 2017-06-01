package com.phylogeny.simulatednights.reference;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.minecraftforge.fml.common.Loader;

public class Config
{
	public static boolean disableNightSimulation;
	public static float timeTickPercentage;
	public static int sleepDelay;
	public static int simulatedTicksPerServerTick;
	public static SleepExecution sleepExecution;
	public static EnumMap<SleepExecution, String> sleepExecutionMap = new EnumMap<SleepExecution, String>(SleepExecution.class);
	public static SleepSoundsFadeRange sleepSoundsFadeRange;
	public static EnumMap<SleepSoundsFadeRange, String> sleepSoundsFadeRangeMap = new EnumMap<SleepSoundsFadeRange, String>(SleepSoundsFadeRange.class);
	public static boolean sleepTickAllBlocks;
	public static boolean sleepTickAllEntities;
	public static boolean sleepTickAllTileEntities;
	public static boolean enterDeepSleep;
	public static int commandPermissionLevel;
	public static boolean commandMessageLocalization;
	public static boolean allowClientsWithMissingMod;
	public static Set<String> blacklistTileEntities;
	public static boolean checkBlacklistTileEntities;
	
	static
	{
		sleepExecutionMap.put(SleepExecution.MULTIPLE, "Over Multiple Ticks");
		sleepExecutionMap.put(SleepExecution.SINGLE, "In Single Tick");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.NORMAL, "Through Normal Sleep");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.DEEP, "Through Deep Sleep");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.ALL, "Through Normal & Deep Sleep");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.NONE, "No Sound Fading");
	}
	
	public static void setBlacklistTileEntities(String[] blacklistEntries)
	{
		List<String> list = new LinkedList<String>(Arrays.asList(blacklistEntries));
		for (int i = 0; i < list.size(); i++)
		{
			String entry = list.get(i);
			int index = entry.indexOf(":");
			if (entry.length() < 3 || index < 0 || !Loader.isModLoaded(entry.substring(0, index)))
			{
				list.remove(i);
				i--;
			}
		}
		blacklistTileEntities = new HashSet<String>(list);
		checkBlacklistTileEntities = !blacklistTileEntities.isEmpty();
	}
	
	public static void setSleepExecution(String configString)
	{
		sleepExecution = configString.equals(SleepExecution.SINGLE.getName()) ? SleepExecution.SINGLE : SleepExecution.MULTIPLE;
	}
	
	public static void setSleepSoundsFadeRange(String configString)
	{
		for (SleepSoundsFadeRange range : SleepSoundsFadeRange.values())
		{
			if (range.getName().equals(configString))
			{
				sleepSoundsFadeRange = range;
				break;
			}
		}
	}
	
	public static enum SleepExecution
	{
		MULTIPLE, SINGLE;
		
		public String getName()
		{
			return sleepExecutionMap.get(this);
		}
		
	}
	
	public static enum SleepSoundsFadeRange
	{
		NORMAL, DEEP, ALL, NONE;
		
		public String getName()
		{
			return sleepSoundsFadeRangeMap.get(this);
		}
		
	}
	
}