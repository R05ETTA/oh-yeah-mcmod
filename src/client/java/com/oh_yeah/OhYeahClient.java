package com.oh_yeah;

import com.oh_yeah.model.TiansuluoBattleFaceModel;
import com.oh_yeah.model.TiansuluoPinkScarfModel;
import com.oh_yeah.model.SuxiaEntityModel;
import com.oh_yeah.renderer.TiansuluoBattleFaceRenderer;
import com.oh_yeah.renderer.TiansuluoPinkScarfRenderer;
import com.oh_yeah.renderer.SuxiaEntityRenderer;
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
		EntityModelLayerRegistry.registerModelLayer(TiansuluoPinkScarfModel.LAYER_LOCATION, TiansuluoPinkScarfModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(TiansuluoBattleFaceModel.LAYER_LOCATION, TiansuluoBattleFaceModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(SuxiaEntityModel.LAYER_LOCATION, SuxiaEntityModel::getTexturedModelData);
		TiansuluoPinkScarfRenderer.register();
		TiansuluoBattleFaceRenderer.register();
		SuxiaEntityRenderer.register();
		EntityRendererRegistry.register(ModEntityTypes.TIANSULUO_PINK_SCARF_PROJECTILE, FlyingItemEntityRenderer::new);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.player != null) {
				client.player.sendMessage(Text.literal("欢迎来到 略略世界。"), false);
			}
		});
	}
}
