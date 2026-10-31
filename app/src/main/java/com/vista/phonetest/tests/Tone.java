package com.vista.phonetest.tests;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Generates test tones on a background thread. pan: -1 left, 0 both, 1 right. */
class Tone {
    static final int RATE = 48000;
    private volatile boolean stop;
    private Thread th;

    void stopNow() {
        stop = true;
        if (th != null) {
            try {
                th.join(500);
            } catch (InterruptedException ignored) {
            }
        }
        th = null;
    }

    /** Beeps: 0.3 s on / 0.2 s off at freq, for seconds. If sweep, glides 80 Hz → 12 kHz instead. */
    void play(int usage, float pan, float freq, float seconds, boolean sweep) {
        stopNow();
        stop = false;
        th = new Thread(() -> {
            AudioTrack t = null;
            try {
                int min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                t = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(usage)
                                .setContentType(usage == AudioAttributes.USAGE_VOICE_COMMUNICATION
                                        ? AudioAttributes.CONTENT_TYPE_SPEECH : AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setSampleRate(RATE)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build())
                        .setBufferSizeInBytes(Math.max(min, 8192))
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();
                t.play();
                float lg = pan > 0 ? 1 - pan : 1, rg = pan < 0 ? 1 + pan : 1;
                int total = (int) (seconds * RATE);
                short[] buf = new short[1024 * 2];
                double phase = 0;
                int n = 0;
                while (n < total && !stop) {
                    int frames = Math.min(1024, total - n);
                    for (int i = 0; i < frames; i++, n++) {
                        double tsec = n / (double) RATE;
                        double f;
                        double env;
                        if (sweep) {
                            f = 80 * Math.pow(12000 / 80.0, tsec / seconds);
                            env = Math.min(1, Math.min(tsec / 0.05, (seconds - tsec) / 0.05));
                        } else {
                            f = freq;
                            double m = tsec % 0.5;
                            env = m < 0.3 ? Math.min(1, Math.min(m / 0.01, (0.3 - m) / 0.01)) : 0;
                        }
                        phase += 2 * Math.PI * f / RATE;
                        if (phase > 2 * Math.PI) phase -= 2 * Math.PI;
                        double s = Math.sin(phase) * env * 0.6 * 32767;
                        buf[i * 2] = (short) (s * lg);
                        buf[i * 2 + 1] = (short) (s * rg);
                    }
                    t.write(buf, 0, frames * 2);
                }
            } catch (Exception ignored) {
            } finally {
                if (t != null) {
                    try {
                        t.stop();
                    } catch (Exception ignored) {
                    }
                    t.release();
                }
            }
        });
        th.start();
    }
}
