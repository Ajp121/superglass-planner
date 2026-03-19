package com.superglassplanner;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SuperglassPlannerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SuperglassPlannerPlugin.class);
		RuneLite.main(args);
	}
}
