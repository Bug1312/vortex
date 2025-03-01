package com.bug1312.vortex.packets.c2s;

import com.bug1312.vortex.VortexMod;
import com.bug1312.vortex.records.Waypoint;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MoveToWaypointPayload(
		Waypoint waypoint
) implements CustomPayload {
	
	public static final CustomPayload.Id<MoveToWaypointPayload> ID = new CustomPayload.Id<>(Identifier.of(VortexMod.MOD_ID, "to/server/move_to_waypoint"));
	
	public static final PacketCodec<RegistryByteBuf, MoveToWaypointPayload> CODEC = PacketCodec.tuple(
			Waypoint.PACKET_CODEC, MoveToWaypointPayload::waypoint, 
   	
			MoveToWaypointPayload::new
	);
 
	@Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

}
