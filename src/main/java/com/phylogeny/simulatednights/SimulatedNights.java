package com.phylogeny.simulatednights;

import java.util.Map;

import com.phylogeny.simulatednights.command.CommandSimulate;
import com.phylogeny.simulatednights.proxy.ProxyCommon;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.Reference;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = Reference.MOD_ID, version = Reference.VERSION, guiFactory = Reference.GUI_FACTORY_CLASSPATH, acceptedMinecraftVersions = Reference.MC_VERSIONS_ACCEPTED)
public class SimulatedNights
{
	@Mod.Instance(Reference.MOD_ID)
	public static SimulatedNights instance;
	
	@SidedProxy(clientSide = Reference.CLIENT_CLASSPATH, serverSide = Reference.COMMON_CLASSPATH)
	public static ProxyCommon proxy;
	
	public static SimpleNetworkWrapper packetNetwork = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);
	
	@NetworkCheckHandler
	public boolean checkConnectingClient(Map<String, String> versions, Side side)
	{
		return side == Side.SERVER || Config.allowClientsWithMissingMod || versions.containsKey(Reference.MOD_ID);
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		proxy.preInit(event);
	}
	
	@EventHandler
	public void postInit(@SuppressWarnings("unused") FMLPostInitializationEvent event)
	{
		proxy.postInit();
	}
	
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandSimulate());
	}
	
}