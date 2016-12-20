package com.phylogeny.simulatednights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.phylogeny.simulatednights.SimulatedNights;
import com.phylogeny.simulatednights.integration.IntegrationMorpheus;
import com.phylogeny.simulatednights.packet.PacketDeepSleepCheck;

public class DeepSleepHandler
{
	
	@SubscribeEvent
	public void openSleepGui(GuiOpenEvent event)
	{
		if (IntegrationMorpheus.isMorpheusLoaded && event.getGui() != null && event.getGui() instanceof GuiSleepMP && Minecraft.getMinecraft().thePlayer.isPlayerSleeping())
			SimulatedNights.packetNetwork.sendToServer(new PacketDeepSleepCheck());
	}
	
}