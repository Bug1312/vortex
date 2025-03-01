package com.bug1312.vortex.commands;

import com.bug1312.vortex.helpers.VortexWorldState;
import com.bug1312.vortex.helpers.VortexWorldState.WorldState;
import com.mojang.brigadier.Command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class WhereIsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("vortex-whereis")
                .then(CommandManager.argument("dim1", DimensionArgumentType.dimension())
                .executes(context -> {
                    return execute(context.getSource(),
                		DimensionArgumentType.getDimensionArgument(context, "dim1"));
                })));
        });
    }

	private static int execute(ServerCommandSource source, ServerWorld vortex) {
		WorldState state = VortexWorldState.getState(vortex);
		if (state.currentPos == null) return 0;
		
		source.sendFeedback(() -> Text.literal(state.currentPos.toShortString()), false);
		return Command.SINGLE_SUCCESS;
	}

	
}
