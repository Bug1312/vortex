package com.bug1312.vortex.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bug1312.vortex.vortex.VortexPilotingClient;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
	
	@Shadow @Final private GameOptions settings;
	
	@Inject(method = "tick", at = @At(value = "TAIL"))
	public void tick(CallbackInfo ci) {
		VortexPilotingClient.keyTick(settings);
	}
}
