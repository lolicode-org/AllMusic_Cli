package org.lolicode.nekomusic_cli;

import org.lolicode.nekomusic_cli.config.ModConfig;
import org.lolicode.nekomusic_cli.hud.HudUtils;
import org.lolicode.nekomusic_cli.packet.ListPacketReceiver;
import org.lolicode.nekomusic_cli.packet.MetadataPacketReceiver;
import org.lolicode.nekomusic_cli.player.APlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import lol.bai.badpackets.api.PacketSender;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NekoMusic_Cli implements ClientModInitializer {
    public static final Identifier ID = new Identifier("nekomusic", "channel");
    public static final Identifier ALLMUSIC_COMPAT_ID = new Identifier("allmusic", "channel");
    public static final String NEKO_ID = "nekomusic";
    public static APlayer nowPlaying;
    public static String currentImg = "";
    public static boolean isPlay = false;
    public static HudUtils hudUtils;
    private static int ang = 0;
    private static int count = 0;
    private static KeyBinding globalDisableKeyBinding;
    private static KeyBinding serverDisableKeyBinding;

    private static ScheduledExecutorService service;

    public static ModConfig config;

    public static void onServerQuit() {
        try {
            nowPlaying.close();
            nowPlaying.closePlayer();
            hudUtils.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        hudUtils.Lyric = hudUtils.Info = hudUtils.List = "";
        hudUtils.haveImg = false;
        hudUtils.save = null;
    }

    public static void PacketProcess(PacketByteBuf buf) {
        try {
            byte[] buff = new byte[buf.readableBytes()];
            buf.readBytes(buff);
            buff[0] = 0;
            String data = new String(buff, StandardCharsets.UTF_8).substring(1);
//            System.out.println(data);
            onClientPacket(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onClientPacket(final String message) {
        new Thread(() -> {
            try {
                // Record current img so we can get it back after altering the hud
                // Even when we're not playing, we still need to update this, or we'll get wrong results after the server starts playing next music
                if (message.startsWith("[Img]")) {
                    currentImg = message.substring(5);
                }
                if (!config.enabled ||
                        config.bannedServers.contains(Objects.requireNonNull(
                                MinecraftClient.getInstance().getCurrentServerEntry()).address)) {
                    return;
                }
                if (hudUtils.save == null) {
                    hudUtils.setPos(config);
                }
                if (message.equals("[Stop]")) {
                    stopPlaying();
                    currentImg = null;  // Clear image cache so it will be reloaded for next song.
                    // Have to do this here because the fucker server sometimes sends next image before sending next song.
                    // If we clear it after we get [play] it might clear current image
                } else if (message.startsWith("[Play]")) {
                    MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.MUSIC);
                    MinecraftClient.getInstance().getSoundManager().stopSounds(null, SoundCategory.RECORDS);
                    stopPlaying();
                    nowPlaying.setMusic(message.replace("[Play]", ""));
                } else if (message.startsWith("[Img]") && config.enableHudImg) {
                    hudUtils.setImg(message.substring(5));  // the server will only send img once (right before or after sending song), no need to check if is playing
                } else if (!nowPlaying.isClose()) {  // Don't accept hud until next song
                    if (message.startsWith("[Lyric]") && config.enableHudLyric) {
                        hudUtils.Lyric = message.substring(7);
                    } else if (message.startsWith("[Info]") && config.enableHudInfo) {
                        hudUtils.Info = message.substring(6);
                    } else if (message.startsWith("[List]") && config.enableHudList) {
                        hudUtils.List = message.substring(6);
                    } else if (message.startsWith("[Pos]")) {
                        nowPlaying.set(message.substring(5));
                    } else if (message.equalsIgnoreCase("[clear]")) {
                        hudUtils.close();
//                } else if (message.startsWith("{")) {
//                    hudUtils.setPos(message);
                        // Ignore server position
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "nekomusic").start();
    }

    private static void stopPlaying() {
        try {
            nowPlaying.closePlayer();
            hudUtils.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final MatrixStack stack = new MatrixStack();

    public static void drawText(String item, float x, float y) {
        var hud = MinecraftClient.getInstance().textRenderer;
        hud.draw(stack, item, x, y, 0xffffff);
    }

    public static void drawPic(int textureID, int size, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, textureID);

        MatrixStack stack = new MatrixStack();
        Matrix4f matrix = stack.peek().getPositionMatrix();

        int a = size / 2;


        if (hudUtils.save.EnablePicRotate && hudUtils.thisRoute) {
            matrix = matrix.translationRotate(x + a, y + a, 0,
                    new Quaternionf().fromAxisAngleDeg(0, 0, 1, ang));
        } else {
            matrix = matrix.translation(x + a, y + a, 0);
        }
        int x0 = -a;
        int x1 = a;
        int y0 = -a;
        int y1 = a;
        int z = 0;
        int u0 = 0;
        float u1 = 1;
        float v0 = 0;
        float v1 = 1;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float) x0, (float) y1, (float) z).texture(u0, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float) x1, (float) y0, (float) z).texture(u1, v0).next();
        bufferBuilder.vertex(matrix, (float) x0, (float) y0, (float) z).texture(u0, v0).next();

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

//        DrawableHelper.drawTexture();
    }

    public static void sendMessage(String data) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(data));
        });
    }

    public static void runMain(Runnable runnable) {
        MinecraftClient.getInstance().execute(runnable);
    }

    public static float getVolume() {
        return MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.RECORDS);
    }

    public static void reload() {
        if (nowPlaying != null) {
            nowPlaying.setReload();
        }
    }

    private static void time1() {
        if (hudUtils.save == null)
            return;
        if (count < hudUtils.save.PicRotateSpeed) {
            count++;
            return;
        }
        count = 0;
        ang++;
        ang = ang % 360;
    }

    private static void onGlobalDisablePressed(MinecraftClient client) {
        if (!config.enabled) {
            config.enabled = true;
            client.player.sendMessage(Text.translatable("nekomusic.enable"), false);
            if (client.getCurrentServerEntry() != null && !config.bannedServers.contains(client.getCurrentServerEntry().address)) {
                sendHello(client);
            }
        } else {
            config.enabled = false;
            stopPlaying();
            currentImg = null;
            client.player.sendMessage(Text.translatable("nekomusic.disable"), false);
            if (client.getCurrentServerEntry() != null) {
                sendBye(client);
            }
        }
        config.save();
    }

    private static void onServerDisablePressed(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null) {
            if (client.player != null)
                client.player.sendMessage(Text.translatable("nekomusic.not_multiplayer"), false);
            return;
        }
        if (config.bannedServers.contains(info.address)) {
            config.bannedServers.remove(info.address);
            if (client.player != null)
                client.player.sendMessage(Text.translatable("nekomusic.server_enable"), false);
            sendHello(client);
        } else {
            config.bannedServers.add(info.address);
            stopPlaying();
            currentImg = null;
            if (client.player != null)
                client.player.sendMessage(Text.translatable("nekomusic.server_disable"), false);
            sendBye(client);
        }
        config.save();
    }

    private static void sendHello(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null) {
            return;
        }
        try {
            PacketSender.c2s().send(Identifier.of(NEKO_ID, "client_hello"), PacketByteBufs.empty());
        } catch (Exception e) {
            client.player.sendMessage(Text.translatable("nekomusic.send_packet_fail"), false);
        }
    }

    private static void sendBye(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null) {
            return;
        }
        try {
            PacketSender.c2s().send(Identifier.of(NEKO_ID, "client_bye"), PacketByteBufs.empty());
        } catch (Exception e) {
            client.player.sendMessage(Text.translatable("nekomusic.send_packet_fail"), false);
        }
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ALLMUSIC_COMPAT_ID, (client, handler, buffer, responseSender) -> {
            PacketProcess(buffer);
        });
        nowPlaying = new APlayer();
        hudUtils = new HudUtils();

        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(NekoMusic_Cli::time1, 0, 1, TimeUnit.MILLISECONDS);

        globalDisableKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nekomusic.disable", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_F7, // The keycode of the key
                "category.nekomusic.general" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (globalDisableKeyBinding.wasPressed()) {
                onGlobalDisablePressed(client);
            }
        });

        serverDisableKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nekomusic.server_disable", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_F8, // The keycode of the key
                "category.nekomusic.general" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (serverDisableKeyBinding.wasPressed()) {
                onServerDisablePressed(client);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo info = client.getCurrentServerEntry();
            if (info == null) {
                return;
            }
            if (config.enabled && !config.bannedServers.contains(info.address)) {
                sendHello(client);
            }
        });

//        S2CPacketReceiver.register(Identifier.of(NEKO_ID, "channel"), new PacketReceiver());
        MetadataPacketReceiver.register();
        ListPacketReceiver.register();

        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((manager, data) -> {
                    config = data;
                    if (!config.enabled) {
                        stopPlaying();
                    } else {
                        hudUtils.setPos(config);
                    }
                    return ActionResult.SUCCESS;
                }
        );
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
