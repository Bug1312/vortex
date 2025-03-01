package com.bug1312.vortex.helpers;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Clearable;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.TickPriority;

public class StructureTransport {

	public static void transportStructure(
			ServerWorld fromWorld, BlockPos fromPos, 
			ServerWorld toWorld, BlockPos toPos, 
			Vec3i size, Identifier structureId, int replaceFlags, Random random
	) {
		BlockPos fromCornerPos = fromPos;
		BlockPos toCornerPos = fromCornerPos.add(size);
		Box areaBox = Box.enclosing(toCornerPos.add(-1,-1,-1), fromCornerPos);
		Iterable<BlockPos> areaIterator = BlockPos.iterate(toCornerPos.add(-1,-1,-1), fromCornerPos);
		
		try {
			StructureTemplateManager manager = fromWorld.getStructureTemplateManager();
			
			// Save structure from source location
			StructureTemplate structure = manager.getTemplateOrBlank(structureId);
			structure.saveFromWorld(fromWorld, fromCornerPos, size, false, null);

			// Get chunks that need updating
			ChunkPos fromChunk = fromWorld.getChunk(fromCornerPos).getPos();
			ChunkPos toChunk = fromWorld.getChunk(toCornerPos).getPos();
			Set<WorldChunk> chunks = new HashSet<>();
			for (int x = fromChunk.x; x <= toChunk.x; x++)
				for (int z = fromChunk.z; z <= toChunk.z; z++)
					chunks.add(fromWorld.getChunk(x, z));

			// Copy block/fluid ticks
			Map<BlockPos, Triple<Block, Integer, TickPriority>> blockTicks = new HashMap<>();
			Map<BlockPos, Triple<Fluid, Integer, TickPriority>> fluidTicks = new HashMap<>();
			chunks.forEach(chunk -> copyTicks(chunk, fromWorld, fromPos, toPos, areaBox, blockTicks, fluidTicks));

			// Place structure at destination
			BlockPos templatePos = toPos;//.add(nwdCornerRelative);
			structure.place(toWorld, templatePos, templatePos, new StructurePlacementData(), random, Block.NOTIFY_ALL);
			
			// Apply ticks at destination
			blockTicks.forEach((bp, triple) -> toWorld.scheduleBlockTick(bp, triple.getLeft(), triple.getMiddle(), triple.getRight()));
			fluidTicks.forEach((bp, triple) -> toWorld.scheduleFluidTick(bp, triple.getLeft(), triple.getMiddle(), triple.getRight()));
			
			manager.unloadTemplate(structureId);
			
			// Transport entities
			fromWorld.getEntitiesByClass(Entity.class, areaBox, (e) -> true).forEach(entity -> {
				Vec3d destPos = entity.getPos().add(
					toPos.getX()-fromPos.getX(),
					toPos.getY()-fromPos.getY(), 
					toPos.getZ()-fromPos.getZ()
				);
				try {
					// Try to use ImmPtl teleportation if available
					Class.forName("qouteall.imm_ptl.core.teleportation.ServerTeleportationManager");
					qouteall.imm_ptl.core.teleportation.ServerTeleportationManager.teleportEntityGeneral(entity, destPos, toWorld);
				} catch (ClassNotFoundException e) {
					// Fall back to vanilla teleportation
					if (entity instanceof ServerPlayerEntity player) {
						player.teleport(toWorld, destPos.x, destPos.y, destPos.z, entity.getYaw(), entity.getPitch());
					} else {
						entity.teleport(toWorld, destPos.x, destPos.y, destPos.z, EnumSet.noneOf(PositionFlag.class), entity.getYaw(), entity.getPitch());
					}
				}
			});
			
			// Clear source area
			BlockState barrier = Blocks.BARRIER.getDefaultState();
			BlockState air = Blocks.AIR.getDefaultState();

			// First pass: Replace with barriers, preserving supports
			areaIterator.forEach(bp -> {
				Clearable.clear(fromWorld.getBlockEntity(bp));
				fromWorld.setBlockState(bp, barrier, Block.NOTIFY_LISTENERS);
			});

			// Second pass: Replace with air
			areaIterator.forEach(bp -> {
				fromWorld.setBlockState(bp, air, Block.NOTIFY_ALL);
			});

		} catch (InvalidIdentifierException err) {
			// TODO: Log as error
		}
	}
	
	private static void copyTicks(WorldChunk chunk, ServerWorld world, BlockPos fromPos, BlockPos toPos, Box areaBox, Map<BlockPos, Triple<Block, Integer, TickPriority>> blockTicks, Map<BlockPos, Triple<Fluid, Integer, TickPriority>> fluidTicks) {
		
		ChunkTickScheduler<Block> blockScheduler = (ChunkTickScheduler<Block>) chunk.getBlockTickScheduler();
		ChunkTickScheduler<Fluid> fluidScheduler = (ChunkTickScheduler<Fluid>) chunk.getFluidTickScheduler();
		
		blockScheduler.getQueueAsStream().forEach(tick -> {
			if (!areaBox.contains(tick.pos().toCenterPos())) return;
			BlockPos newBp = tick.pos().add(
				toPos.getX()-fromPos.getX(),
				toPos.getY()-fromPos.getY(),
				toPos.getZ()-fromPos.getZ()
			);
			int newTime = (int) (tick.triggerTick() - world.getTime());
			blockTicks.put(newBp, Triple.of((Block) tick.type(), newTime, tick.priority()));
		});
		
		fluidScheduler.getQueueAsStream().forEach(tick -> {
			if (!areaBox.contains(tick.pos().toCenterPos())) return;
			BlockPos newBp = tick.pos().add(
				toPos.getX()-fromPos.getX(),
				toPos.getY()-fromPos.getY(),
				toPos.getZ()-fromPos.getZ()
			);
			int newTime = (int) (tick.triggerTick() - world.getTime());
			fluidTicks.put(newBp, Triple.of((Fluid) tick.type(), newTime, tick.priority()));
		});
	}
}
