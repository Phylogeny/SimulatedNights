package com.phylogeny.simulatednights.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.quetzi.morpheus.Morpheus;
import net.quetzi.morpheus.MorpheusRegistry;
import net.quetzi.morpheus.helpers.SleepChecker;
import net.quetzi.morpheus.world.WorldSleepState;

public class IntegrationMorpheus
{
	public static boolean isMorpheusLoaded;
	
	public static boolean areEnoughPlayersAsleep(World world)
	{
		int dimension = world.provider.getDimension();
		WorldSleepState sleepState = Morpheus.playerSleepStatus.get(dimension);
		if (sleepState == null)
			return false;
		
		boolean enough = false;
		List<String> usernames = new ArrayList<String>();
		/* Mimics code in SleepChecker#updatePlayerStates that sets players sleeping
		   and then checks if enough people are sleeping to advance to morning.
		   This is done to beat Morpheus to the punch in determining as such.
		 */
		for (EntityPlayer entityPlayer : world.playerEntities)
		{
			String username = entityPlayer.getGameProfile().getName();
			if (entityPlayer.isPlayerFullyAsleep() && !sleepState.isPlayerSleeping(username))
			{
				/* Actually sets players sleeping so that WorldSleepState#getPercentSleeping
				   will return the soon-to-be correct value when called below.
				 */
				sleepState.setPlayerAsleep(username);
				usernames.add(username);
			}
		}
		if (Morpheus.playerSleepStatus.get(dimension).getSleepingPlayers() > 0
				&& (dimension == 0 || MorpheusRegistry.registry.get(dimension) != null)
				&& sleepState.getPercentSleeping() >= Morpheus.perc)
		{
			enough = true;
		}
		else
		{
			/* Puts the players back the way they were found so Morpheus can set them sleeping.
			   This allows the proper alerts to be generated and sent in SleepChecker.
			 */
			for (String username : usernames)
				sleepState.setPlayerAwake(username);
		}
		return enough;
	}
	
	public static void preventWakeUpAlert(WorldServer worldServer)
	{
		if (!isMorpheusLoaded)
			return;
		
		HashMap<Integer, Boolean> alertSent = ReflectionHelper.getPrivateValue(SleepChecker.class, Morpheus.checker, "alertSent");
		alertSent.put(worldServer.provider.getDimension(), true);
		ReflectionHelper.setPrivateValue(SleepChecker.class, Morpheus.checker, alertSent, "alertSent");
	}
	
}