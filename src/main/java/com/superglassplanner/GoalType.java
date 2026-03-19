package com.superglassplanner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Goal type for the Superglass Planner.
 */
@Getter
@RequiredArgsConstructor
public enum GoalType
{
	TARGET_LEVEL("Target Level"),
	TARGET_XP("Target XP"),
	TARGET_GLASS("Target Glass Amount");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
