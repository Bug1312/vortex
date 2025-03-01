package com.bug1312.vortex.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bug1312.vortex.vortex.VortexPilotingClient;

import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "doAttack", at = @At(value = "HEAD"), cancellable = true)
	public void doAttack(CallbackInfoReturnable<Boolean> ci) {
		if (VortexPilotingClient.isPiloting) {
			ci.setReturnValue(false);
			ci.cancel();
		}
	}
	
	@Inject(method = "doItemUse", at = @At(value = "HEAD"), cancellable = true)
	public void doItemUse(CallbackInfo ci) {
		if (VortexPilotingClient.isPiloting) ci.cancel();
	}

	@Inject(method = "doItemPick", at = @At(value = "HEAD"), cancellable = true)
	public void doItemPick(CallbackInfo ci) {
		if (VortexPilotingClient.isPiloting) ci.cancel();
	}
	
	@Inject(method = "handleBlockBreaking", at = @At(value = "HEAD"), cancellable = true)
	public void handleBlockBreaking(CallbackInfo ci) {
		if (VortexPilotingClient.isPiloting) ci.cancel();
	}

}
