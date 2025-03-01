package com.bug1312.vortex.helpers;

import com.bug1312.vortex.VortexMod;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class VortexWorldState {

	public static WorldState getState(ServerWorld world) {
		return WorldState.getServerState(world);
	}

	// State
	
	private static final String KEY_SIZE = "size";
	private static final String KEY_CURRENT_POS = "pos";
	private static final String KEY_RANGE = "range";

	public static class WorldState extends PersistentState {

		public Vec3i size;
		public BlockPos currentPos;
		public int flightRange = 200;
		
		private static WorldState getServerState(ServerWorld server) {
			PersistentStateManager persistentStateManager = server.getPersistentStateManager();
			WorldState StateSaverAndLoader = persistentStateManager.getOrCreate(type, VortexMod.MOD_ID);
			StateSaverAndLoader.markDirty();
			return StateSaverAndLoader;
		}
		
		@Override
		public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
			nbt.put(KEY_SIZE, NbtHelper.fromBlockPos(new BlockPos(size)));
			nbt.put(KEY_CURRENT_POS, NbtHelper.fromBlockPos(currentPos));
			nbt.putInt(KEY_RANGE, flightRange);
			return nbt;
		}
		
		private static WorldState createFromNbt(NbtCompound nbt, WrapperLookup registryLookup) {
			WorldState state = new WorldState();
				
			state.size = NbtHelper.toBlockPos(nbt, KEY_SIZE).orElse(new BlockPos(Constants.JUNK_SIZE));
			state.currentPos = NbtHelper.toBlockPos(nbt, KEY_CURRENT_POS).orElse(BlockPos.ORIGIN);
			state.flightRange = nbt.getInt(KEY_RANGE);
						
			return state;
		}
				
		private static PersistentState.Type<WorldState> type = new PersistentState.Type<>(WorldState::new, WorldState::createFromNbt, DataFixTypes.LEVEL);
	}

}
