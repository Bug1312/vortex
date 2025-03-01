package com.bug1312.vortex.ducks;

import org.spongepowered.asm.mixin.Unique;

import net.minecraft.state.property.BooleanProperty;

public interface SignBlocksMixinDuck {
	@Unique() static final BooleanProperty WAYPOINT_PROPERTY = BooleanProperty.of("waypoint");
}
