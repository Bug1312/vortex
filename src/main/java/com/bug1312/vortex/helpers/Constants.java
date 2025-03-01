package com.bug1312.vortex.helpers;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Constants {

	public static final String PREFIX_JUNK = "junk";

	public static final Vec3d TARDIS_CENTER_VEC = new Vec3d(0, 3, 0);
	public static final BlockPos TARDIS_CENTER = BlockPos.ofFloored(TARDIS_CENTER_VEC);
	
	public static final Vec3i JUNK_SIZE = new Vec3i(7,5,7);
	public static final Vec3i JUNK_CENTER = new Vec3i(3, 2, 3); // Half & Floor'd Length/Width, 2 Height for floor & table
}
