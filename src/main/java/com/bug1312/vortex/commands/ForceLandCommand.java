package com.bug1312.vortex.commands;

import com.bug1312.vortex.VortexMod;
import com.bug1312.vortex.helpers.Constants;
import com.bug1312.vortex.helpers.StructureTransport;
import com.mojang.brigadier.Command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ForceLandCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("vortex-forceland")
                .then(CommandManager.argument("dim1", DimensionArgumentType.dimension())
                .then(CommandManager.argument("dim2", DimensionArgumentType.dimension())
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                .executes(context -> {
                    return execute(context.getSource(),
                		DimensionArgumentType.getDimensionArgument(context, "dim1"),
                		DimensionArgumentType.getDimensionArgument(context, "dim2"),
                        BlockPosArgumentType.getBlockPos(context, "pos"));
                })))));
        });
    }

	private static int execute(ServerCommandSource source, ServerWorld vortex, ServerWorld landing, BlockPos landingPos) {
		Identifier id = Identifier.of(VortexMod.MOD_ID, Constants.PREFIX_JUNK);
		
		StructureTransport.transportStructure(
				vortex, Constants.TARDIS_CENTER,
				landing, landingPos, 
				Constants.JUNK_SIZE, id, Block.REDRAW_ON_MAIN_THREAD, vortex.random
		);
								
		return Command.SINGLE_SUCCESS;
	}
}
