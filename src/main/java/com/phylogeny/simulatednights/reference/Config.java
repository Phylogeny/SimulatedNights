package com.phylogeny.simulatednights.reference;

import java.util.EnumMap;
import java.util.Set;

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
	public static Set<String> blackListTileEntities;
	
	static
	{
		sleepExecutionMap.put(SleepExecution.MULTIPLE, "Over Multiple Ticks");
		sleepExecutionMap.put(SleepExecution.SINGLE, "In Single Tick");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.NORMAL, "Through Normal Sleep");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.DEEP, "Through Deep Sleep");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.ALL, "Through Normal & Deep Sleep");
		sleepSoundsFadeRangeMap.put(SleepSoundsFadeRange.NONE, "No Sound Fading");
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