package com.bug1312.vortex.ducks;

import org.spongepowered.asm.mixin.Unique;

public interface VortexPilotHandlerDuck {
	@Unique() void startPiloting();
}
