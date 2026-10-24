package com.example.walkietalkie.client;

import com.example.walkietalkie.RadioItem;
import com.example.walkietalkie.RadioStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class WalkieTalkieClient implements ClientModInitializer {
    private static KeyBinding pttKey;

    // что мы последний раз сообщили серверу (null = ещё ничего, нужно отправить)
    private static Boolean lastPtt = null;
    private static boolean lastPressed = false;

    @Override
    public void onInitializeClient() {
        ClientConfig.load();

        pttKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.walkietalkie.ptt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.walkietalkie"));

        // при входе на сервер заново сообщаем режим
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> lastPtt = null);

        ClientTickEvents.END_CLIENT_TICK.register(WalkieTalkieClient::tick);

        // /radiomode [voice|ptt]
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("radiomode")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("Режим рации: " + describe(ClientConfig.getMode())));
                            return 1;
                        })
                        .then(ClientCommandManager.literal("voice").executes(ctx -> {
                            setMode(ClientConfig.Mode.VOICE);
                            ctx.getSource().sendFeedback(Text.literal("Режим рации: " + describe(ClientConfig.Mode.VOICE)));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("ptt").executes(ctx -> {
                            setMode(ClientConfig.Mode.PUSH_TO_TALK);
                            ctx.getSource().sendFeedback(Text.literal("Режим рации: " + describe(ClientConfig.Mode.PUSH_TO_TALK)));
                            return 1;
                        }))));
    }

    private static void setMode(ClientConfig.Mode mode) {
        ClientConfig.setMode(mode);
        lastPtt = null; // отправить серверу сразу
    }

    private static String describe(ClientConfig.Mode mode) {
        return mode == ClientConfig.Mode.PUSH_TO_TALK
                ? "по кнопке (удерживай клавишу из настроек управления)"
                : "обычный (микрофон работает, пока рация в руке)";
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            lastPtt = null;
            lastPressed = false;
            return;
        }

        boolean ptt = ClientConfig.getMode() == ClientConfig.Mode.PUSH_TO_TALK;
        boolean holding = mc.player.getMainHandStack().getItem() instanceof RadioItem
                || mc.player.getOffHandStack().getItem() instanceof RadioItem;
        boolean pressed = ptt && holding && mc.currentScreen == null && pttKey.isPressed();

        if (lastPtt == null || lastPtt != ptt || pressed != lastPressed) {
            lastPtt = ptt;
            lastPressed = pressed;
            if (ClientPlayNetworking.canSend(RadioStatePayload.ID)) {
                ClientPlayNetworking.send(new RadioStatePayload(ptt, pressed));
            }
        }
    }
}
