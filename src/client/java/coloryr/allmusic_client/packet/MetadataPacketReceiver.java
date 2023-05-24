package coloryr.allmusic_client.packet;

import coloryr.allmusic_client.AllMusic;
import coloryr.allmusic_client.music.MusicList;
import coloryr.allmusic_client.music.MusicObj;
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
    private static final Identifier METADATA = new Identifier(AllMusic.NEKO_ID, "metadata");

    @Override
    public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        PacketProcess(buf);
    }

    // Currently, the server will send music url with fabric's networking api, and the metadata with badpackets api, so we'll only receive metadata here.
    // If this changes in the future, this method should also be changed accordingly.
    static void PacketProcess(PacketByteBuf buf) {
        if (buf == null) return;
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
        if (music.album != null && music.album.picUrl != null && !music.album.picUrl.isEmpty()) {
            AllMusic.hudUtils.setImg(music.album.picUrl);
        }
        if (music.name != null && !music.name.isBlank()) {
            StringBuilder list = new StringBuilder();
            list.append(music.name).append("\n");
            if (music.ar != null && !music.ar.isEmpty())
                list.append(music.ar.stream().map(artistObj -> artistObj.name).reduce((a, b) -> a + " & " + b).orElse("")).append("\n");
            if (music.album != null && music.album.name != null && !music.album.name.isBlank())
                list.append(music.album.name).append("\n");
            if (music.player != null && !music.player.isBlank())
                list.append("by: ").append(music.player);
            AllMusic.hudUtils.Info = list.toString();
        }
        AllMusic.hudUtils.setLyric(music);
    }

    public static void register() {
        S2CPacketReceiver.register(METADATA, new MetadataPacketReceiver());
    }
}
