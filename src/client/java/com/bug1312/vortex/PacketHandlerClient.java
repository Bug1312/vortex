package com.bug1312.vortex;

import com.bug1312.vortex.packets.s2c.RetrieveWaypointsPayload;
import com.bug1312.vortex.vortex.VortexPilotingClient;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class PacketHandlerClient {

	public static void RetrieveWaypoints(RetrieveWaypointsPayload payload, ClientPlayNetworking.Context context) {
		MinecraftClient client = context.client();
		client.execute(() -> {
			VortexPilotingClient.currentPos = payload.currentPos();
			VortexPilotingClient.waypoints = payload.waypoints();
		});
	}
}
