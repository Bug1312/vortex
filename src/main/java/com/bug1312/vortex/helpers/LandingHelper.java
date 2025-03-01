package com.bug1312.vortex.helpers;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

public class LandingHelper {

	private static boolean canLandOnBlock(ServerWorld world, BlockPos pos, Vec3i size) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		
		int minX = pos.getX();
		int minY = pos.getY();
		int minZ = pos.getZ();
		int maxX = pos.getX() + size.getX();
		int maxY = pos.getY() + size.getY();
		int maxZ = pos.getZ() + size.getZ();
		
		// Batch chunk loading for better performance
		world.getChunkManager().addTicket(ChunkTicketType.UNKNOWN, new ChunkPos(minX >> 4, minZ >> 4), 1, new ChunkPos(minX >> 4, minZ >> 4));
		world.getChunkManager().addTicket(ChunkTicketType.UNKNOWN, new ChunkPos(maxX >> 4, maxZ >> 4), 1, new ChunkPos(maxX >> 4, maxZ >> 4));
		
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					mutable.set(x, y, z);
					
					var blockState = world.getBlockState(mutable);
					
					if (blockState.isReplaceable() || blockState.isAir()) {
						continue;
					}
					
					return false;
				}
			}
		}
		
		return true;
	}

	public static BlockPos findClosestLandingSpot(ServerWorld world, BlockPos pos, Vec3i size, int maxSearchRadius) {
		BlockPos.Mutable checkPos = new BlockPos.Mutable();
		BlockPos.Mutable groundPos = new BlockPos.Mutable();
		
		int structureSizeX = size.getX();
		int structureSizeZ = size.getZ();
		
		// Track chunks to avoid redundant loading
		Set<Long> loadedChunks = new HashSet<>();
		
		for (int radius = 0; radius <= maxSearchRadius; radius++) {
			int minX = pos.getX() - radius;
			int maxX = pos.getX() + radius;
			int minZ = pos.getZ() - radius;
			int maxZ = pos.getZ() + radius;
			
			for (int x = minX >> 4; x <= (maxX + structureSizeX) >> 4; x++) {
				for (int z = minZ >> 4; z <= (maxZ + structureSizeZ) >> 4; z++) {
					long chunkPos = ChunkPos.toLong(x, z);
					if (loadedChunks.add(chunkPos)) {
						world.getChunkManager().addTicket(ChunkTicketType.UNKNOWN, new ChunkPos(x, z), 1, new ChunkPos(x, z));
					}
				}
			}
			
			if (radius > 0) {
				for (int x = minX; x <= maxX; x++) {
					if (checkGroundPosition(world, x, minZ, pos.getY(), checkPos, groundPos, size)) return checkPos.toImmutable();
					if (checkGroundPosition(world, x, maxZ, pos.getY(), checkPos, groundPos, size)) return checkPos.toImmutable();
				}
				
				for (int z = minZ + 1; z < maxZ; z++) {
					if (checkGroundPosition(world, minX, z, pos.getY(), checkPos, groundPos, size)) return checkPos.toImmutable();
					if (checkGroundPosition(world, maxX, z, pos.getY(), checkPos, groundPos, size)) return checkPos.toImmutable();
				}
			} else {
				if (checkGroundPosition(world, pos.getX(), pos.getZ(), pos.getY(), checkPos, groundPos, size)) return checkPos.toImmutable();
			}
		}
		
		return null;
	}
	
	private static boolean checkGroundPosition(ServerWorld world, int x, int z, int startY, BlockPos.Mutable checkPos, BlockPos.Mutable groundPos, Vec3i size) {
		groundPos.set(x, startY, z);
		
		while (groundPos.getY() > world.getBottomY()) {
			groundPos.move(0, -1, 0);
			if (!world.getBlockState(groundPos).isReplaceable()) {
				checkPos.set(x, groundPos.getY() + 1, z);
				return canLandOnBlock(world, checkPos, size);
			}
		}
		
		return false;
	}
}
