package com.bug1312.vortex.packets.s2c;

import java.util.List;

import com.bug1312.vortex.VortexMod;
import com.bug1312.vortex.records.Waypoint;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RetrieveWaypointsPayload(
		Waypoint currentPos,
		List<Waypoint> waypoints
) implements CustomPayload {

	public static final CustomPayload.Id<RetrieveWaypointsPayload> ID = new CustomPayload.Id<>(Identifier.of(VortexMod.MOD_ID, "to/client/retrieve_waypoints"));
	
	public static final PacketCodec<RegistryByteBuf, RetrieveWaypointsPayload> CODEC = PacketCodec.tuple(
			Waypoint.PACKET_CODEC, RetrieveWaypointsPayload::currentPos, 
			Waypoint.PACKET_CODEC.collect(PacketCodecs.toList()), RetrieveWaypointsPayload::waypoints,
   	
			RetrieveWaypointsPayload::new
	);
 
	@Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

}
