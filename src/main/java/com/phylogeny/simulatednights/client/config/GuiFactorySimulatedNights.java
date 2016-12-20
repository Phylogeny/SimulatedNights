package com.phylogeny.simulatednights.client.config;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

public class GuiFactorySimulatedNights implements IModGuiFactory
{
	
	@Override
	public void initialize(Minecraft minecraftInstance) {}
	
	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass()
	{
		return GuiConfigSimulatedNights.class;
	}
	
	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories()
	{
		return null;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element)
	{
		return null;
	}
	
}