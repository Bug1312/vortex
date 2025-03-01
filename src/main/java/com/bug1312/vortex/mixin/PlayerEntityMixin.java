package com.bug1312.vortex.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.bug1312.vortex.ducks.VortexPilotHandlerDuck;

import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements VortexPilotHandlerDuck {

	@Override
	public void startPiloting() { }

}
