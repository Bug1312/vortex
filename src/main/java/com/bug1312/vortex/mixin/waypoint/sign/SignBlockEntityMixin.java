package com.bug1312.vortex.mixin.waypoint.sign;

import static com.bug1312.vortex.ducks.SignBlocksMixinDuck.WAYPOINT_PROPERTY;

import java.util.Optional;
import java.util.function.BiPredicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.Oxidizable;
import net.minecraft.block.Oxidizable.OxidationLevel;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.HoneycombItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus;
import net.minecraft.world.poi.PointOfInterestTypes;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private static void tick(World world, BlockPos pos, BlockState state, SignBlockEntity blockEntity, CallbackInfo ci) {		
		if (world.isClient()) return;
		if (!state.contains(WAYPOINT_PROPERTY)) return;
		if (world.getTime() % 20 != 0) return;
		
		Block block = state.getBlock();
		ServerWorld serverWorld = (ServerWorld) world;

		boolean newValue = false;
		if (block instanceof WallSignBlock) {
			BlockPos onPos = pos.offset(state.get(HorizontalFacingBlock.FACING).getOpposite());

			Optional<PointOfInterest> poi = findPoi(serverWorld, onPos);
			if (poi.isEmpty()) newValue = false;
			else newValue = climbCopper(serverWorld, onPos, poi.get().getPos());
			
		} else if (block instanceof WallHangingSignBlock) {
			BlockPos onPos1 = pos.offset(state.get(HorizontalFacingBlock.FACING).rotateYClockwise());
			BlockPos onPos2 = pos.offset(state.get(HorizontalFacingBlock.FACING).rotateYCounterclockwise());

			Optional<PointOfInterest> poi1 = findPoi(serverWorld, onPos1);
			Optional<PointOfInterest> poi2 = findPoi(serverWorld, onPos2);
			
			Optional<PointOfInterest> poi = Optional.empty();
			if (poi1.isPresent() && climbCopper(serverWorld, onPos1, poi1.get().getPos())) poi = poi1;
			else if (poi2.isPresent() && climbCopper(serverWorld, onPos2, poi2.get().getPos())) poi = poi2;
			
			if (poi.isEmpty()) newValue = false;
			else newValue = true;
			
		} else if (block instanceof HangingSignBlock) {
			BlockPos onPos = pos.up();

			Optional<PointOfInterest> poi = findPoi(serverWorld, onPos);
			if (poi.isEmpty()) newValue = false;
			else newValue = climbCopper(serverWorld, onPos, poi.get().getPos());

		} else return;
		
		if (newValue != state.get(WAYPOINT_PROPERTY).booleanValue())
			world.setBlockState(pos, state.with(WAYPOINT_PROPERTY, newValue));
	}
	
	private static Optional<PointOfInterest> findPoi(ServerWorld world, BlockPos onPos) {
		Chunk chunk = world.getChunk(onPos);
		return world.getPointOfInterestStorage()
				.getInChunk(poi -> poi.matchesKey(PointOfInterestTypes.LIGHTNING_ROD), chunk.getPos(), OccupationStatus.ANY)
				.filter(poi -> 
					poi.getPos().getX() == onPos.getX() &&
					poi.getPos().getY() >  onPos.getY() &&
					poi.getPos().getZ() == onPos.getZ()
				)
				.sorted((a, b) -> a.getPos().getY() - b.getPos().getY())
				.findFirst();
	}
	
	private static final BiPredicate<World, BlockPos> COPPER_CLIMBER = (world, pos) -> {
		BlockState state = world.getBlockState(pos);
		return (
			state.getBlock() instanceof Oxidizable block
			&& block.getDegradationLevel() != OxidationLevel.OXIDIZED
		) || (
			Optional.ofNullable(HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(state.getBlock())).isPresent()
		);
	};
	
	private static boolean climbCopper(World world, BlockPos start, BlockPos end) {
		BlockPos bottom = start;
		BlockPos top = end.down();
		
		while (bottom.getY() <= top.getY()) {
			if (!COPPER_CLIMBER.test(world, bottom) || !COPPER_CLIMBER.test(world, top)) return false;
			if (bottom.getY() == top.getY()) break;
			bottom = bottom.up();
			top = top.down();
		}
		
		return true;
	}

}
