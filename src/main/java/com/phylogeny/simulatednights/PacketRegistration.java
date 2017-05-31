package com.phylogeny.simulatednights;

import net.minecraftforge.fml.relauncher.Side;

import com.phylogeny.simulatednights.packet.PacketDeepSleep;
import com.phylogeny.simulatednights.packet.PacketDeepSleepCheck;

public class PacketRegistration
{
	public static int packetId = 0;
	
	public static void registerPackets()
	{
		registerPacket(PacketDeepSleep.Handler.class, PacketDeepSleep.class, Side.CLIENT);
		registerPacket(PacketDeepSleepCheck.Handler.class, PacketDeepSleepCheck.class, Side.SERVER);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void registerPacket(Class handler, Class packet, Side side)
	{
		SimulatedNights.packetNetwork.registerMessage(handler, packet, packetId++, side);
	}
	
}