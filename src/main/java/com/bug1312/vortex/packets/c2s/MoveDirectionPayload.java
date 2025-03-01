package com.bug1312.vortex.packets.c2s;

import com.bug1312.vortex.VortexMod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MoveDirectionPayload(
		float direction
) implements CustomPayload {
	
	public static final CustomPayload.Id<MoveDirectionPayload> ID = new CustomPayload.Id<>(Identifier.of(VortexMod.MOD_ID, "to/server/move_direction"));
	
	public static final PacketCodec<RegistryByteBuf, MoveDirectionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT, MoveDirectionPayload::direction, 
   	
			MoveDirectionPayload::new
	);
 
	@Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
