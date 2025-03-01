package com.bug1312.vortex;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bug1312.vortex.commands.ForceLandCommand;
import com.bug1312.vortex.commands.WhereIsCommand;
import com.bug1312.vortex.ducks.SignBlocksMixinDuck;
import com.bug1312.vortex.helpers.DimensionIdentifier;
import com.bug1312.vortex.helpers.VortexWorldState;
import com.bug1312.vortex.helpers.WaypointHelper;
import com.bug1312.vortex.packets.c2s.MoveDirectionPayload;
import com.bug1312.vortex.packets.c2s.MoveToWaypointPayload;
import com.bug1312.vortex.packets.s2c.RetrieveWaypointsPayload;
import com.bug1312.vortex.records.Waypoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.poi.PointOfInterestType;


// Shhh, I know it's a clustered mess here

public class VortexMod implements ModInitializer {
	
	public static final String MOD_ID = "vortex";
	
	public static final ConsoleBlock CONSOLE = register(new ConsoleBlock(AbstractBlock.Settings.create()), Identifier.of(MOD_ID, "console"), true);

	public static final BlockEntityType<ConsoleBlockEntity> CONSOLE_BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "console"), BlockEntityType.Builder.create(ConsoleBlockEntity::new, CONSOLE).build());
	
	private static final Set<BlockState> WAYPOINT_SIGNS = (Set<BlockState>)ImmutableList.of(
			Blocks.OAK_HANGING_SIGN,
			Blocks.SPRUCE_HANGING_SIGN,
			Blocks.BIRCH_HANGING_SIGN,
			Blocks.ACACIA_HANGING_SIGN, 
			Blocks.CHERRY_HANGING_SIGN, 
			Blocks.JUNGLE_HANGING_SIGN, 
			Blocks.DARK_OAK_HANGING_SIGN, 
			Blocks.CRIMSON_HANGING_SIGN, 
			Blocks.WARPED_HANGING_SIGN, 
			Blocks.MANGROVE_HANGING_SIGN, 
			Blocks.BAMBOO_HANGING_SIGN, 

			Blocks.OAK_WALL_SIGN,
			Blocks.SPRUCE_WALL_SIGN,
			Blocks.BIRCH_WALL_SIGN,
			Blocks.ACACIA_WALL_SIGN, 
			Blocks.CHERRY_WALL_SIGN, 
			Blocks.JUNGLE_WALL_SIGN, 
			Blocks.DARK_OAK_WALL_SIGN, 
			Blocks.CRIMSON_WALL_SIGN, 
			Blocks.WARPED_WALL_SIGN, 
			Blocks.MANGROVE_WALL_SIGN, 
			Blocks.BAMBOO_WALL_SIGN, 

			Blocks.OAK_WALL_HANGING_SIGN,
			Blocks.SPRUCE_WALL_HANGING_SIGN,
			Blocks.BIRCH_WALL_HANGING_SIGN,
			Blocks.ACACIA_WALL_HANGING_SIGN, 
			Blocks.CHERRY_WALL_HANGING_SIGN, 
			Blocks.JUNGLE_WALL_HANGING_SIGN, 
			Blocks.DARK_OAK_WALL_HANGING_SIGN, 
			Blocks.CRIMSON_WALL_HANGING_SIGN, 
			Blocks.WARPED_WALL_HANGING_SIGN, 
			Blocks.MANGROVE_WALL_HANGING_SIGN, 
			Blocks.BAMBOO_WALL_HANGING_SIGN
		)
		.stream()
		.flatMap(block -> block.getStateManager().getStates().stream())
		.filter(blockState -> blockState.get(SignBlocksMixinDuck.WAYPOINT_PROPERTY) == true)
		.collect(ImmutableSet.toImmutableSet());
	public static final PointOfInterestType WAYPOINT_POI = PointOfInterestHelper.register(Identifier.of(MOD_ID, "waypoint"), 1, 1, WAYPOINT_SIGNS);


	public static <T extends Block> T register(T block, Identifier blockId, boolean hasItem) {

		if (hasItem) {
			BlockItem blockItem = new BlockItem(block, new Item.Settings());
			Registry.register(Registries.ITEM, blockId, blockItem);
			ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
				entries.add(block.asItem());
			});
		}

		return Registry.register(Registries.BLOCK, blockId, block);
	}
	
	@Override
	public void onInitialize() {
		
		// Server -> Client Packet Registration
		PayloadTypeRegistry.playS2C().register(RetrieveWaypointsPayload.ID, RetrieveWaypointsPayload.CODEC);

		// Client -> Server Packet Registration & Handling
		PayloadTypeRegistry.playC2S().register(MoveDirectionPayload.ID, MoveDirectionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MoveToWaypointPayload.ID, MoveToWaypointPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MoveDirectionPayload.ID, PacketHandler::MoveDirection);
		ServerPlayNetworking.registerGlobalReceiver(MoveToWaypointPayload.ID, PacketHandler::MoveToWaypoint);

		ForceLandCommand.register();
		WhereIsCommand.register();
		
		// Handle player join events for vortex dimensions
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerWorld world = handler.player.getServerWorld();
						
			if (DimensionIdentifier.isJunk(world)) {
				VortexWorldState.WorldState worldState = VortexWorldState.getState(world);
				Pair<Optional<Waypoint>, List<Waypoint>> results = WaypointHelper.getWaypoints(server.getOverworld(), worldState.currentPos, worldState.flightRange);
				
				ServerPlayNetworking.send(handler.player, new RetrieveWaypointsPayload(results.getLeft().orElse(new Waypoint(worldState.currentPos, Text.empty(), DyeColor.WHITE.getSignColor())), results.getRight()));
			}
		});
	}

}