package dev.engine_room.flywheel.impl.mixin;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.FlwImplXplat;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;

@Mixin(value = LevelRenderer.class, priority = 1001) // Higher priority to go after Sodium
abstract class LevelRendererMixin {
	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Shadow
	@Final
	private LevelRenderState levelRenderState;

	@Unique
	@Nullable
	private RenderContextImpl flywheel$renderContext;

	@Inject(method = "render", at = @At("HEAD"))
	private void flywheel$beginRender(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		Matrix4f projectionMatrix = RenderContextImpl.PROJECTION_MATRIX_NO_BOB.get();
		flywheel$renderContext = RenderContextImpl.create((LevelRenderer) (Object) this, level, renderBuffers, modelViewMatrix, projectionMatrix, cameraState, levelRenderState, deltaTracker.getGameTimeDeltaPartialTick(false));

		VisualizationManager manager = VisualizationManager.get(level);
		if (manager != null) {
			manager.renderDispatcher().onStartLevelRender(flywheel$renderContext);
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void flywheel$endRender(CallbackInfo ci) {
		flywheel$renderContext = null;
	}

	@Inject(method = "resetLevelRenderData", at = @At("RETURN"))
	private void flywheel$reload(CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			FlwImplXplat.INSTANCE.dispatchReloadLevelRendererEvent(level);
		}
	}

	@Inject(method = "submitFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
	private void flywheel$beforeBlockEntities(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			ClientLevel level = Minecraft.getInstance().level;
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().afterSubmitEntities(flywheel$renderContext);
			}
		}
	}

	@Inject(method = "submitFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitBlockDestroyAnimation(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"))
	private void flywheel$beforeRenderCrumbling(CallbackInfo ci) {
		if (flywheel$renderContext != null) {
			ClientLevel level = Minecraft.getInstance().level;
			VisualizationManager manager = VisualizationManager.get(level);
			if (manager != null) {
				manager.renderDispatcher().beforeSubmitCrumbling(flywheel$renderContext);
			}
		}
	}
}
