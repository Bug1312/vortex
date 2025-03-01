package com.bug1312.vortex.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bug1312.vortex.vortex.VortexPilotingClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public class EntityMixin {

	@Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
	public void changeLookDirection(CallbackInfo ci) {
		if (!VortexPilotingClient.isPiloting) return;
		MinecraftClient client = MinecraftClient.getInstance();
				
		if (
			client.player == ((Entity) ((Object) this)) && 
			VortexPilotingClient.forwardKeyDown && 
			!VortexPilotingClient.awaitingForwardKeyUp
		) ci.cancel();
	}

}
