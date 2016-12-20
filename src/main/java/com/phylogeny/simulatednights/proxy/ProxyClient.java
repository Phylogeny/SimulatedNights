package com.phylogeny.simulatednights.proxy;

import com.phylogeny.simulatednights.client.DeepSleepHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ProxyClient extends ProxyCommon
{
	
	@Override
	public void preInit(FMLPreInitializationEvent event)
	{
		super.preInit(event);
		MinecraftForge.EVENT_BUS.register(new DeepSleepHandler());
	}
	
	@Override
	public void postInit()
	{
		super.postInit();
	}
	
}