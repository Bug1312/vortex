package com.bug1312.vortex.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bug1312.vortex.vortex.VortexPilotingClient;
import com.bug1312.vortex.vortex.VortexRendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererAccessor {

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	public void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> ci) {
		if (!VortexPilotingClient.isPiloting || !VortexPilotingClient.forwardKeyDown) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;

		double addedAmount = VortexRendering.FovManipulator.calcAddFov(client.world.getTime(), tickDelta);
		ci.setReturnValue(ci.getReturnValue() + addedAmount);
	}

}
