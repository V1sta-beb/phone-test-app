package com.vista.phonetest.tests;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class MicTest extends BaseTest {
    private static final int RATE = 44100;
    private volatile boolean busy, stop;
    private ProgressBar meter;
    private TextView st;
    private final String[] results = new String[3];

    public MicTest() {
        super("mic", "Микрофоны", "Нижний и верхний, запись и прослушивание");
    }

    @Override
    public String[] permissions() {
        return new String[]{Manifest.permission.RECORD_AUDIO};
    }

    @Override
    public void build(LinearLayout body) {
        stop = false;
        if (a.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            body.addView(U.note(a, "Нет разрешения на микрофон. Закройте тест и откройте снова, разрешите доступ."));
            return;
        }
        body.addView(U.howto(a, "Каждая кнопка пишет 4 секунды с одного микрофона и сразу воспроизводит запись. Говорите обычным голосом на расстоянии вытянутой руки. У Samsung «Основной» — это обычно нижний микрофон, «Видео» — верхний, «Голос» — с шумоподавлением. Голос должен быть чистым, без шипения и обрывов."));
        meter = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        meter.setMax(100);
        body.addView(meter);
        st = U.text(a, "", 15, U.MUTED, false);
        body.addView(st);
        body.addView(U.button(a, "Основной (нижний)", v -> record(MediaRecorder.AudioSource.MIC, 0, "основной")));
        body.addView(U.button(a, "Видео (верхний)", v -> record(MediaRecorder.AudioSource.CAMCORDER, 1, "видео")));
        body.addView(U.button(a, "Голос (с шумоподавлением)", v -> record(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 2, "голос")));
    }

    private void record(int source, int idx, String name) {
        if (busy) return;
        busy = true;
        st.setText("Запись: " + name + "…");
        new Thread(() -> {
            AudioRecord rec = null;
            AudioTrack tr = null;
            try {
                int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                rec = new AudioRecord(source, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 8192));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("микрофон не инициализировался");
                short[] all = new short[RATE * 4];
                short[] buf = new short[1024];
                int pos = 0;
                double peak = 0;
                rec.startRecording();
                while (pos < all.length && !stop) {
                    int n = rec.read(buf, 0, Math.min(buf.length, all.length - pos));
                    if (n <= 0) break;
                    double s = 0;
                    for (int i = 0; i < n; i++) {
                        all[pos + i] = buf[i];
                        s += buf[i] * (double) buf[i];
                    }
                    pos += n;
                    double rms = Math.sqrt(s / n) / 32768.0;
                    peak = Math.max(peak, rms);
                    final int lvl = (int) Math.min(100, rms * 400);
                    ui(() -> meter.setProgress(lvl));
                }
                rec.stop();
                final double pk = peak;
                final String level = pk > 0.05 ? "хороший" : pk > 0.01 ? "слабый" : "почти нет сигнала";
                results[idx] = name + ": " + level;
                ui(() -> {
                    meter.setProgress(0);
                    st.setText("Уровень (" + name + "): " + level + ". Воспроизвожу…");
                    st.setTextColor(pk > 0.01 ? U.FG : U.BAD);
                    updateNote();
                });
                if (stop) return;
                tr = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        .setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(pos * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                tr.write(all, 0, pos);
                tr.play();
                Thread.sleep(pos * 1000L / RATE + 200);
                ui(() -> st.setText(st.getText().toString().replace(" Воспроизвожу…", " Готово.")));
            } catch (Exception e) {
                final String m = e.getMessage();
                results[idx] = name + ": ошибка";
                ui(() -> {
                    st.setText("Ошибка (" + name + "): " + m);
                    st.setTextColor(U.BAD);
                    updateNote();
                });
            } finally {
                if (rec != null) rec.release();
                if (tr != null) {
                    try {
                        tr.stop();
                    } catch (Exception ignored) {
                    }
                    tr.release();
                }
                busy = false;
            }
        }).start();
    }

    private void updateNote() {
        StringBuilder sb = new StringBuilder();
        for (String r : results) if (r != null) sb.append(sb.length() > 0 ? ", " : "").append(r);
        setNote(sb.toString());
    }

    @Override
    public void stop() {
        stop = true;
    }
}
