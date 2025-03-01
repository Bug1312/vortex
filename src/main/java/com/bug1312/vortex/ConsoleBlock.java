package com.bug1312.vortex;

import static net.minecraft.state.property.Properties.POWERED;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.bug1312.vortex.ducks.VortexPilotHandlerDuck;
import com.bug1312.vortex.helpers.Constants;
import com.bug1312.vortex.helpers.DimensionIdentifier;
import com.bug1312.vortex.helpers.StructureTransport;
import com.bug1312.vortex.helpers.VortexWorldState;
import com.bug1312.vortex.helpers.WaypointHelper;
import com.bug1312.vortex.packets.s2c.RetrieveWaypointsPayload;
import com.bug1312.vortex.records.Waypoint;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;



public class ConsoleBlock extends BlockWithEntity {
	public static final MapCodec<ConsoleBlock> CODEC = ConsoleBlock.createCodec(ConsoleBlock::new);
	@Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

	@Override @Nullable public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new ConsoleBlockEntity(pos, state); }
		
	public ConsoleBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getDefaultState().with(POWERED, Boolean.valueOf(false)));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(POWERED);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}
	
	@Override @Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (!world.isClient()) return validateTicker(type, VortexMod.CONSOLE_BLOCK_ENTITY_TYPE, ConsoleBlockEntity::tickParticles);
		return null;
	}

	public void runTransport(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.isClient()) return;

		MinecraftServer server = world.getServer();
		
		boolean powered = world.isReceivingRedstonePower(pos);

		boolean isJunkInVortex = DimensionIdentifier.isJunk(world);
		
		if (powered) {
			if (isJunkInVortex) return; /* Already in flight */
			else { /* Junk out of vortex */
				Identifier id = Identifier.of(VortexMod.MOD_ID, Constants.PREFIX_JUNK);
				
				Optional<RegistryKey<World>> newWorldKey = server.getWorldRegistryKeys().stream()
					.filter(key -> key.getValue().equals(id))
					.findFirst();
						
				if (newWorldKey.isEmpty()) return;
				
				ServerWorld newWorld = server.getWorld(newWorldKey.orElseThrow());
				StructureTransport.transportStructure(
						world, pos.subtract(Constants.JUNK_CENTER), 
						newWorld, Constants.TARDIS_CENTER, // Adding means it all is positive
						Constants.JUNK_SIZE, id, NOTIFY_ALL_AND_REDRAW, random
				);
				
				VortexWorldState.WorldState worldState = VortexWorldState.getState(newWorld);
				worldState.size = Constants.JUNK_SIZE;
				worldState.currentPos = pos.subtract(Constants.JUNK_CENTER);
				
				Pair<Optional<Waypoint>, List<Waypoint>> results = WaypointHelper.getWaypoints(world, worldState.currentPos, worldState.flightRange);
			
				newWorld.getPlayers().forEach(player -> ServerPlayNetworking.send(player, new RetrieveWaypointsPayload(results.getLeft().orElse(new Waypoint(worldState.currentPos, Text.empty(), DyeColor.WHITE.getSignColor())), results.getRight())));
			}
		} else {
			if (isJunkInVortex) {			
				Identifier id = Identifier.of(VortexMod.MOD_ID, Constants.PREFIX_JUNK);
				
				StructureTransport.transportStructure(
						world, pos.subtract(Constants.JUNK_CENTER),
						// TODO: Make newWorld dynamic
						server.getOverworld(), VortexWorldState.getState(world).currentPos, 
						Constants.JUNK_SIZE, id, NOTIFY_ALL_AND_REDRAW, random
				);				
			} else { /* Junk out of vortex */ }
		}
	}


	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		if (world.isClient()) return;

		boolean current = state.get(POWERED);
		boolean powered = world.isReceivingRedstonePower(pos);
		if (current != powered)	{
			world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
			
			boolean isJunkInVortex = DimensionIdentifier.isJunk(world);
			
				BlockEntity _be = world.getBlockEntity(pos);
				if (_be != null && _be instanceof ConsoleBlockEntity be) {
					if (isJunkInVortex) be.startParticles = powered ? -1 : world.getTime();
					else be.startParticles = powered ? world.getTime() : -1;
						
					be.markDirty();
				}
		}
	}
	
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient() && state.get(POWERED).booleanValue()) {
			boolean isJunkInVortex = DimensionIdentifier.isJunk(world);
						
			if (isJunkInVortex) {
				((VortexPilotHandlerDuck) player).startPiloting();
				return ActionResult.CONSUME;
			}
			
		} else {
			//
		}
		
		return ActionResult.SUCCESS;
	}



}
