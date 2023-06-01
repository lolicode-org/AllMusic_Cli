package org.lolicode.nekomusic_cli.packet;

import org.lolicode.nekomusic_cli.NekoMusic_Cli;
import org.lolicode.nekomusic_cli.music.MusicList;
import com.google.gson.Gson;
import lol.bai.badpackets.api.PacketSender;
import lol.bai.badpackets.api.S2CPacketReceiver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public class ListPacketReceiver implements S2CPacketReceiver {
    private static final Identifier LIST = new Identifier(NekoMusic_Cli.NEKO_ID, "list");
    private static final Gson gson = new Gson();

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        PacketProcess(buf);
    }

    static void PacketProcess(PacketByteBuf buf) {
        if (buf == null || !NekoMusic_Cli.config.enableHudList || !NekoMusic_Cli.config.enableHud) return;
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

        MusicList musicList = gson.fromJson(data, MusicList.class);
        if (musicList == null || musicList.isEmpty()) return;
        NekoMusic_Cli.hudUtils.List = musicList.toString();
    }

    public static void register() {
        S2CPacketReceiver.register(LIST, new ListPacketReceiver());
    }
}
