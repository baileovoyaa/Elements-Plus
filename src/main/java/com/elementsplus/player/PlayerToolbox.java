package com.elementsplus.player;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlayerToolbox {

    public static final String GROUP_INPUT_OUTPUT = "input_output";
    public static final String GROUP_BUS = "bus";

    // ───────── nested type ─────────

    public static final class Group {
        @Nullable public String id;          // non-null ⇒ builtin
        public String name;                  // display name (custom = literal, builtin = ignored by rendering)
        public final List<ResourceLocation> components;

        public Group(@Nullable String id, String name, List<ResourceLocation> components) {
            this.id = id;
            this.name = name != null ? name : "";
            this.components = components != null ? components : new ArrayList<>();
        }

        public Group(@Nullable String id, String name, ResourceLocation... components) {
            this(id, name, new ArrayList<>(List.of(components)));
        }

        public Group(String name, ResourceLocation... components) {
            this(null, name, components);
        }

        public boolean isBuiltin() {
            return id != null;
        }

        public Component displayName() {
            if (isBuiltin()) {
                return Component.translatable("gui.elements-plus.toolbox.group." + id);
            }
            return Component.literal(name);
        }
    }

    // ───────── fields ─────────

    public final List<Group> groups;

    private PlayerToolbox(List<Group> groups) {
        this.groups = groups;
    }

    // ───────── defaults ─────────

    public static PlayerToolbox createDefault() {
        PlayerToolbox t = new PlayerToolbox(new ArrayList<>());
        t.addBuiltin(GROUP_INPUT_OUTPUT,
                BuiltinCircuitComponents.INPUT.getId(),
                BuiltinCircuitComponents.OUTPUT.getId());
        t.addBuiltin(GROUP_BUS,
                BuiltinCircuitComponents.BUS_JOINER_8.getId(),
                BuiltinCircuitComponents.BUS_SPLITTER_8.getId());
        return t;
    }

    // ───────── mutation helpers ─────────

    public void addBuiltin(@Nullable String id, ResourceLocation... components) {
        groups.add(new Group(id, "", components));
    }

    public void addCustomGroup(String name, int index) {
        groups.add(index, new Group(name));
    }

    public void addCustomGroup(String name) {
        groups.add(new Group(name));
    }

    public PlayerToolbox copy() {
        List<Group> ng = new ArrayList<>(groups.size());
        for (Group g : groups) ng.add(new Group(g.id, g.name, new ArrayList<>(g.components)));
        return new PlayerToolbox(ng);
    }

    // ───────── codec / stream codec ─────────

    private static final Codec<Group> GROUP_CODEC = RecordCodecBuilder.create(g -> g.group(
            Codec.STRING.optionalFieldOf("id").forGetter(gr -> Optional.ofNullable(gr.id).map(s -> s)),
            Codec.STRING.optionalFieldOf("name").forGetter(gr -> Optional.of(gr.name)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("components", List.of()).forGetter(gr -> gr.components)
    ).apply(g, (idOpt, nameOpt, comps) -> new Group(idOpt.orElse(null), nameOpt.orElse(""), new ArrayList<>(comps))));

    public static final Codec<PlayerToolbox> CODEC = RecordCodecBuilder.create(t -> t.group(
            GROUP_CODEC.listOf().optionalFieldOf("groups", List.of()).forGetter(toolbox -> toolbox.groups)
    ).apply(t, groups -> new PlayerToolbox(new ArrayList<>(groups))));

    // stream codec ── share helpers with the group encoder / decoder
    private static void writeGroup(net.minecraft.network.FriendlyByteBuf buf, Group group) {
        buf.writeUtf(group.id != null ? group.id : "");
        buf.writeUtf(group.name);
        buf.writeVarInt(group.components.size());
        for (ResourceLocation id : group.components) {
            ResourceLocation.STREAM_CODEC.encode(buf, id);
        }
    }

    private static Group readGroup(net.minecraft.network.FriendlyByteBuf buf) {
        String idRaw = buf.readUtf();
        String name = buf.readUtf();
        int size = buf.readVarInt();
        List<ResourceLocation> comps = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            comps.add(ResourceLocation.STREAM_CODEC.decode(buf));
        }
        return new Group(idRaw.isEmpty() ? null : idRaw, name, comps);
    }

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PlayerToolbox> STREAM_CODEC =
            StreamCodec.of(
                    (buf, toolbox) -> {
                        buf.writeVarInt(toolbox.groups.size());
                        for (Group g : toolbox.groups) writeGroup(buf, g);
                    },
                    buf -> {
                        int count = buf.readVarInt();
                        List<Group> groups = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) groups.add(readGroup(buf));
                        return new PlayerToolbox(groups);
                    }
            );

    // ───────── sanitize ─────────

    public void sanitize() {
        if (groups == null) return;
        groups.removeIf(g -> g.components.removeIf(id -> BuiltinCircuitComponents.byId(id) == null));
        for (Group g : groups) {
            if (g.isBuiltin() && g.components.isEmpty()) {
                g.components.clear();
                switch (g.id) {
                    case GROUP_INPUT_OUTPUT -> {
                        g.components.add(BuiltinCircuitComponents.INPUT.getId());
                        g.components.add(BuiltinCircuitComponents.OUTPUT.getId());
                    }
                    case GROUP_BUS -> {
                        g.components.add(BuiltinCircuitComponents.BUS_JOINER_8.getId());
                        g.components.add(BuiltinCircuitComponents.BUS_SPLITTER_8.getId());
                    }
                }
            }
        }
    }
}
