package com.phylogeny.simulatednights.client.config;

import com.phylogeny.simulatednights.ConfigHandler;
import com.phylogeny.simulatednights.reference.Reference;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiConfigSimulatedNights extends GuiConfig
{
	
	public GuiConfigSimulatedNights(GuiScreen parentScreen)
	{
		super(parentScreen, new ConfigElement(ConfigHandler.configFile.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
				Reference.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(ConfigHandler.configFile.toString()));
	}
	
}