package com.bug1312.vortex.vortex;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.bug1312.vortex.VortexMod;
import com.bug1312.vortex.helpers.DimensionIdentifier;
import com.bug1312.vortex.packets.c2s.MoveDirectionPayload;
import com.bug1312.vortex.packets.c2s.MoveToWaypointPayload;
import com.bug1312.vortex.records.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class VortexRendering {
	
	public static boolean isTardisPresent = false;
	
	public static Optional<Waypoint> lookingAtWaypoint = Optional.empty();
	
	public static long rotationStart = -1;
	
	public static void rotateOrbit(MatrixStack matrixStack, Camera camera, long time, float tickDelta, boolean forward) {		
		float speedModifier = 1;
		float timePassed = (float) (time - rotationStart) + tickDelta;
		
		Vec3d centerPos = VortexPilotingClient.JUNK_CENTER.toCenterPos();
		
		matrixStack.translate(-camera.getPos().x, 0, -camera.getPos().z);
		matrixStack.translate(centerPos.x, centerPos.y, centerPos.z);

		matrixStack.multiply((forward ? RotationAxis.POSITIVE_Y : RotationAxis.NEGATIVE_Y).rotationDegrees(timePassed * speedModifier));

		matrixStack.translate(-centerPos.x, -centerPos.y, -centerPos.z);
		matrixStack.translate(camera.getPos().x, 0, camera.getPos().z);
	}
	
	public static List<Pair<Vec3d, Waypoint>> calculateWaypoints() {
		Vec3d origin = VortexPilotingClient.currentPos.pos().toCenterPos();
		
		double pitchMin = -40;
		double pitchMax = 10;
		
		double distanceMin = 50;
		double distanceMax = 72;
		
		List<Pair<Vec3d, Waypoint>> result = new ArrayList<>();

		// Find global min and max for pitch and distance
		double minPitch = Double.MAX_VALUE;
		double maxPitch = Double.MIN_VALUE;
		double minDistance = Double.MAX_VALUE;
		double maxDistance = Double.MIN_VALUE;
		
		if (VortexPilotingClient.waypoints.size() == 1) {
			minPitch = pitchMin;
			maxPitch = pitchMax;
			minDistance = distanceMin;
			maxDistance = distanceMax;
		}
		
		for (Waypoint waypoint : VortexPilotingClient.waypoints) {
			Vec3d point = waypoint.pos().toCenterPos();
						
			Vec3d relative = point.subtract(origin);
			double distance = relative.length();
			double pitch = Math.toDegrees(relative.y);

			minDistance = Math.min(minDistance, distance);
			maxDistance = Math.max(maxDistance, distance);
			minPitch = Math.min(minPitch, pitch);
			maxPitch = Math.max(maxPitch, pitch);
		} 
		
		for (Waypoint waypoint : VortexPilotingClient.waypoints) {
			Vec3d point = waypoint.pos().toCenterPos();
			// Step 1: Calculate relative position
			Vec3d relative = point.subtract(origin);
			
			// Step 2: Convert to spherical coordinates
			double distance = relative.subtract(0, relative.y, 0).length();
			double pitch = Math.toDegrees(relative.y);
			double yaw = Math.toDegrees(Math.atan2(relative.z, relative.x));

			// Step 3: Normalize pitch and distance
			double normalizedPitch = mapRange(pitch, minPitch, maxPitch, pitchMin, pitchMax);
			double normalizedDistance = mapRange(distance, minDistance, maxDistance, distanceMin, distanceMax);

			// Step 4: Convert back to Cartesian coordinates
			double radPitch = Math.toRadians(normalizedPitch);
			double radYaw = Math.toRadians(yaw);

			double newX = normalizedDistance * Math.cos(radPitch) * Math.cos(radYaw);// + origin.x;
			double newY = normalizedDistance * Math.sin(radPitch);// + origin.y;
			double newZ = normalizedDistance * Math.cos(radPitch) * Math.sin(radYaw);// + origin.z;

			result.add(new Pair<Vec3d, Waypoint>(new Vec3d(newX, newY, newZ), waypoint));
		}

		// Filter points that are too close in angle
		List<Pair<Vec3d, Waypoint>> filtered = new ArrayList<>();
		
		for (Pair<Vec3d, Waypoint> point : result) {
			boolean tooClose = false;
			Vec3d pos = point.getLeft();
			
			// Convert current point to angles
			double pitch = Math.toDegrees(Math.asin(pos.y / pos.length()));
			double yaw = Math.toDegrees(Math.atan2(pos.z, pos.x));
			
			// Compare against all points we've already accepted
			for (Pair<Vec3d, Waypoint> existing : filtered) {
				Vec3d existingPos = existing.getLeft();
				
				// Get angles of existing point
				double existingPitch = Math.toDegrees(Math.asin(existingPos.y / existingPos.length()));
				double existingYaw = Math.toDegrees(Math.atan2(existingPos.z, existingPos.x));
				
				// Calculate angular distance
				double pitchDiff = Math.abs(pitch - existingPitch);
				double yawDiff = Math.abs(yaw - existingYaw);
				if (yawDiff > 180) yawDiff = 360 - yawDiff;
				
				// If angles are too close, check which point is closer to origin
				if (pitchDiff < 6 && yawDiff < 11) {
					tooClose = true;
					if (pos.length() < existingPos.length()) {
						filtered.remove(existing);
						filtered.add(point);
					}
					break;
				}
			}
			
			// Add point if it's not too close to any existing points
			if (!tooClose) {
				filtered.add(point);
			}
		}
		
		result = filtered;

		return result;
	}
	
	private static double mapRange(double value, double inMin, double inMax, double outMin, double outMax) {
		return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
	}

	
	public static class CameraManipulator {
		
		public static void moveCamera(ClientWorld world, Camera camera, float tickDelta) {
			boolean isJunk = DimensionIdentifier.isJunk(world);
		
			long time = world.getTime();
						
			if (isJunk) moveCameraJunk(camera, time, tickDelta);
		}

		private static void moveCameraJunk(Camera camera, long time, float tickDelta) {
			if (!VortexPilotingClient.isPiloting) return;
			Entity focusedEntity = camera.focusedEntity;
			float timePassed = (float) (time - rotationStart) + tickDelta;
			Vec3d pos = VortexPilotingClient.JUNK_CENTER.toCenterPos();

			double transitionTime = 120;
			float distance = 8.5F;

			double percent = Math.min(timePassed / transitionTime, 1);
			double easeOutCurve = 1 - Math.pow(1 - percent, 5);
			
			camera.setRotation(focusedEntity.getYaw(tickDelta), focusedEntity.getPitch(tickDelta));
			
			camera.setPos(
				MathHelper.lerp(easeOutCurve, focusedEntity.getX(), pos.x),
				MathHelper.lerp(easeOutCurve, focusedEntity.getEyeY(), pos.y),
				MathHelper.lerp(easeOutCurve, focusedEntity.getZ(), pos.z)
			);
			
			camera.moveBy((float) MathHelper.lerp(easeOutCurve, 0.0, -distance), 0.0f, 0.0f);
		}
		
	}
	
	public static class FovManipulator {
		
		public static double calcAddFov(long time, float tickDelta) {
			float timeDown = (float) (time - VortexPilotingClient.forwardKeyDownStart) + tickDelta;

			float zoomInTime = 50;
			float zoomOutTime = 5;

			float zoomInMax = 50;

			if ((timeDown / zoomInTime) < 1) { // Zoom in
				double percentIn = Math.min(timeDown / zoomInTime, 1);
				double lerpIn = curveOvershootStart(percentIn);
				
				return MathHelper.lerp(lerpIn, 0, zoomInMax);
			} else { // Zoom out
				double percentOut = Math.min((timeDown - zoomInTime) / zoomOutTime, 1);
				double lerpOut = curveOvershootEnd(percentOut);
				if (lerpOut >= 1 && !VortexPilotingClient.awaitingForwardKeyUp) {
					MinecraftClient client = MinecraftClient.getInstance();

					if (client.cameraEntity.getPitch() == 90) {
						
					} else if (VortexRendering.lookingAtWaypoint.isPresent()) {			
						ClientPlayNetworking.send(new MoveToWaypointPayload(VortexRendering.lookingAtWaypoint.get()));
					} else {					
						ClientPlayNetworking.send(new MoveDirectionPayload(Direction.fromRotation(client.cameraEntity.getYaw()).ordinal()));
//
//						
//						float f = client.cameraEntity.getPitch() * (float) (Math.PI / 180.0);
//						float g = -(client.cameraEntity.getYaw()) * (float) (Math.PI / 180.0);
//						float h = MathHelper.cos(g);
//						float i = MathHelper.sin(g);
//						float j = MathHelper.cos(f);
//						float k = MathHelper.sin(f);
//						Vector3f vector = new Vector3f((float) (i * j), (float) (-k), (float) (h * j));
//
//						
//						ClientPlayNetworking.send(new ClientToServerPayload(true, vector, VortexPilotingClient.currentPos.pos()));
					}
					
					VortexPilotingClient.awaitingForwardKeyUp = true;
				}
				
				return MathHelper.lerp(lerpOut, zoomInMax, 0);
			}
		}
		
		// y=x^2(3-2x)-0.75x(1-x)^2
		private static double curveOvershootStart(double x) {
			return x * x * (3 - 2 * x) - 0.75 * x * (1 - x) * (1 - x);
		}

		// y=x(1+0.3x-0.3x^2)+x(1-x)(1.3-0.3x)
		private static double curveOvershootEnd(double x) {
			return x * (1 + 0.3 * x - 0.3 * x * x) + (1 - x) * x * (1.3 - 0.3 * x);
		}

	}
	
	public static class WaypointRenderer {
		
		private static final RenderLayer POINT_RENDER_LAYER_ABOVE = RenderLayer.of(
				Identifier.of(VortexMod.MOD_ID, "point_render_above").toString(),
				VertexFormats.POSITION_TEXTURE,
				VertexFormat.DrawMode.QUADS,
				256,
				false,
				true,
				RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.POSITION_TEXTURE_PROGRAM)
					.texture(new RenderPhase.Texture(Identifier.ofVanilla("textures/atlas/particles.png"), false, false))
					.depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
					.cull(RenderPhase.ENABLE_CULLING)
					.build(false)
			);

		private static final RenderLayer POINT_RENDER_LAYER_BELOW = RenderLayer.of(
				Identifier.of(VortexMod.MOD_ID, "point_render_below").toString(),
				VertexFormats.POSITION_TEXTURE,
				VertexFormat.DrawMode.QUADS,
				256,
				false,
				true,
				RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.POSITION_TEXTURE_PROGRAM)
					.texture(new RenderPhase.Texture(Identifier.ofVanilla("textures/atlas/particles.png"), false, false))
					.depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
					.cull(RenderPhase.ENABLE_CULLING)
					.build(false)
			);
		
		private static void renderWaypoint(WorldRenderContext context, Waypoint waypoint, Vector3f renderTarget, boolean canInteract) {			
			renderPoint(context, waypoint, renderTarget, canInteract);
			if (VortexPilotingClient.isPiloting) renderText(context, renderTarget, waypoint.label().getString(), waypoint.color());
		}
		
		private static void renderPoint(WorldRenderContext context, Waypoint waypoint, Vector3f renderTarget, boolean canInteract) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.world == null) return;

			MatrixStack matrixStack = context.matrixStack();
			Camera camera = client.getEntityRenderDispatcher().camera;

			matrixStack.push();
			matrixStack.translate(renderTarget.x, renderTarget.y, renderTarget.z);

			// Apply billboard effect
			Quaternionf rotation = camera.getRotation();
			matrixStack.multiply(rotation);
			
			matrixStack.translate(-1, -1, -1); // Offset sprite
			matrixStack.scale(2, 2, 2); // Scale sprite

			// Get the player's look vector
			Vec3d lookVec = client.player.getRotationVec(context.tickCounter().getTickDelta(true));
			Vec3d particleVec = new Vec3d(renderTarget.x, renderTarget.y, renderTarget.z).normalize();

			// Calculate the angle between the player's look vector and the particle
			double dotProduct = lookVec.dotProduct(particleVec);
			double angle = Math.acos(dotProduct); // Angle in radians
			double angleInDegrees = Math.toDegrees(angle);

			// Determine if the player is looking at the particle
			boolean isLookingAtParticle = canInteract && (angleInDegrees < 5.0); // Safe zone of ±5 degrees

			if (isLookingAtParticle && VortexPilotingClient.isPiloting) {
				matrixStack.translate(-0.5, -0.5, -0.5);
				matrixStack.scale(2, 2, 2);
				VortexRendering.lookingAtWaypoint = Optional.of(waypoint); 
			}
			
			Matrix4f matrix = matrixStack.peek().getPositionMatrix();
			VertexConsumer vertexConsumer = context.consumers().getBuffer(VortexPilotingClient.isPiloting ? POINT_RENDER_LAYER_ABOVE : POINT_RENDER_LAYER_BELOW);

			try {
				Sprite sprite = client.particleManager.particleAtlasTexture.getSprite(Identifier.of("minecraft:glow"));

				vertexConsumer.vertex(matrix, 1, 0, 1).texture(sprite.getMinU(), sprite.getMaxV());
				vertexConsumer.vertex(matrix, 1, 1, 1).texture(sprite.getMinU(), sprite.getMinV());
				vertexConsumer.vertex(matrix, 0, 1, 1).texture(sprite.getMaxU(), sprite.getMinV());
				vertexConsumer.vertex(matrix, 0, 0, 1).texture(sprite.getMaxU(), sprite.getMaxV());
			} catch (Error err) { }

			matrixStack.pop();
		}
		
		private static void renderText(WorldRenderContext context, Vector3f target, String name, int color) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.world == null) return;

			MatrixStack matrixStack = context.matrixStack();
			TextRenderer textRenderer = client.textRenderer;
			Camera camera = client.getEntityRenderDispatcher().camera;
			
			matrixStack.push();

			matrixStack.translate(target.x, target.y, target.z);

			// Apply billboard effect
			Quaternionf rotation = camera.getRotation();
			matrixStack.multiply(rotation);
			// Rotate s'more because text weird :P
			matrixStack.multiply(new Quaternionf(1,0,0,0));
			
			matrixStack.translate(-1, -1, -1); // Offset sprite
			matrixStack.translate(1, 2.5, 0); // Offset text below the sprite
			matrixStack.scale(2, 2, 2); // Scale sprite
			matrixStack.scale(0.15F, 0.15F, 0.15F); // Scale text
			
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_ALWAYS);
			
			float timeDown = (float) (client.world.getTime() - VortexPilotingClient.forwardKeyDownStart) + client.getRenderTickCounter().getTickDelta(true);
			
			float fadeOutTime = 15;
			float popBackStart = 50;

			int opacity = 255;
			
			if ((timeDown / popBackStart) < 1) {
				double percent = Math.min(timeDown / fadeOutTime, 1);
				opacity = (int) MathHelper.lerp(percent, 255, 4);
			}
						
			String[] arr = name.split("\n");
			for (int i = 0; i < arr.length; i++) {
				textRenderer.draw(
						arr[i],
						-textRenderer.getWidth(arr[i]) / 2F, 
						i * 9, 
						(color & 0x00FFFFFF) | (opacity << 24),
						false, 
						matrixStack.peek().getPositionMatrix(),
						context.consumers(),
						TextRenderer.TextLayerType.SEE_THROUGH,
						0, 
						15728880
					);
			}
			
			RenderSystem.disableDepthTest();

			matrixStack.pop();
		}
		
		public static void render(WorldRenderContext context) {			
			MatrixStack matrixStack = context.matrixStack();
			
			Camera cm = context.camera();

			matrixStack.push();

			boolean isJunk = DimensionIdentifier.isJunk(context.world());


			
			if (isJunk && VortexPilotingClient.isPiloting) rotateOrbit(matrixStack, cm, context.world().getTime(), context.tickCounter().getTickDelta(true), false);
			
			VortexRendering.lookingAtWaypoint = Optional.empty();
			if (isJunk || VortexPilotingClient.isPiloting) {
				VortexRendering.calculateWaypoints().forEach(pair -> renderWaypoint(context, pair.getRight(), pair.getLeft().toVector3f(), true));
				renderWaypoint(context, VortexPilotingClient.currentPos, new Vector3f(0, -30, 0), false);
			}

			
			matrixStack.pop();
		}		

	}
 
	public static class VortexRenderer {
		
		private static final RenderLayer VORTEX_RENDER_LAYER = RenderLayer.of(
				Identifier.of(VortexMod.MOD_ID, "vortex_render").toString(),
				VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS,
				256,
				false,
				true,
				RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.POSITION_COLOR_TEXTURE_LIGHTMAP_PROGRAM)
					.colorLogic(RenderPhase.NO_COLOR_LOGIC)
					.lightmap(new RenderPhase.Lightmap(false))
					.texture(new RenderPhase.Texture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, false, false))
					.lightmap(RenderPhase.ENABLE_LIGHTMAP)
					.build(false)			
				);

		
		public static void render(WorldRenderContext context) {
			ClientWorld world = context.world();
			long time = world.getTime();
			float tickDelta = context.tickCounter().getTickDelta(true);
						
			boolean isJunk = DimensionIdentifier.isJunk(world);
			
			if (isJunk) {
				
				
				MatrixStack matrixStack = new MatrixStack();
				Camera camera = context.camera();		
				
				matrixStack.push();

				RenderLayer renderLayer = VORTEX_RENDER_LAYER;
				VertexConsumer vertexConsumer = context.consumers().getBuffer(renderLayer);
				
				// TODO: This section doesn't make sense when leaving pilot mode
				
				// This rotates the vortex in the same way the world rotates when the world isn't rotating
				// This makes the transition between piloting and non-piloting in Junk TARDIS seam-less
				if (isJunk && VortexPilotingClient.isPiloting) rotateOrbit(matrixStack, camera, time, tickDelta, false);

				matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(time + tickDelta));
				matrixStack.translate(300, 10, 0);

				rotateOrbit(matrixStack, camera, time, tickDelta, true);

				renderInnerTorus(matrixStack, vertexConsumer, 300, 80, 20);

				matrixStack.pop();
			} else {
				VortexPilotingClient.stopPiloting();
			}

		}

		private static void renderInnerTorus(MatrixStack matrices, VertexConsumer vertexConsumer, float edgeRadius, float holeRadius, int segments) {
			for (int i = 0; i < segments; i++) {
				float theta1 = (float) (i * 2 * Math.PI / segments);
				float theta2 = (float) ((i + 1) * 2 * Math.PI / segments);

				for (int j = 0; j < segments; j++) {
					float phi1 = (float) (j * 2 * Math.PI / segments);
					float phi2 = (float) ((j + 1) * 2 * Math.PI / segments);

					// Calculate vertices for inner quad
					float x1 = (edgeRadius - holeRadius * MathHelper.cos(phi1)) * MathHelper.cos(theta1);
					float y1 = holeRadius * MathHelper.sin(phi1);
					float z1 = (edgeRadius - holeRadius * MathHelper.cos(phi1)) * MathHelper.sin(theta1);

					float x2 = (edgeRadius - holeRadius * MathHelper.cos(phi2)) * MathHelper.cos(theta1);
					float y2 = holeRadius * MathHelper.sin(phi2);
					float z2 = (edgeRadius - holeRadius * MathHelper.cos(phi2)) * MathHelper.sin(theta1);

					float x3 = (edgeRadius - holeRadius * MathHelper.cos(phi2)) * MathHelper.cos(theta2);
					float y3 = holeRadius * MathHelper.sin(phi2);
					float z3 = (edgeRadius - holeRadius * MathHelper.cos(phi2)) * MathHelper.sin(theta2);

					float x4 = (edgeRadius - holeRadius * MathHelper.cos(phi1)) * MathHelper.cos(theta2);
					float y4 = holeRadius * MathHelper.sin(phi1);
					float z4 = (edgeRadius - holeRadius * MathHelper.cos(phi1)) * MathHelper.sin(theta2);

					// Calculate normals for lighting
					Vector3f normal1 = new Vector3f(x1, y1, z1).normalize();
					Vector3f normal2 = new Vector3f(x2, y2, z2).normalize();
					Vector3f normal3 = new Vector3f(x3, y3, z3).normalize();
					Vector3f normal4 = new Vector3f(x4, y4, z4).normalize();

					// Render quad for inner face with lighting
					renderVortexQuad(matrices.peek().getPositionMatrix(), vertexConsumer, x1, y1, z1, normal1, x2, y2, z2, normal2, x3, y3, z3, normal3, x4, y4, z4, normal4);
				}
			}

		}

		private static void renderVortexQuad(Matrix4f matrix, VertexConsumer vertexConsumer, float x1, float y1, float z1, Vector3f normal1, float x2, float y2, float z2, Vector3f normal2, float x3, float y3, float z3, Vector3f normal3, float x4, float y4, float z4, Vector3f normal4) {
			SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/stone"));
			Sprite sprite = spriteIdentifier.getSprite();

			vertexConsumer.vertex(matrix, x1, y1, z1).color(100, 0, 255, 255).texture(sprite.getMinU(), sprite.getMaxV()).overlay(0).light(0).normal(normal1.x(), normal1.y(), normal1.z());
			vertexConsumer.vertex(matrix, x2, y2, z2).color(100, 0, 255, 255).texture(sprite.getMinU(), sprite.getMinV()).overlay(0).light(0).normal(normal2.x(), normal2.y(), normal2.z());
			vertexConsumer.vertex(matrix, x3, y3, z3).color(10, 0, 255, 255).texture(sprite.getMaxU(), sprite.getMinV()).overlay(0).light(0).normal(normal3.x(), normal3.y(), normal3.z());
			vertexConsumer.vertex(matrix, x4, y4, z4).color(10, 0, 255, 255).texture(sprite.getMaxU(), sprite.getMaxV()).overlay(0).light(0).normal(normal4.x(), normal4.y(), normal4.z());
		}

	}

}
