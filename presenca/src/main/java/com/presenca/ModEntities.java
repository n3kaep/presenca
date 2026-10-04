package com.presenca;

import net.fabricmc.fabric.api.entity.event.v1.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
    public static final EntityType<StalkerEntity> STALKER = Registry.register(
            Registries.ENTITY_TYPE, Presenca.id("stalker"),
            EntityType.Builder.create(StalkerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f)
                    .maxTrackingRange(12)
                    .build("stalker"));

    public static final Item STALKER_EGG = Registry.register(
            Registries.ITEM, Presenca.id("stalker_spawn_egg"),
            new SpawnEggItem(STALKER, 0x000000, 0xFFFFFF, new Item.Settings()));

    public static void register() {
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> e.add(STALKER_EGG));
    }
}
