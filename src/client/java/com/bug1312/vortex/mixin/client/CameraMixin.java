package com.bug1312.vortex.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bug1312.vortex.vortex.VortexPilotingClient;
import com.bug1312.vortex.vortex.VortexRendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;

@Mixin(Camera.class)
public class CameraMixin {

	@Inject(method = "update", at = @At("TAIL"))
	public void transitionCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		
		VortexRendering.CameraManipulator.moveCamera(client.world, (Camera) ((Object) this), tickDelta);
	}
	
	@Inject(method = "isThirdPerson", at = @At("RETURN"), cancellable = true)
	public void renderPlayerWhenPiloting(CallbackInfoReturnable<Boolean> ci) {
		if (VortexPilotingClient.isPiloting) ci.setReturnValue(true);
	}

}
