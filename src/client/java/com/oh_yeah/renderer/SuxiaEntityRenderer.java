package com.oh_yeah.renderer;

import com.oh_yeah.OhYeah;
import com.oh_yeah.entity.ModEntityTypes;
import com.oh_yeah.entity.SuxiaEntity;
import com.oh_yeah.model.SuxiaEntityModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SuxiaEntityRenderer extends MobEntityRenderer<SuxiaEntity, SuxiaEntityModel<SuxiaEntity>> {
    private static final Identifier TEXTURE = Identifier.of(OhYeah.MOD_ID, "textures/entity/suxia.png");
    private static final float MODEL_FORWARD_YAW_OFFSET_DEGREES = 90.0F;

    public SuxiaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SuxiaEntityModel<>(context.getPart(SuxiaEntityModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public Identifier getTexture(SuxiaEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void setupTransforms(SuxiaEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
        float tilt = entity.isTouchingWater()
                ? MathHelper.lerp(tickDelta, entity.prevTiltAngle, entity.tiltAngle)
                : 0.0F;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MODEL_FORWARD_YAW_OFFSET_DEGREES));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
    }

    @Override
    protected float getAnimationProgress(SuxiaEntity entity, float tickDelta) {
        return entity.age + tickDelta;
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntityTypes.SUXIA, SuxiaEntityRenderer::new);
    }
}
