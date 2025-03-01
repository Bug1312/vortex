package com.bug1312.vortex.mixin.client;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bug1312.vortex.helpers.DimensionIdentifier;
import com.bug1312.vortex.vortex.VortexPilotingClient;
import com.bug1312.vortex.vortex.VortexRendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.RotationAxis;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	
	@Inject(method = "render", at = @At(value = "HEAD"))
	private void rotateWorld(
			RenderTickCounter tickCounter,
			boolean renderBlockOutline,
			Camera camera,
			GameRenderer gameRenderer,
			LightmapTextureManager lightmapTextureManager,
			Matrix4f matrices,
			Matrix4f projectionMatrix,
			CallbackInfo ci
		) {   	
		
		if (!VortexPilotingClient.isPiloting) return;
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		if (!DimensionIdentifier.isJunk(client.world)) return;
		float tickDelta = tickCounter.getTickDelta(true);

		matrices.translate((float) -camera.getPos().x, 0, (float) -camera.getPos().z);
		matrices.translate(VortexPilotingClient.JUNK_CENTER.toCenterPos().toVector3f());

		float time = (float) (client.world.getTime() - VortexRendering.rotationStart) + tickDelta;
		
		matrices.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(time));
		
		matrices.translate(VortexPilotingClient.JUNK_CENTER.toCenterPos().toVector3f().mul(-1));
		matrices.translate((float) camera.getPos().x, 0, (float) camera.getPos().z);
	}
	
}
