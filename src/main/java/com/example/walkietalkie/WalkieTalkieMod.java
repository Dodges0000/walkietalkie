package com.example.walkietalkie;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class WalkieTalkieMod implements ModInitializer {
    public static final String MOD_ID = "walkietalkie";

    public static final Item RADIO = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "radio"),
            new RadioItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ModSounds.init();
        RadioNetworking.register();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(RADIO));
    }
}
