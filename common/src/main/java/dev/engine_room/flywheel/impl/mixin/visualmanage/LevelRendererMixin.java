package dev.engine_room.flywheel.impl.mixin.visualmanage;

import java.util.Iterator;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Iterators;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(LevelExtractor.class)
abstract class LevelRendererMixin {
	@Shadow
	@Nullable
	private ClientLevel level;

	/**
	 * This gets called when a block is marked for rerender by vanilla.
	 */
	@Inject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
	private void flywheel$checkUpdate(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
		if (level == null) {
			return;
		}

		VisualizationManager manager = VisualizationManager.get(level);
		if (manager == null) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}

		var blockEntities = manager.blockEntities();
		if (oldState != newState) {
			blockEntities.queueRemove(blockEntity);
			blockEntities.queueAdd(blockEntity);
		} else {
			// I don't think this is possible to reach in vanilla
			blockEntities.queueUpdate(blockEntity);
		}
	}

	@ModifyExpressionValue(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;"))
	private Iterator<Entity> flywheel$decideNotToRenderEntity(Iterator<Entity> original) {
		return Iterators.filter(original, entity -> !(VisualizationManager.supportsVisualization(entity.level()) &&
				VisualizationHelper.skipVanillaRender(entity)));
	}
}
