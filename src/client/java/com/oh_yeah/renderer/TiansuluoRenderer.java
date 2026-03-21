package com.oh_yeah.renderer;

import com.oh_yeah.OhYeah;
import com.oh_yeah.entity.ModEntityTypes;
import com.oh_yeah.entity.TiansuluoEntity;
import com.oh_yeah.model.TiansuluoModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class TiansuluoRenderer extends MobEntityRenderer<TiansuluoEntity, TiansuluoModel> {
	private static final Identifier DEFAULT_TEXTURE = Identifier.of(OhYeah.MOD_ID, "textures/entity/tiansuluo.png");

	public TiansuluoRenderer(EntityRendererFactory.Context context) {
		super(context, new TiansuluoModel(context.getPart(TiansuluoModel.LAYER_LOCATION)), 0.7F);
	}

	@Override
	public Identifier getTexture(TiansuluoEntity entity) {
		Identifier textureId = entity.getTextureId();
		return textureId != null ? textureId : DEFAULT_TEXTURE;
	}

	@Override
	protected void scale(TiansuluoEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
		if (entity.isBaby()) {
			matrices.scale(0.55F, 0.55F, 0.55F);
		}
	}

	public static void register() {
		EntityRendererRegistry.register(ModEntityTypes.TIANSULUO, TiansuluoRenderer::new);
	}
}
