package com.phylogeny.simulatednights.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.phylogeny.simulatednights.ConfigHandler;
import com.phylogeny.simulatednights.SimulationHandler;
import com.phylogeny.simulatednights.PacketRegistration;
import com.phylogeny.simulatednights.integration.IntegrationMorpheus;

public class ProxyCommon
{
	
	public void preInit(FMLPreInitializationEvent event)
	{
		ConfigHandler.setUpConfigs(event.getSuggestedConfigurationFile());
		MinecraftForge.EVENT_BUS.register(new ConfigHandler());
		MinecraftForge.EVENT_BUS.register(new SimulationHandler());
		PacketRegistration.registerPackets();
	}
	
	public void postInit()
	{
		IntegrationMorpheus.isMorpheusLoaded = Loader.isModLoaded("morpheus") || Loader.isModLoaded("Morpheus");
	}
	
}