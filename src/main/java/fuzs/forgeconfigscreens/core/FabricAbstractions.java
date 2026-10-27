package fuzs.forgeconfigscreens.core;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeModConfigEvents;
import fuzs.forgeconfigscreens.ForgeConfigScreens;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricAbstractions implements CommonAbstractions {
    private final Map<Class<? extends WritableMessage>, ResourceLocation> channelNames = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, ClientMessageDispatcher> clientHandlers = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, ServerMessageDispatcher> serverHandlers = new ConcurrentHashMap<>();
    private final AtomicInteger discriminator = new AtomicInteger();
    private volatile boolean payloadTypesRegistered;

    @Override
    public Optional<String> getModDisplayName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(ModContainer::getMetadata).map(ModMetadata::getName);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isClientEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public Path getDefaultConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void fireReloadingEvent(ModConfig modConfig) {
        ForgeModConfigEvents.reloading(modConfig.getModId()).invoker().onModConfigReloading(modConfig);
    }

    @Override
    public <T extends WritableMessage> void registerClientboundMessage(Class<T> clazz, Supplier<NetworkingHelper.ClientMessageListener<T>> listener) {
        ensurePayloadTypesRegistered();
        ResourceLocation channelName = registerChannelName(clazz);
        Function<FriendlyByteBuf, T> factory = CommonAbstractions.findMessageConstructor(clazz);
        clientHandlers.put(channelName, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> {
                if (client.player == null || client.level == null || client.getConnection() == null) return;
                T message = decode(factory, payload.data());
                listener.get().handle(message, client, client.getConnection(), client.player, client.level);
            });
        });
    }

    @Override
    public <T extends WritableMessage> void registerServerboundMessage(Class<T> clazz, Supplier<NetworkingHelper.ServerMessageListener<T>> listener) {
        ensurePayloadTypesRegistered();
        ResourceLocation channelName = registerChannelName(clazz);
        Function<FriendlyByteBuf, T> factory = CommonAbstractions.findMessageConstructor(clazz);
        serverHandlers.put(channelName, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            server.execute(() -> {
                T message = decode(factory, payload.data());
                listener.get().handle(message, server, player.connection, player, player.serverLevel());
            });
        });
    }

    private synchronized void ensurePayloadTypesRegistered() {
        if (this.payloadTypesRegistered) return;
        PayloadTypeRegistry.playS2C().register(FabricMessagePayload.TYPE, FabricMessagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FabricMessagePayload.TYPE, FabricMessagePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FabricMessagePayload.TYPE, (payload, context) -> {
            ServerMessageDispatcher handler = this.serverHandlers.get(payload.channelName());
            if (handler != null) handler.handle(payload, context);
        });
        if (this.isClientEnvironment()) {
            ClientPlayNetworking.registerGlobalReceiver(FabricMessagePayload.TYPE, (payload, context) -> {
                ClientMessageDispatcher handler = this.clientHandlers.get(payload.channelName());
                if (handler != null) handler.handle(payload, context);
            });
        }
        this.payloadTypesRegistered = true;
    }

    private ResourceLocation registerChannelName(Class<? extends WritableMessage> clazz) {
        ResourceLocation channelName = ForgeConfigScreens.id("play/" + this.discriminator.getAndIncrement());
        this.channelNames.put(clazz, channelName);
        return channelName;
    }

    private static <T extends WritableMessage> T decode(Function<FriendlyByteBuf, T> factory, byte[] data) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            return factory.apply(buffer);
        } finally {
            buffer.release();
        }
    }

    private FabricMessagePayload encode(WritableMessage message) {
        ResourceLocation channelName = this.channelNames.get(message.getClass());
        Objects.requireNonNull(channelName, "channel name for " + message.getClass() + " is null");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            message.write(buffer);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new FabricMessagePayload(channelName, data);
        } finally {
            buffer.release();
        }
    }

    @Override
    public Packet<?> toClientboundPacket(WritableMessage message) {
        return ServerPlayNetworking.createS2CPacket(this.encode(message));
    }

    @Override
    public Packet<?> toServerboundPacket(WritableMessage message) {
        return ClientPlayNetworking.createC2SPacket(this.encode(message));
    }

    @Override
    public void registerConfig(String modId, ModConfig.Type type, IConfigSpec<?> spec) {
        ForgeConfigRegistry.INSTANCE.register(modId, type, spec);
    }

    @Override
    public void registerConfig(String modId, ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
        ForgeConfigRegistry.INSTANCE.register(modId, type, spec, fileName);
    }

    @FunctionalInterface
    private interface ClientMessageDispatcher {
        void handle(FabricMessagePayload payload, ClientPlayNetworking.Context context);
    }

    @FunctionalInterface
    private interface ServerMessageDispatcher {
        void handle(FabricMessagePayload payload, ServerPlayNetworking.Context context);
    }
}
