package com.bug1312.vortex.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.bug1312.vortex.ducks.VortexPilotHandlerDuck;
import com.bug1312.vortex.vortex.VortexPilotingClient;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity implements VortexPilotHandlerDuck {
		
	ClientPlayerEntity _this = (ClientPlayerEntity) ((Object) this);
	
	public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
		super(world, profile);
	}

	private Optional<Float> prevYaw = Optional.empty();
	private Optional<Float> prevPitch = Optional.empty();
	
	@Inject(method = "isCamera", at = @At(value = "HEAD"), cancellable = true)
	public void saveHeadRotationDontRotate(CallbackInfoReturnable<Boolean> ci) {
		if (VortexPilotingClient.isPiloting) {
			if (prevYaw.isEmpty() || prevPitch.isEmpty()) {
				this.prevYaw = Optional.of(this.getYaw());
				this.prevPitch = Optional.of(this.getPitch());
			}
		} else if (prevYaw.isPresent() && prevPitch.isPresent()) {	
			this.setYaw(prevYaw.get());
			this.setPitch(prevPitch.get());
			
			this.prevYaw = Optional.empty();
			this.prevPitch = Optional.empty();
		}
	}

	@Inject(
			at = @At(
					value = "INVOKE", 
					shift = Shift.AFTER,
					target = "Lnet/minecraft/client/input/Input;tick(ZF)V"
			),
			method = "tickMovement", 
			cancellable = true
	)
	public void tickMovement(CallbackInfo ci) {
		if (VortexPilotingClient.isPiloting) {
		
			MinecraftClient client = MinecraftClient.getInstance();
			
			client.player.input.jumping = false;
			client.player.input.movementForward = 0;
			client.player.input.movementSideways = 0;
			
			
			super.tickMovement();
			ci.cancel();
		}
	}
	
	@Override
	public void startPiloting() {		
		VortexPilotingClient.startPiloting();
	}
	
}
