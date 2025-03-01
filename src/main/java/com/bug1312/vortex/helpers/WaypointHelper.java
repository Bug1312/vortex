package com.bug1312.vortex.helpers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.bug1312.vortex.VortexMod;
import com.bug1312.vortex.records.Waypoint;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;

public class WaypointHelper {

	public static Pair<Optional<Waypoint>, List<Waypoint>> getWaypoints(ServerWorld world, BlockPos searchOrigin, int range) {
		PointOfInterestStorage storage = world.getPointOfInterestStorage();
		 
		// Collect positions into a list first so we can use it multiple times
		List<BlockPos> positionsList = storage
				.getInCircle(
						poiType -> poiType.value().equals(VortexMod.WAYPOINT_POI),
						searchOrigin, 
						range, 
						PointOfInterestStorage.OccupationStatus.ANY
				).map(poi -> poi.getPos()).toList();
		
		Predicate<? super BlockPos> filter = (pos) -> pos.equals(searchOrigin);
		
		if (positionsList.stream().anyMatch(filter))
			return new Pair<Optional<Waypoint>, List<Waypoint>>(
					streamToWaypoints(world, positionsList.stream().filter(filter)).findFirst(), 
					streamToWaypoints(world, positionsList.stream().filter(pos -> !filter.test(pos))).toList()
			);
		
		return new Pair<Optional<Waypoint>, List<Waypoint>>(Optional.empty(), streamToWaypoints(world, positionsList.stream()).toList());
	}
	
	public static Waypoint toWaypoint(ServerWorld world, BlockPos pos) {
		BlockEntity be = world.getBlockEntity(pos);
		Text title;
		Integer color = null;
		
		// LOCK FOR BC25
		boolean allowed = (world.getBlockState(pos.down().down().down()).getBlock().equals(Blocks.RED_WOOL));
		
		if (be != null && be instanceof SignBlockEntity sign) {
			List<String> text = Arrays.asList(sign.getText(true).getMessages(true)).stream().map(t -> t.getLiteralString()).filter(s -> !s.trim().isBlank()).toList();
			
			if (text.size() == 0) {
				text = Arrays.asList(sign.getText(false).getMessages(true)).stream().map(t -> t.getLiteralString()).filter(s -> !s.trim().isBlank()).toList();
				if (text.size() == 0) {
					// No Text
					title = Text.literal(NameGenerator.genName(pos.asLong()));
				} else {
					// Back Text
					title = Text.literal(String.join("\n", text));
					color = sign.getText(false).getColor().getSignColor();
				}
			} else {
				// Front Text
				title = Text.literal(String.join("\n", text));
				color = sign.getText(true).getColor().getSignColor();
			}
			
			color = sign.getFrontText().getColor().getSignColor();
		} else {
			title = Text.literal(NameGenerator.genName(pos.asLong()));
		}
		
		if (color == null || color == DyeColor.BLACK.getSignColor()) color = DyeColor.WHITE.getSignColor();

		return new Waypoint(pos, title, color, allowed);
	}
	
	private static Stream<Waypoint> streamToWaypoints(ServerWorld world, Stream<BlockPos> positions) {   	
		return positions.map(bp -> toWaypoint(world, bp));
	}
}
