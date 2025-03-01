package com.bug1312.vortex;

import org.jetbrains.annotations.Nullable;

import com.bug1312.vortex.helpers.Constants;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class ConsoleBlockEntity extends BlockEntity {

	public long startParticles = -1;
	
	@Nullable
	public BlockPos landing;
		
	public ConsoleBlockEntity(BlockPos pos, BlockState state) {
		super(VortexMod.CONSOLE_BLOCK_ENTITY_TYPE, pos, state);
	}
	
	@Override
	protected void writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
		if (landing != null) nbt.put("pos", NbtHelper.fromBlockPos(this.landing));
		super.writeNbt(nbt, registryLookup);
	}
	
	@Override
	protected void readNbt(NbtCompound nbt, WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		NbtHelper.toBlockPos(nbt, "pos").ifPresent(pos -> this.landing = pos);
	}
	
	public static void tickParticles(World world, BlockPos pos, BlockState state, ConsoleBlockEntity blockEntity) {
		if (world.isClient()) return;
		
		if (blockEntity.startParticles != -1 && (double) world.getTime() > (double) blockEntity.startParticles + (20D * 6D)) {
			blockEntity.startParticles = -1;
			blockEntity.markDirty();
			Block block = state.getBlock();
			if (block instanceof ConsoleBlock console) console.runTransport(state, (ServerWorld) world, pos, world.random);
		}
		
		if (blockEntity.startParticles == -1 || (double) world.getTime() > (double) blockEntity.startParticles + (20D * 6D)) return;
		
		blockEntity.spawnCubeParticles(Constants.JUNK_SIZE, Constants.JUNK_CENTER);
	}
		
	private void spawnCubeParticles(Vec3i size, Vec3i center) {
		if (world.isClient()) return;
		// Starts on the next tick
		if (world.getTime() != startParticles + 1 && world.getTime() % 12 != 0) return;

		int width = size.getX();
		int height = size.getY();
		int length = size.getZ();
				
		BlockPos position = pos;
				
		double xMin = position.getX() - (center.getX());
		double yMin = position.getY() - (center.getY());
		double zMin = position.getZ() - (center.getZ());
		
		double xMax = xMin + width;
		double yMax = yMin + height;
		double zMax = zMin + length;
		
		// There's 100% a better way to do this
		// Iterate over the edges of the cube
		for (double x = xMin; x <= xMax; x++) {
			for (double y = yMin; y <= yMax; y++) {
				for (double z = zMin; z <= zMax; z++) {
					// Only spawn particles on the edges of the cube
					boolean onXEdge = (x == xMin || x == xMax) && (y == yMin || y == yMax || z == zMin || z == zMax);
					boolean onYEdge = (y == yMin || y == yMax) && (x == xMin || x == xMax || z == zMin || z == zMax);
					boolean onZEdge = (z == zMin || z == zMax) && (x == xMin || x == xMax || y == yMin || y == yMax);
					
					if (onXEdge || onYEdge || onZEdge) {
						// Spawn the particle at the edge position
						((ServerWorld) world).spawnParticles(
								ParticleTypes.WAX_OFF, // Replace with your particle type
								x, // Offset to center the particle
								y,
								z,
								1, // Number of particles
								0.01, 0.01, 0.01, // Spread (velocity in x, y, z)
								1 // Speed multiplier
						);
					}
				}
			}
		}

	}

}
