package com.presenca;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** /presenca ... (nível de permissão 2 = OP). */
public class Commands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((d, access, env) -> d.register(
                literal("presenca").requires(s -> s.hasPermissionLevel(2))
                        .then(literal("spawn")
                                .executes(c -> spawn(c.getSource(), c.getSource().getPlayerOrThrow()))
                                .then(argument("alvo", EntityArgumentType.player())
                                        .executes(c -> spawn(c.getSource(), EntityArgumentType.getPlayer(c, "alvo")))))
                        .then(literal("limpar").executes(c -> {
                            int[] n = {0};
                            c.getSource().getServer().getWorlds().forEach(w ->
                                    w.getEntitiesByType(ModEntities.STALKER, e -> true).forEach(e -> { e.discard(); n[0]++; }));
                            c.getSource().sendFeedback(() -> Text.literal("Removidos " + n[0] + " Stalker(s)."), true);
                            return n[0];
                        }))
                        .then(literal("toggle").executes(c -> {
                            Config.get().enabled = !Config.get().enabled;
                            c.getSource().sendFeedback(() -> Text.literal("Sistema de terror: " + (Config.get().enabled ? "LIGADO" : "DESLIGADO")), true);
                            return 1;
                        }))
                        .then(literal("sanidade")
                                .then(literal("get").then(argument("alvo", EntityArgumentType.player()).executes(c -> {
                                    ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "alvo");
                                    c.getSource().sendFeedback(() -> Text.literal(p.getName().getString() + ": " + Math.round(Sanity.get(p)) + "/100"), false);
                                    return 1;
                                })))
                                .then(literal("set").then(argument("alvo", EntityArgumentType.player())
                                        .then(argument("valor", FloatArgumentType.floatArg(0, 100)).executes(c -> {
                                            ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "alvo");
                                            float v = FloatArgumentType.getFloat(c, "valor");
                                            Sanity.set(p, v);
                                            c.getSource().sendFeedback(() -> Text.literal("Sanidade de " + p.getName().getString() + " = " + v), true);
                                            return 1;
                                        })))))
                        .then(literal("evento")
                                .then(event("apagao", Sanity::eventBlackout))
                                .then(event("passos", Sanity::fakeFootstep))
                                .then(event("batimentos", Sanity::eventHeartbeat))
                                .then(event("sussurro", Sanity::eventWhisper))
                                .then(event("susto", Sanity::eventJumpscare)))
        ));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> event(String name, Consumer<ServerPlayerEntity> run) {
        return literal(name).then(argument("alvo", EntityArgumentType.player()).executes(c -> {
            ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "alvo");
            run.accept(p);
            c.getSource().sendFeedback(() -> Text.literal("Evento '" + name + "' em " + p.getName().getString()), true);
            return 1;
        }));
    }

    private static int spawn(ServerCommandSource src, ServerPlayerEntity p) throws CommandSyntaxException {
        StalkerEntity s = Sanity.spawnStalker(p, 12, 18, false);
        src.sendFeedback(() -> Text.literal(s != null ? "Stalker invocado perto de " + p.getName().getString() : "Sem espaço pra spawnar."), true);
        return s != null ? 1 : 0;
    }
}
