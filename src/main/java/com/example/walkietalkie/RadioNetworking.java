package com.example.walkietalkie;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Серверная часть: запоминает режим каждого игрока и состояние кнопки, играет звуки начала/конца. */
public final class RadioNetworking {

    private record State(boolean pushToTalk, boolean pressed, int freq) {}

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RadioStatePayload.ID, RadioStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RadioStatePayload.ID,
                (payload, context) -> handle(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                STATES.remove(handler.getPlayer().getUuid()));
    }

    /**
     * Можно ли сейчас передавать голос в рацию.
     * Режим 1 (обычный) или клиент без мода: да. Режим 2 (кнопка): только пока кнопка зажата.
     */
    public static boolean canTransmit(UUID player) {
        State s = STATES.get(player);
        return s == null || !s.pushToTalk() || s.pressed();
    }

    private static void handle(ServerPlayerEntity player, RadioStatePayload p) {
        UUID id = player.getUuid();
        State old = STATES.get(id);
        boolean wasPressed = old != null && old.pressed();
        int freq = old != null ? old.freq() : 1;
        boolean wantPressed = p.pushToTalk() && p.pressed();

        if (wantPressed && !wasPressed) {
            // начало передачи: нужна включённая рация в руке
            ItemStack radio = RadioItem.getTransmitter(player);
            if (radio == null) {
                STATES.put(id, new State(p.pushToTalk(), false, freq));
                return;
            }
            freq = RadioItem.getFrequency(radio);
            STATES.put(id, new State(true, true, freq));
            broadcastSound(player, freq, ModSounds.PTT_START);
        } else if (!wantPressed && wasPressed) {
            // конец передачи (кнопку отпустили или режим сменили)
            STATES.put(id, new State(p.pushToTalk(), false, freq));
            broadcastSound(player, freq, ModSounds.PTT_END);
        } else {
            STATES.put(id, new State(p.pushToTalk(), wasPressed && wantPressed, freq));
        }
    }

    /** Звук слышат говорящий и все, кто принимает эту частоту (каждый только у себя). */
    private static void broadcastSound(ServerPlayerEntity sender, int freq, SoundEvent sound) {
        playTo(sender, sound, 0.8f);
        for (ServerPlayerEntity other : sender.getServer().getPlayerManager().getPlayerList()) {
            if (other != sender && RadioItem.canReceive(other, freq)) {
                playTo(other, sound, 0.5f);
            }
        }
    }

    private static void playTo(ServerPlayerEntity player, SoundEvent sound, float volume) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(sound), SoundCategory.PLAYERS,
                player.getX(), player.getY(), player.getZ(), volume, 1.0f,
                player.getWorld().getRandom().nextLong()));
    }

    private RadioNetworking() {}
}
