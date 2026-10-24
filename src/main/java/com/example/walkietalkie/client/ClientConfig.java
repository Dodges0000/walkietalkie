package com.example.walkietalkie.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Клиентская настройка режима рации. Файл: config/walkietalkie-client.properties */
public final class ClientConfig {
    public enum Mode { VOICE, PUSH_TO_TALK }

    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("walkietalkie-client.properties");

    private static Mode mode = Mode.VOICE;

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode newMode) {
        mode = newMode;
        save();
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }
        mode = "ptt".equalsIgnoreCase(props.getProperty("mode", "voice")) ? Mode.PUSH_TO_TALK : Mode.VOICE;
    }

    private static void save() {
        Properties props = new Properties();
        props.setProperty("mode", mode == Mode.PUSH_TO_TALK ? "ptt" : "voice");
        try (OutputStream out = Files.newOutputStream(FILE)) {
            props.store(out, "Walkie Talkie client settings. mode = voice | ptt");
        } catch (IOException ignored) {
        }
    }

    private ClientConfig() {}
}
