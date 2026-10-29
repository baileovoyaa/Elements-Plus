package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 服务端 -> 客户端：同步所有已编译电路的分离存储索引。 */
public record CircuitStorageSyncPayload(List<StoredCircuit> circuits) implements CustomPacketPayload {

    public static final Type<CircuitStorageSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "circuit_storage_sync"));

    public static final StreamCodec<FriendlyByteBuf, CircuitStorageSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.circuits().size());
                for (StoredCircuit circuit : payload.circuits()) {
                    net.minecraft.nbt.Tag encoded = StoredCircuit.CODEC.encodeStart(NbtOps.INSTANCE, circuit)
                            .result().orElseThrow(() -> new IllegalStateException("Failed to encode circuit sync"));
                    if (!(encoded instanceof CompoundTag tag)) {
                        throw new IllegalStateException("Encoded circuit sync is not a compound tag");
                    }
                    buf.writeNbt(tag);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                List<StoredCircuit> list = new ArrayList<>(count);
                // 按依赖顺序发送；解码时逐条放入索引，使嵌套自定义元件可解析。
                // 不能清空全局索引，否则集成服务器与客户端共用 JVM 时会丢失服务端数据。
                for (int i = 0; i < count; i++) {
                    CompoundTag tag = buf.readNbt();
                    if (tag == null) {
                        continue;
                    }
                    StoredCircuit.CODEC.parse(NbtOps.INSTANCE, tag).result().ifPresent(record -> {
                        list.add(record);
                        CircuitStorage.get().putTransient(record);
                    });
                }
                return new CircuitStorageSyncPayload(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
