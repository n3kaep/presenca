package com.presenca;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Config em config/presenca.json (gerado na primeira execução). */
public class Config {
    public boolean enabled = true;
    public double darkLoss = 0.5;        // sanidade perdida por segundo no escuro
    public double aloneLoss = 0.3;       // por segundo estando sozinho
    public double stalkerLoss = 1.5;     // por segundo com o Stalker perto
    public double recover = 0.5;         // recuperação por segundo (luz + companhia)
    public double spawnChance = 0.03;    // chance por segundo de spawnar com sanidade < 30
    public double hitSanityLoss = 15;    // sanidade perdida ao ser atingido
    public int lookTicks = 40;           // ticks sendo olhado até ele sumir
    public int stalkerLifetimeTicks = 6000;

    private static Config INSTANCE = new Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config get() { return INSTANCE; }

    public static void load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("presenca.json");
        try {
            if (Files.exists(p)) {
                INSTANCE = GSON.fromJson(Files.readString(p), Config.class);
            } else {
                Files.writeString(p, GSON.toJson(INSTANCE));
            }
        } catch (Exception e) {
            System.err.println("[Presenca] Erro na config, usando padrão: " + e);
        }
    }
}
