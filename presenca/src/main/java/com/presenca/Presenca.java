package com.presenca;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Presenca implements ModInitializer {
    public static final String ID = "presenca";

    public static Identifier id(String path) {
        return Identifier.of(ID, path);
    }

    @Override
    public void onInitialize() {
        Config.load();
        ModEntities.register();
        Sanity.init();
        Commands.register();
    }
}
