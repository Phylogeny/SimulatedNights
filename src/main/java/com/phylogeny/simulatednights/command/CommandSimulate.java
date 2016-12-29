package com.phylogeny.simulatednights.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.MutablePair;

import com.phylogeny.simulatednights.SimulationHandler;
import com.phylogeny.simulatednights.SimulationHandler.TickCountCommand;
import com.phylogeny.simulatednights.reference.Config;
import com.phylogeny.simulatednights.reference.LangKey;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

public class CommandSimulate extends CommandBase
{
	public static final String NAME = "simulate";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public int getRequiredPermissionLevel()
	{
		return Config.commandPermissionLevel;
	}
	
	@Override
	public String getUsage(ICommandSender sender)
	{
		return MessageLang.USAGE.getMessageString();
	}
	
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		int argCount = args.length;
		boolean timeMode = argCount > 0 && args[0].equalsIgnoreCase("time");
		if (argCount == 0 || !(timeMode || args[0].equalsIgnoreCase("ticks")))
		{
			MessageLang.ARGUMENTS_FIRST.sendMessage(sender);
			return;
		}
		int simulatedTicks = -1;
		boolean setMode = false;
		int startIndex;
		if (timeMode)
		{
			setMode = argCount > 1 && args[1].equalsIgnoreCase("set");
			if (argCount == 1 || !(setMode || args[1].equalsIgnoreCase("add")))
			{
				MessageLang.ARGUMENTS_TIME.sendMessage(sender);
				return;
			}
			try
			{
				simulatedTicks = parseInt(args[2], 0);
			}
			catch (ArrayIndexOutOfBoundsException e) {}
			catch (NumberInvalidException e)
			{
				if (setMode)
				{
					if (args[2].equalsIgnoreCase("day"))
						simulatedTicks = 1000;
					
					if (args[2].equalsIgnoreCase("night"))
						simulatedTicks = 13000;
				}
				if (!e.getMessage().equals("commands.generic.num.invalid"))
					throw e;
			}
			if (simulatedTicks < 0)
			{
				MessageLang.ARGUMENTS_TIME_AMOUNT.sendMessage(sender);
				return;
			}
			startIndex = 3;
		}
		else
		{
			try
			{
				simulatedTicks = parseInt(args[1], 0);
			}
			catch (ArrayIndexOutOfBoundsException e) {}
			catch (NumberInvalidException e)
			{
				if (!e.getMessage().equals("commands.generic.num.invalid"))
					throw e;
			}
			if (simulatedTicks < 0)
			{
				MessageLang.ARGUMENTS_TICKS.sendMessage(sender);
				return;
			}
			startIndex = 2;
		}
		boolean dimensionSpecified = false;
		boolean dimensionSet = false;
		int dimensionId = 0;
		try
		{
			dimensionId = Integer.parseInt(args[startIndex]);
			dimensionSpecified = true;
			dimensionSet = true;
		}
		catch (ArrayIndexOutOfBoundsException e) {}
		catch (NumberFormatException e)
		{
			boolean useCurrentDimension = args[startIndex].equalsIgnoreCase("this");
			if (sender.getEntityWorld() != null && (useCurrentDimension || args[startIndex].equalsIgnoreCase("all")))
			{
				dimensionId = sender.getEntityWorld().provider.getDimension();
				dimensionSpecified = true;
				dimensionSet = useCurrentDimension;
			}
		}
		if (!dimensionSpecified)
		{
			MessageLang.ARGUMENTS_DIMENSION.sendMessage(sender);
			return;
		}
		startIndex++;
		String dimensionInfo = dimensionSet ? " " + dimensionId : " (";
		boolean tickAllEntities = true;
		boolean tickTileEntities = true;
		boolean tickBlocks = true;
		boolean runInSingleServerTick = false;
		int simulatedTicksPerServerTick = 0;
		if (argCount > startIndex)
		{
			List<String> argsRemaining = Arrays.asList(Arrays.copyOfRange(args, startIndex, args.length));
			runInSingleServerTick = argsRemaining.contains("singletick");
			if (argsRemaining.contains("allentities") || argsRemaining.contains("tileentities") || argsRemaining.contains("blocks"))
			{
				tickAllEntities = argsRemaining.contains("allentities");
				tickTileEntities = argsRemaining.contains("tileentities");
				tickBlocks = argsRemaining.contains("blocks");
			}
			for (String arg : argsRemaining)
			{
				try
				{
					simulatedTicksPerServerTick = parseInt(arg, 1);
				}
				catch (NumberInvalidException e) {}
			}
		}
		boolean foundDimension = false;
		for (int i = 0; i < server.worlds.length; i++)
		{
			WorldServer worldServer = server.worlds[i];
			if (worldServer == null)
				continue;
			
			int currentDimensionId = worldServer.provider.getDimension();
			if (dimensionSet)
			{
				if (currentDimensionId != dimensionId)
					continue;
			}
			else
				dimensionInfo += currentDimensionId + ", ";
			
			foundDimension = true;
			if (runInSingleServerTick)
				SimulationHandler.simulateTicks(worldServer, simulatedTicks, timeMode, setMode, tickAllEntities, tickTileEntities, tickBlocks, true, true);
			else
			{
				if (SimulationHandler.SERVER_SIMULATED_TICK_MAP.containsKey(dimensionId))
				{
					MessageLang.QUEUE_IN_PROGRESS.sendMessage(sender);
					return;
				}
				int tickCount = timeMode ? simulatedTicks : (int) (simulatedTicks * Config.timeTickPercentage);
				if (simulatedTicksPerServerTick < 1)
					simulatedTicksPerServerTick = Config.simulatedTicksPerServerTick;
				
				int simulatedTicksCurrent = Math.min(tickCount, simulatedTicksPerServerTick);
				int remainder = tickCount - simulatedTicksCurrent;
				SimulationHandler.simulateTicks(worldServer, simulatedTicksCurrent, timeMode, setMode, tickAllEntities, tickTileEntities, tickBlocks, true, remainder == 0);
				if (remainder > 0)
					SimulationHandler.SERVER_SIMULATED_TICK_MAP.put(dimensionId, new TickCountCommand(remainder, timeMode, setMode, tickAllEntities, tickTileEntities, tickBlocks, simulatedTicksPerServerTick));
			}
		}
		if (foundDimension)
		{
			if (!dimensionSet)
				dimensionInfo = dimensionInfo.substring(0, dimensionInfo.lastIndexOf(',')) + ")";
			
			List<ITextComponent> messages = new ArrayList<ITextComponent>();
			MessageLang messageLang = tickAllEntities ? (tickBlocks ? MessageLang.RESULT_BLOCKS_ALL_ENTITIES : MessageLang.RESULT_ALL_ENTITIES)
							: (tickBlocks ? (tickTileEntities ? MessageLang.RESULT_BLOCKS_TILEENTITIES : MessageLang.RESULT_BLOCKS) : MessageLang.RESULT_TILEENTITIES);
			messages.add(messageLang.getMessage((dimensionSet ? MessageLang.DIMENSION_SINGLE : MessageLang.DIMENSION_ALL).getMessage(dimensionId), dimensionInfo + "."));
			if (timeMode)
				messages.add(new TextComponentTranslation("commands.time." + (setMode ? "set" : "added"), Integer.valueOf(simulatedTicks)));
			
			if (SimulationHandler.SERVER_SIMULATED_TICK_MAP.containsKey(dimensionId))
				SimulationHandler.commandCompletionMessages = new MutablePair<ICommandSender, List<ITextComponent>>(sender, messages);
			else
			{
				for (ITextComponent message : messages)
					sender.sendMessage(message);
			}
		}
		else
		{
			(dimensionSet ? MessageLang.DIMENSION_MISSING_SINGLE : MessageLang.DIMENSION_MISSING_ALL).sendMessage(sender, dimensionId);
		}
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
	{
		int argCount = args.length;
		if (argCount == 1)
			return getListOfStringsMatchingLastWord(args, "ticks", "time");
		
		int startIndex = 3;
		if (args[0].equalsIgnoreCase("time"))
		{
			switch (argCount)
			{
				case 2: return getListOfStringsMatchingLastWord(args, "set", "add");
				case 3: return getListOfStringsMatchingLastWord(args, "day", "night");
			}
			startIndex++;
		}
		List<String> options = new ArrayList<String>();
		if (argCount == startIndex)
		{
			options.add("all");
			options.add("this");
			for (int i = 0; i < server.worlds.length; i++)
			{
				WorldServer worldServer = server.worlds[i];
				if (worldServer != null)
					options.add(Integer.toString(worldServer.provider.getDimension()));
			}
			options = getListOfStringsMatchingLastWord(args, options);
		}
		if (argCount > startIndex)
		{
			List<String> argsRemaining = Arrays.asList(Arrays.copyOfRange(args, startIndex, args.length));
			String[] optionalArgs = new String[]{"allentities", "blocks", "singletick"};
			for (String option : optionalArgs)
			{
				if (!argsRemaining.contains(option))
					options.add(option);
			}
			if (!argsRemaining.contains("tileentities") && !argsRemaining.contains("allentities"))
				options.add("tileentities");
			
			options = getListOfStringsMatchingLastWord(args, options);
		}
		return options;
	}
	
	private static enum MessageLang
	{
		USAGE("usage", "/simulate" +
				"\n         - <ticks : time>" +
				"\n             - follow 'ticks' with: <number of ticks to simulate>" +
				"\n             - follow 'time' with: <set : add>" +
				"\n                 - follow 'set' with: <day : night : time to set>" +
				"\n                 - follow 'add' with: <time to add>" +
				"\n         - <dimension id : 'all' : 'this' for current dimension>" +
				"\n         - [tileentities : allentities : blocks (tick those specified)]" +
				"\n         - [simulated ticks per server tick (default if absent)]" +
				"\n         - ['singletick' to run simulation all in one server tick]"),
		ARGUMENTS_FIRST("arguments.first", TextFormatting.RED, "Required first argument must be '%s' or '%s'.", "ticks", "time"),
		ARGUMENTS_TIME("arguments.time", TextFormatting.RED, "/%s %s must be followed by '%s' or '%s'.", "simulate", "time", "set", "add"),
		ARGUMENTS_TIME_AMOUNT("arguments.time.amount", TextFormatting.RED, "/%s %s <%s : %s> must be followed by the time to set or add.", "simulate", "time", "set", "add"),
		ARGUMENTS_TICKS("arguments.ticks", TextFormatting.RED, "/%s %s must be followed by the number of ticks to simulate.", "simulate", "ticks"),
		ARGUMENTS_DIMENSION("arguments.dimension", TextFormatting.RED, "Dimension argument is required and must be a valid dimension id, '%s' for the current dimension, or '%s' for all dimensions.", "this", "all"),
		DIMENSION_SINGLE("dimension.single", "dimension"),
		DIMENSION_ALL("dimension.all", "all dimensions"),
		DIMENSION_MISSING_SINGLE("dimension.missing.single", TextFormatting.RED, "No dimension was found with an id of %s."),
		DIMENSION_MISSING_ALL("dimension.missing.all", TextFormatting.RED, "No dimensions were found."),
		RESULT_ALL_ENTITIES("result.allentities", "All entities were ticked in %s%s"),
		RESULT_TILEENTITIES("result.tileentities", "All tile entities were ticked in %s%s"),
		RESULT_BLOCKS("result.blocks", "Blocks were randomly ticked in all persistent chunks of %s%s"),
		RESULT_BLOCKS_ALL_ENTITIES("result.blocks.allentities", "All entities were ticked and blocks were randomly ticked in all persistent chunks of %s%s"),
		RESULT_BLOCKS_TILEENTITIES("result.blocks.tileentities", "All tile entities were ticked and blocks were randomly ticked in all persistent chunks of %s%s"),
		QUEUE_IN_PROGRESS("queue.inprogress", TextFormatting.RED, "A simulation command is already in progress. Please wait until it is finished.");
		
		private String langKey, hardCodedText;
		private TextFormatting color;
		private Object[] args;
		
		private MessageLang(String langKey, TextFormatting color, String hardCodedText, Object... args)
		{
			this(langKey, hardCodedText, args);
			this.color = color;
		}
		
		private MessageLang(String langKey, String hardCodedText, Object... args)
		{
			this.langKey = LangKey.COMMAND_PREFIX + langKey;
			this.hardCodedText = hardCodedText;
			this.args = args;
		}
		
		private String getMessageInternal(String langKey)
		{
			return Config.commandMessageLocalization ? langKey : hardCodedText;
		}
		
		public String getMessageString()
		{
			return getMessageInternal(langKey);
		}
		
		public void sendMessage(ICommandSender sender, Object... args)
		{
			sender.sendMessage(getMessage(args));
		}
		
		public TextComponentTranslation getMessage(Object... args)
		{
			TextComponentTranslation message = new TextComponentTranslation(getMessageInternal(langKey), args.length > 0 ? args : this.args);
			if (color != null)
				message.getStyle().setColor(color);
			
			return message;
		}
		
	}
	
}
