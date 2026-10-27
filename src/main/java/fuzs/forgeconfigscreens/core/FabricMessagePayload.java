package fuzs.forgeconfigscreens.core;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FabricMessagePayload(ResourceLocation channelName, byte[] data) implements CustomPacketPayload {
    public static final Type<FabricMessagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("forgeconfigscreens", "message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FabricMessagePayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.channelName());
                buffer.writeByteArray(payload.data());
            },
            buffer -> new FabricMessagePayload(buffer.readResourceLocation(), buffer.readByteArray())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
