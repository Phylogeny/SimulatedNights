package com.phylogeny.simulatednights.reference;

public class Config
{
	public static boolean disableNightSimulation;
	public static float timeTickPercentage;
	public static int sleepDelay;
	public static int simulatedTicksPerServerTick;
	public static boolean segmentSleepExecution;
	public static final String[] SEGMENT_SLEEP_EXECUTION_VALID_VALUES = new String[]{"Over Multiple Ticks", "In Single Tick"};
	public static boolean sleepTickAllBlocks;
	public static boolean sleepTickAllEntities;
	public static boolean sleepTickAllTileEntities;
	public static boolean enterDeepSleep;
	public static int commandPermissionLevel;
	public static boolean commandMessageLocalization;
	public static boolean allowClientsWithMissingMod;
	
	public static void setSegmentSleepExecution(String configString)
	{
		segmentSleepExecution = !configString.equals(SEGMENT_SLEEP_EXECUTION_VALID_VALUES[1]);
	}
	
}