package com.phylogeny.simulatednights.client;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class GuiDeepSleep extends GuiSleepMP
{
	private String text = "";
	private int renderCounter;
	private boolean mimicGuiSleepMP, closing;
	private GuiScreen parentGui;
	private GuiButtonNull button;
	
	public GuiDeepSleep(GuiScreen parentGui)
	{
		this.parentGui = parentGui;
		mimicGuiSleepMP = parentGui instanceof GuiSleepMP;
		if (mimicGuiSleepMP)
		{
			GuiTextField inputField = ReflectionHelper.getPrivateValue(GuiChat.class, (GuiChat) parentGui, "field_146415_a", "a", "inputField");
			text = inputField.getText();
		}
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		buttonList.get(0).visible = false;
		if (!mimicGuiSleepMP)
			return;
		
		button = new GuiButtonNull(1, width / 2 - 100, height - 40, I18n.format("multiplayer.stopSleeping", new Object[0]));
		inputField.setText(text);
		text = "";
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		if (mimicGuiSleepMP)
		{
			int alpha = 255 - renderCounter;
			enableBlend();
			drawRect(2, height - 14, width - 2, height - 2, (alpha << 24));
			enableBlend();
			inputField.drawTextBox();
			inputField.setTextColor(14737632 | (alpha << 24));
			button.drawButton(mc, mouseX, mouseY, alpha);
		}
		drawOverlay();
		boolean fullyAsleep = mc.thePlayer.isPlayerFullyAsleep();
		if (fullyAsleep)
			renderCounter = MathHelper.clamp_int(renderCounter + (closing ? -1 : 1), 0, 251);
		
		if (mimicGuiSleepMP)
			buttonList.get(0).visible = renderCounter == 0;
		else if (renderCounter == 0 && fullyAsleep)
				Minecraft.getMinecraft().displayGuiScreen(parentGui);
	}
	
	private void drawOverlay()
	{
		int sleepTime = Math.min(80, (int) (renderCounter / 2.5F));
		float opacity = sleepTime / 100.0F;
		if (opacity > 1.0F)
			opacity = 1.0F - (sleepTime - 100) / 10.0F;
		
		int color = (int)(220.0F * opacity) << 24 | 1052688;
		drawRect(0, 0, width, height, color);
	}
	
	public static void enableBlend()
	{
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}
	
	@Override
	public void updateScreen()
	{
		if (renderCounter < 20)
			super.updateScreen();
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException
	{
		if (renderCounter == 0)
			super.keyTyped(typedChar, keyCode);
	}
	
	public void setClosingState(boolean closing)
	{
		this.closing = closing;
	}
	
	private static class GuiButtonNull extends GuiButton
	{
		
		public GuiButtonNull(int buttonId, int x, int y, String buttonText)
		{
			super(buttonId, x, y, buttonText);
		}
		
		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
		{
			return false;
		}
		
		public void drawButton(Minecraft mc, int mouseX, int mouseY, int alpha)
		{
			FontRenderer fontrenderer = mc.fontRendererObj;
			mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
			GlStateManager.color(1.0F, 1.0F, 1.0F, alpha / 255.0F);
			boolean hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
			int i = hovered ? 2 : 1;
			enableBlend();
			drawTexturedModalRect(xPosition, yPosition, 0, 46 + i * 20, width / 2, height);
			drawTexturedModalRect(xPosition + width / 2, yPosition, 200 - width / 2, 46 + i * 20, width / 2, height);
			int colorButtonText = packedFGColour != 0 ? packedFGColour : (hovered ? 16777120 : 14737632);
			enableBlend();
			drawCenteredString(fontrenderer, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, colorButtonText | (alpha << 24));
		}
		
	}
}