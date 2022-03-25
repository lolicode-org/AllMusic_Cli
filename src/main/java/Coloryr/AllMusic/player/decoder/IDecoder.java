package Coloryr.AllMusic.player.decoder;

import Coloryr.AllMusic.player.decoder.mp3.Header;
import org.apache.http.client.HttpClient;

import java.net.URL;

public interface IDecoder {
    BuffPack decodeFrame() throws Exception;

    void close() throws Exception;

    void set(HttpClient client, URL url) throws Exception;

    int getOutputFrequency();

    int getOutputChannels();

    void set(int time);
}