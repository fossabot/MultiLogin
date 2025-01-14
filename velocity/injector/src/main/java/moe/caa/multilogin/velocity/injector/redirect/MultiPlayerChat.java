package moe.caa.multilogin.velocity.injector.redirect;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.PlayerChat;
import io.netty.buffer.ByteBuf;
import moe.caa.multilogin.api.logger.LoggerProvider;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * 擦除聊天签名的包
 */
public class MultiPlayerChat extends PlayerChat {
    private static MethodHandle signatureFieldSetter;
    private static MethodHandle saltFieldSetter;
    private static MethodHandle previousMessagesFieldSetter;

    public static void init() throws NoSuchFieldException, IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Field signatureField = PlayerChat.class.getDeclaredField("signature");
        signatureField.setAccessible(true);
        MultiPlayerChat.signatureFieldSetter = lookup.unreflectSetter(signatureField);

        Field saltField = PlayerChat.class.getDeclaredField("salt");
        saltField.setAccessible(true);
        MultiPlayerChat.saltFieldSetter = lookup.unreflectSetter(saltField);

        Field previousMessagesField = PlayerChat.class.getDeclaredField("previousMessages");
        previousMessagesField.setAccessible(true);
        MultiPlayerChat.previousMessagesFieldSetter = lookup.unreflectSetter(previousMessagesField);
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        removeSignature();
        super.encode(buf, direction, protocolVersion);
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        removeSignature();
        return super.handle(handler);
    }

    private void removeSignature() {
        try {
            signatureFieldSetter.invoke(this, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
            saltFieldSetter.invoke(this, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
            previousMessagesFieldSetter.invoke(this, new SignaturePair[0]);
        } catch (Throwable throwable) {
            LoggerProvider.getLogger().error("An exception was encountered while clearing the chat signature.", throwable);
        }
    }
}
