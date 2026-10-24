package com.example.walkietalkie;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Клиент -> сервер: выбранный режим рации и состояние кнопки "говорить". */
public record RadioStatePayload(boolean pushToTalk, boolean pressed) implements CustomPayload {
    public static final Id<RadioStatePayload> ID =
            new Id<>(Identifier.of(WalkieTalkieMod.MOD_ID, "radio_state"));

    public static final PacketCodec<RegistryByteBuf, RadioStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, RadioStatePayload::pushToTalk,
            PacketCodecs.BOOL, RadioStatePayload::pressed,
            RadioStatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
