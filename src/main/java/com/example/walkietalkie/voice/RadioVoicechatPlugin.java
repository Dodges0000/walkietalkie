package com.example.walkietalkie.voice;

import com.example.walkietalkie.RadioItem;
import com.example.walkietalkie.RadioNetworking;
import com.example.walkietalkie.WalkieTalkieMod;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RadioVoicechatPlugin implements VoicechatPlugin {

    private record ChannelKey(UUID sender, UUID receiver) {}

    /** Декодер/кодер/фильтры конкретного говорящего. */
    private static final class SenderState {
        final OpusDecoder decoder;
        final OpusEncoder encoder;
        final RadioEffect effect = new RadioEffect();

        SenderState(VoicechatServerApi api) {
            decoder = api.createDecoder();
            encoder = api.createEncoder();
        }

        byte[] process(byte[] opus) {
            short[] pcm = decoder.decode(opus);
            effect.apply(pcm);
            return encoder.encode(pcm);
        }

        void close() {
            decoder.close();
            encoder.close();
        }
    }

    private volatile VoicechatServerApi api;
    private final Map<UUID, SenderState> senders = new ConcurrentHashMap<>();
    private final Map<ChannelKey, StaticAudioChannel> channels = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return WalkieTalkieMod.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> api = e.getVoicechat());
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(PlayerDisconnectedEvent.class, e -> cleanup(e.getPlayerUuid()));
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatServerApi api = this.api;
        VoicechatConnection senderConn = event.getSenderConnection();
        if (api == null || senderConn == null) return;
        if (!(senderConn.getPlayer().getPlayer() instanceof ServerPlayerEntity sender)) return;

        UUID senderId = sender.getUuid();
        byte[] opus = event.getPacket().getOpusEncodedData();

        // Пустой пакет = конец фразы: сбрасываем состояние кодека
        if (opus == null || opus.length == 0) {
            SenderState st = senders.remove(senderId);
            if (st != null) st.close();
            return;
        }

        // Режим "по кнопке": пока кнопка не зажата, в рацию ничего не уходит
        if (!RadioNetworking.canTransmit(senderId)) return;

        ItemStack radio = RadioItem.getTransmitter(sender);
        if (radio == null) return; // рации нет — обычный голос по близости, мы не вмешиваемся
        int freq = RadioItem.getFrequency(radio);

        SenderState state = senders.computeIfAbsent(senderId, k -> new SenderState(api));
        byte[] processed;
        synchronized (state) {
            processed = state.process(opus);
        }

        for (ServerPlayerEntity receiver : new ArrayList<>(sender.getServer().getPlayerManager().getPlayerList())) {
            if (receiver == sender) continue;
            if (!RadioItem.canReceive(receiver, freq)) continue;

            VoicechatConnection conn = api.getConnectionOf(receiver.getUuid());
            if (conn == null || !conn.isConnected() || conn.isDisabled()) continue;

            StaticAudioChannel channel = channels.computeIfAbsent(
                    new ChannelKey(senderId, receiver.getUuid()),
                    k -> api.createStaticAudioChannel(UUID.randomUUID(),
                            api.fromServerLevel(receiver.getServerWorld()), conn));
            if (channel != null) channel.send(processed);
        }
    }

    private void cleanup(UUID player) {
        SenderState st = senders.remove(player);
        if (st != null) st.close();
        channels.keySet().removeIf(k -> k.sender().equals(player) || k.receiver().equals(player));
    }
}
