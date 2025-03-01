package com.bug1312.vortex.mixin.waypoint.sign;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bug1312.vortex.ducks.SignBlocksMixinDuck;

import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.state.StateManager;

@Mixin({ WallSignBlock.class, WallHangingSignBlock.class, HangingSignBlock.class })
public abstract class SignBlocksMixin extends AbstractSignBlock implements SignBlocksMixinDuck {
	protected SignBlocksMixin(WoodType type, Settings settings) { super(type, settings); }

	@Inject(method = "<init>", at = @At("RETURN"))
	public void appendConstructor(CallbackInfo ci) {
		this.setDefaultState(this.stateManager.getDefaultState().with(WAYPOINT_PROPERTY, false));
	}

	@Inject(method = "appendProperties", at = @At("RETURN"))
	private void appendProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
		builder.add(WAYPOINT_PROPERTY);
	}

}
