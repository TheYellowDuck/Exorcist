// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package main;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

public class Sound {

    private static final List<Clip> active = new ArrayList<>();

    public static void play(String resourcePath) {
        new Thread(() -> {
            try {
                InputStream is = Sound.class.getResourceAsStream(resourcePath);
                if (is == null) {
                    System.err.println("[Sound] resource not found: " + resourcePath);
                    return;
                }
                AudioInputStream raw = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                AudioFormat base = raw.getFormat();
                // Convert to PCM_SIGNED if needed (e.g. MP3/ALAW/ULAW)
                AudioInputStream ais = raw;
                if (base.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                    AudioFormat pcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        base.getSampleRate(), 16,
                        base.getChannels(), base.getChannels() * 2,
                        base.getSampleRate(), false);
                    ais = AudioSystem.getAudioInputStream(pcm, raw);
                }
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                synchronized (active) { active.add(clip); }
                clip.addLineListener(e -> {
                    if (e.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        synchronized (active) { active.remove(clip); }
                    }
                });
                clip.start();
                System.out.println("[Sound] playing: " + resourcePath);
            } catch (Exception e) {
                System.err.println("[Sound] error playing " + resourcePath + ": " + e);
                e.printStackTrace();
            }
        }).start();
    }
}
