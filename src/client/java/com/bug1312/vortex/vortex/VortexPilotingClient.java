package com.bug1312.vortex.vortex;

import java.util.List;

import com.bug1312.vortex.helpers.Constants;
import com.bug1312.vortex.records.Waypoint;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

public class VortexPilotingClient {
	
	public static BlockPos JUNK_CENTER = Constants.TARDIS_CENTER.add(Constants.JUNK_CENTER);
	
	public static boolean isPiloting = false;
	
	public static Waypoint currentPos = new Waypoint(new BlockPos(0,100,0), Text.literal("Current"), DyeColor.WHITE.getSignColor());
	public static List<Waypoint> waypoints = List.of();

	public static boolean forwardKeyDown = false;
	public static long forwardKeyDownStart = -1;
	public static boolean awaitingForwardKeyUp = false;
	
	public static void keyTick(GameOptions settings) {
		if (!isPiloting) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		long currentTime = client.world.getTime();
		
		// Sneak Key
		if (settings.sneakKey.isPressed()) stopPiloting();
		
		// Forward Key
		forwardKeyDown = settings.forwardKey.isPressed();
		if (!forwardKeyDown) {
			forwardKeyDownStart = -1;
			if (awaitingForwardKeyUp) awaitingForwardKeyUp = false;
		} else {
			if (forwardKeyDownStart == -1) forwardKeyDownStart = currentTime;
		}
	}
	
	public static void startPiloting() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		long currentTime = client.world.getTime();
		
		VortexPilotingClient.isPiloting = true;
		VortexRendering.rotationStart = currentTime;
	
		client.inGameHud.setOverlayMessage(Text.translatable("vortex.subtitle.start", Text.keybind("key.forward"), Text.keybind("key.sneak")), false);

		client.gameRenderer.setRenderHand(false);
		client.gameRenderer.setBlockOutlineEnabled(false);
	}
	
	public static void stopPiloting() {
		MinecraftClient client = MinecraftClient.getInstance();

		VortexPilotingClient.isPiloting = false;
		client.gameRenderer.setRenderHand(true);
		client.gameRenderer.setBlockOutlineEnabled(true);
	}
}
