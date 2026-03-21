package com.oh_yeah;

import com.oh_yeah.model.TiansuluoModel;
import com.oh_yeah.renderer.TiansuluoRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.text.Text;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import com.oh_yeah.entity.ModEntityTypes;

public class OhYeahClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(TiansuluoModel.LAYER_LOCATION, TiansuluoModel::getTexturedModelData);
		TiansuluoRenderer.register();
		EntityRendererRegistry.register(ModEntityTypes.TIANSULUO_PROJECTILE, FlyingItemEntityRenderer::new);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.player != null) {
				client.player.sendMessage(Text.literal("欢迎来到 略略 世界。"), false);
			}
		});
	}
}
