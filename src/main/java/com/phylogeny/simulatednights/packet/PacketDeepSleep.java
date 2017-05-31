package com.phylogeny.simulatednights.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.phylogeny.simulatednights.client.GuiDeepSleep;

public class PacketDeepSleep implements IMessage
{
	private boolean wakePlayer;
	
	public PacketDeepSleep() {}
	
	public PacketDeepSleep(boolean wakePlayer)
	{
		this.wakePlayer = wakePlayer;
	}
	
	@Override
	public void toBytes(ByteBuf buffer)
	{
		buffer.writeBoolean(wakePlayer);
	}
	
	@Override
	public void fromBytes(ByteBuf buffer)
	{
		wakePlayer = buffer.readBoolean();
	}
	
	public static class Handler implements IMessageHandler<PacketDeepSleep, IMessage>
	{
		@Override
		public IMessage onMessage(final PacketDeepSleep message, final MessageContext ctx)
		{
			Minecraft.getMinecraft().addScheduledTask(new Runnable()
			{
				@Override
				public void run()
				{
					GuiScreen screen = Minecraft.getMinecraft().currentScreen;
					if (screen instanceof GuiDeepSleep)
						((GuiDeepSleep) screen).setClosingState(message.wakePlayer);
					else if (!message.wakePlayer)
						Minecraft.getMinecraft().displayGuiScreen(new GuiDeepSleep(screen));
				}
			});
			return null;
		}
		
	}
	
}