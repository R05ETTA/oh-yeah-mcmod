package com.oh_yeah.renderer;

import com.oh_yeah.OhYeah;
import com.oh_yeah.entity.TiansuluoBattleFaceEntity;
import com.oh_yeah.entity.ModEntityTypes;
import com.oh_yeah.model.TiansuluoBattleFaceModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public final class TiansuluoBattleFaceRenderer extends MobEntityRenderer<TiansuluoBattleFaceEntity, TiansuluoBattleFaceModel> {
    private static final Identifier TEXTURE = Identifier.of(OhYeah.MOD_ID, "textures/entity/tiansuluo_battle_face.png");

    public TiansuluoBattleFaceRenderer(EntityRendererFactory.Context context) {
        super(context, new TiansuluoBattleFaceModel(context.getPart(TiansuluoBattleFaceModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    public Identifier getTexture(TiansuluoBattleFaceEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TiansuluoBattleFaceEntity entity, MatrixStack matrices, float amount) {
        super.scale(entity, matrices, amount);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        float scale = TiansuluoBattleFaceEntity.ADULT_RENDER_SCALE;
        if (entity.isBaby()) {
            scale *= TiansuluoBattleFaceEntity.BABY_SCALE_FACTOR;
        }
        matrices.scale(scale, scale, scale);
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntityTypes.TIANSULUO_BATTLE_FACE, TiansuluoBattleFaceRenderer::new);
    }
}
