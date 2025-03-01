package com.bug1312.vortex;

import com.bug1312.vortex.packets.s2c.RetrieveWaypointsPayload;
import com.bug1312.vortex.vortex.VortexRendering;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

// A warning to anyone reading this code
// It is held together with tooth picks and gorilla glue
// I am absolute rubbish at rendering
// Contributions are appreciated

public class VortexModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		ClientPlayNetworking.registerGlobalReceiver(RetrieveWaypointsPayload.ID, PacketHandlerClient::RetrieveWaypoints);

		WorldRenderEvents.START.register(VortexRendering.VortexRenderer::render);
		WorldRenderEvents.LAST.register(VortexRendering.WaypointRenderer::render);
		
	}

}