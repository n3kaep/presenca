package com.presenca.client;

import com.presenca.Presenca;
import com.presenca.StalkerEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/** Usa o modelo humanoide do jogador (estilo Steve/Herobrine) com textura própria. */
public class StalkerRenderer extends BipedEntityRenderer<StalkerEntity, BipedEntityModel<StalkerEntity>> {
    private static final Identifier TEXTURE = Presenca.id("textures/entity/stalker.png");

    public StalkerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(StalkerEntity entity) {
        return TEXTURE;
    }
}
