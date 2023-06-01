package org.lolicode.nekomusic_cli.packet;

import org.lolicode.nekomusic_cli.NekoMusic_Cli;
import org.lolicode.nekomusic_cli.music.MusicObj;
import com.google.gson.Gson;
import lol.bai.badpackets.api.PacketSender;
import lol.bai.badpackets.api.S2CPacketReceiver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public class MetadataPacketReceiver implements S2CPacketReceiver {
    private static final Gson gson = new Gson();
    private static final Identifier METADATA = new Identifier(NekoMusic_Cli.NEKO_ID, "metadata");

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        PacketProcess(buf);
    }

    static void PacketProcess(PacketByteBuf buf) {
        if (buf == null || !NekoMusic_Cli.config.enableHud) return;
        String data;
        try {
            byte[] buff = new byte[buf.readableBytes()];
            buf.readBytes(buff);
            buff[0] = 0;
            data = new String(buff, StandardCharsets.UTF_8).substring(1);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (data.isEmpty()) return;
        MusicObj music = gson.fromJson(data, MusicObj.class);
        if (music == null) return;
//        NekoMusic_Cli.hudUtils.close();  // emmmm，不close反而可能正常显示封面，咱也看不懂为啥
        if (music.album != null && music.album.picUrl != null && !music.album.picUrl.isEmpty() && NekoMusic_Cli.config.enableHudImg) {
            NekoMusic_Cli.hudUtils.setImg(music.album.picUrl);
        }
        if (music.name != null && !music.name.isBlank() && NekoMusic_Cli.config.enableHudInfo) {
            StringBuilder list = new StringBuilder();
            list.append(music.name).append("\n");
            if (music.ar != null && !music.ar.isEmpty())
                list.append(music.ar.stream().map(artistObj -> artistObj.name).reduce((a, b) -> a + " & " + b).orElse("")).append("\n");
            if (music.album != null && music.album.name != null && !music.album.name.isBlank())
                list.append(music.album.name).append("\n");
            if (music.player != null && !music.player.isBlank())
                list.append("by: ").append(music.player);
            NekoMusic_Cli.hudUtils.Info = list.toString();
        }
        if (NekoMusic_Cli.config.enableHudLyric)
            NekoMusic_Cli.hudUtils.setLyric(music);
    }

    public static void register() {
        S2CPacketReceiver.register(METADATA, new MetadataPacketReceiver());
    }
}
