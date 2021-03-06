package com.phylogeny.simulatednights.client.config;

import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries.IConfigEntry;
import net.minecraftforge.fml.client.config.GuiConfigEntries.StringEntry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import com.phylogeny.simulatednights.ConfigHandler;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.Config.SleepExecution;
import com.phylogeny.simulatednights.reference.Reference;

public class GuiConfigSimulatedNights extends GuiConfig
{
	public GuiConfigSimulatedNights(GuiScreen parentScreen)
	{
		super(parentScreen, new ConfigElement(ConfigHandler.configFile.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
				Reference.MOD_ID, false, false, GuiConfig.getAbridgedConfigPath(ConfigHandler.configFile.toString()));
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		greySleepDelay();
	}
	
	@Override
	protected void mouseClicked(int x, int y, int mouseEvent) throws IOException
	{
		super.mouseClicked(x, y, mouseEvent);
		greySleepDelay();
	}
	
	private void greySleepDelay()
	{
		boolean multipleTicks = false;
		for (IConfigEntry entry : entryList.listEntries)
		{
			Object mode = entry.getCurrentValue();
			if (Config.sleepExecutionMap.values().contains(mode))
			{
				multipleTicks = mode.equals(SleepExecution.MULTIPLE.getName());
				break;
			}
		}
		for (IConfigEntry entry : entryList.listEntries)
		{
			if (entry.getName().equals("Sleep Delay"))
			{
				GuiTextField textFieldValue = ReflectionHelper.getPrivateValue(StringEntry.class, (StringEntry) entry, "textFieldValue");
				textFieldValue.setTextColor(multipleTicks ? 5263440 : 14737632);
				ReflectionHelper.setPrivateValue(StringEntry.class, (StringEntry) entry, textFieldValue, "textFieldValue");
				break;
			}
		}
	}
	
}