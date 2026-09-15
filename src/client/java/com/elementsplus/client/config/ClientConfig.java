package com.elementsplus.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("elements-plus.json");
    private static final ClientConfig INSTANCE = new ClientConfig();

    public boolean darkMode = true;
    public boolean muted = false;

    private ClientConfig() {
    }

    public static ClientConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (var reader = Files.newBufferedReader(PATH)) {
                ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
                if (loaded != null) {
                    INSTANCE.darkMode = loaded.darkMode;
                    INSTANCE.muted = loaded.muted;
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                // Keep defaults on failure
            }
        }
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (var writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            // Silently ignore — non-critical
        }
    }
}
