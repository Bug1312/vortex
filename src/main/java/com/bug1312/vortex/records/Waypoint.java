package com.bug1312.vortex.records;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

public record Waypoint(
	BlockPos pos,
	Text label,
	int color,
	boolean allowed
) {
	
	public Waypoint(BlockPos pos, Text label) {
		this(pos, label, DyeColor.WHITE.getSignColor(), false);
	}
	
	public Waypoint(BlockPos pos, Text label, int color) {
		this(pos, label, color, false);
	}

	public static final PacketCodec<ByteBuf, Waypoint> PACKET_CODEC = new PacketCodec<ByteBuf, Waypoint>() {
		public Waypoint decode(ByteBuf byteBuf) {
			return new Waypoint(
					PacketByteBuf.readBlockPos(byteBuf), 
					TextCodecs.PACKET_CODEC.decode(byteBuf),
					byteBuf.readInt(),
					byteBuf.readBoolean()
			);
		}

		public void encode(ByteBuf byteBuf, Waypoint waypoint) {
			PacketByteBuf.writeBlockPos(byteBuf, waypoint.pos());
			TextCodecs.PACKET_CODEC.encode(byteBuf, waypoint.label);
			byteBuf.writeInt(waypoint.color);
			byteBuf.writeBoolean(waypoint.allowed);
		}
	};

}
