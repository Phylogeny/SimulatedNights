package com.phylogeny.simulatednights.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.phylogeny.simulatednights.client.DeepSleepHandler;

public class ProxyClient extends ProxyCommon
{
	public static DeepSleepHandler deepSleepHandler;
	
	@Override
	public void preInit(FMLPreInitializationEvent event)
	{
		super.preInit(event);
		deepSleepHandler = new DeepSleepHandler();
		MinecraftForge.EVENT_BUS.register(deepSleepHandler);
	}
	
	@Override
	public void postInit()
	{
		super.postInit();
		deepSleepHandler.saveMasterVolumeCopy();
	}
	
}