package com.bug1312.vortex.helpers;

import com.bug1312.vortex.VortexMod;

import net.minecraft.world.World;

public class DimensionIdentifier {
	
	// Hmm, I wonder why this is separated logic *smirk*

	static final String MATCHER_JUNK = String.format("^%s:%s", VortexMod.MOD_ID, Constants.PREFIX_JUNK);
	
	public static boolean isJunk(World world) {
		return world.getRegistryKey().getValue().toString().matches(MATCHER_JUNK);
	}

}
