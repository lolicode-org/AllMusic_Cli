package coloryr.allmusic_client.config;

import coloryr.allmusic_client.AllMusic;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Config(name = "allmusic")
public class ModConfig implements ConfigData {
    public boolean enabled = true;
    public boolean enableHud = true;
    public boolean enableHudInfo = true;
    public boolean enableHudList = true;
    public boolean enableHudLyric = true;
    public boolean enableHudImg = true;
    public boolean enableHudImgRotate = true;

    public int infoX = 74;
    public int infoY = 2;
    public int listX = 2;
    public int listY = 74;
    public int lyricX = 74;
    public int lyricY = 53;
    public int imgX = 2;
    public int imgY = 2;
    public int imgSize = 70;
    public int imgRotateSpeed = 50;

    public List<String> bannedServers = new ArrayList<>();

    public void validatePostLoad() {
        if (infoX < 0)
            infoX = 0;
        if (infoY < 0)
            infoY = 0;
        if (listX < 0)
            listX = 0;
        if (listY < 0)
            listY = 0;
        if (lyricX < 0)
            lyricX = 0;
        if (lyricY < 0)
            lyricY = 0;
        if (imgX < 0)
            imgX = 0;
        if (imgY < 0)
            imgY = 0;
        if (imgSize < 0)
            imgSize = 10;

        AllMusic.hudUtils.setPos(this);
    }

    public void save() {
        AutoConfig.getConfigHolder(this.getClass()).save();
    }
}
