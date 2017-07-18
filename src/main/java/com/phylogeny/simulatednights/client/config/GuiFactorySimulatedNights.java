package com.phylogeny.simulatednights.client.config;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

public class GuiFactorySimulatedNights implements IModGuiFactory
{
	
	@Override
	public void initialize(Minecraft minecraftInstance) {}
	
	@Override
	public boolean hasConfigGui()
	{
		return true;
	}
	
	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen)
	{
		return new GuiConfigSimulatedNights(parentScreen);
	}
	
	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
	{
		return Collections.emptySet();
	}
	
}