package com.bug1312.vortex;

import java.util.List;
import java.util.Optional;

import com.bug1312.vortex.helpers.LandingHelper;
import com.bug1312.vortex.helpers.VortexWorldState;
import com.bug1312.vortex.helpers.WaypointHelper;
import com.bug1312.vortex.packets.c2s.MoveDirectionPayload;
import com.bug1312.vortex.packets.c2s.MoveToWaypointPayload;
import com.bug1312.vortex.packets.s2c.RetrieveWaypointsPayload;
import com.bug1312.vortex.records.Waypoint;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

public class PacketHandler {

	public static void MoveToWaypoint(MoveToWaypointPayload payload, ServerPlayNetworking.Context context) {
		MinecraftServer server = context.server();

		server.execute(() -> {
			ServerWorld vortexWorld = context.player().getServerWorld();
			VortexWorldState.WorldState state = VortexWorldState.getState(vortexWorld);
			Waypoint waypoint = payload.waypoint();

//			if (!state.currentPos.isWithinDistance(waypoint.pos(), state.flightRange)) return;

			ServerWorld newWorld = server.getOverworld();

			// TODO: Maybe not have this in the move, and instead make a landing for server
			// efficiency?
			BlockPos newPos = LandingHelper.findClosestLandingSpot(newWorld, waypoint.pos(), state.size, 50);
			if (newPos == null) return; // TODO: I want a user to know the server done did died

			if (waypoint.allowed()) state.currentPos = newPos;

			Waypoint proxyWaypoint = new Waypoint(newPos, waypoint.label(), waypoint.color());

			Pair<Optional<Waypoint>, List<Waypoint>> results = WaypointHelper.getWaypoints(newWorld, waypoint.pos(), state.flightRange);

			vortexWorld.getPlayers().forEach(player -> ServerPlayNetworking.send(player, new RetrieveWaypointsPayload(results.getLeft().orElse(proxyWaypoint), results.getRight())));
		});
	}

	public static void MoveDirection(MoveDirectionPayload payload, Context context) {
		MinecraftServer server = context.server();

		server.execute(() -> {
			ServerWorld vortexWorld = context.player().getServerWorld();
			VortexWorldState.WorldState state = VortexWorldState.getState(vortexWorld);
			float yaw = payload.direction();

			float distance = 100;
			
			BlockPos offset = new BlockPos(
				(int) (-Math.sin(Math.toRadians(yaw)) * distance),
				state.currentPos.getY(),
				(int) (Math.cos(Math.toRadians(yaw)) * distance)
			);

			ServerWorld newWorld = server.getOverworld();

			BlockPos newPos = state.currentPos.add(offset);

			// REMOVED: Logic to set pos
			
			Waypoint proxyWaypoint = new Waypoint(newPos, Text.empty(), DyeColor.WHITE.getSignColor());

			Pair<Optional<Waypoint>, List<Waypoint>> results = WaypointHelper.getWaypoints(newWorld, newPos, state.flightRange);

			vortexWorld.getPlayers().forEach(player -> ServerPlayNetworking.send(player, new RetrieveWaypointsPayload(results.getLeft().orElse(proxyWaypoint), results.getRight())));
		});

	}

}
