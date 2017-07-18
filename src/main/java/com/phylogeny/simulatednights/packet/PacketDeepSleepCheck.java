package com.phylogeny.simulatednights.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.phylogeny.simulatednights.SimulatedNights;
import com.phylogeny.simulatednights.SimulationHandler;
import com.phylogeny.simulatednights.reference.Config;

public class PacketDeepSleepCheck implements IMessage
{
	public PacketDeepSleepCheck() {}

	@Override
	public void toBytes(ByteBuf buffer) {}

	@Override
	public void fromBytes(ByteBuf buffer) {}

	public static class Handler implements IMessageHandler<PacketDeepSleepCheck, IMessage>
	{
		@Override
		public IMessage onMessage(final PacketDeepSleepCheck message, final MessageContext ctx)
		{
			WorldServer mainThread = (WorldServer) ctx.getServerHandler().player.world;
			mainThread.addScheduledTask(new Runnable()
			{
				@Override
				public void run()
				{
					if (!Config.enterDeepSleep)
						return;

					int dimensionId = ctx.getServerHandler().player.world.provider.getDimension();
					if (SimulationHandler.WORLD_SIMULATED_TICK_MAP.containsKey(dimensionId)
							&& SimulationHandler.WORLD_SIMULATED_TICK_MAP.get(dimensionId).wasRecentlySet())
						SimulatedNights.packetNetwork.sendTo(new PacketDeepSleep(false), ctx.getServerHandler().player);
				}
			});
			return null;
		}

	}

}
