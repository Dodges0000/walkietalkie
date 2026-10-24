package com.example.walkietalkie;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    /** Звук в момент, когда игрок зажал кнопку рации. */
    public static final SoundEvent PTT_START = register("ptt_start");
    /** Звук в момент, когда игрок отпустил кнопку рации. */
    public static final SoundEvent PTT_END = register("ptt_end");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(WalkieTalkieMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /** Вызывается из onInitialize(), чтобы поля инициализировались. */
    public static void init() {}

    private ModSounds() {}
}
