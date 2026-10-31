package com.vista.phonetest.tests;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.widget.LinearLayout;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class SpeakerTest extends BaseTest {
    private final Tone tone = new Tone();

    public SpeakerTest() {
        super("spk", "Динамики", "Верхний, нижний, свип на дребезг");
    }

    @Override
    public void build(LinearLayout body) {
        body.addView(U.howto(a, "Снимите чехол, громкость на максимум (кнопкой «громче»). «Левый» и «Правый» должны звучать из разных динамиков: в вертикальном положении это верх и низ. Свип плавно идёт от низких к высоким: хрип, треск или дребезг на какой-то частоте — повреждённый динамик или сетка."));
        AudioManager am = a.getSystemService(AudioManager.class);
        a.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        body.addView(U.text(a, "Громкость медиа: " + am.getStreamVolume(AudioManager.STREAM_MUSIC) + " из " + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 14, U.MUTED, false));
        int u = AudioAttributes.USAGE_MEDIA;
        body.addView(U.row(a,
                U.button(a, "◀ Левый", v -> tone.play(u, -1, 440, 3, false)),
                U.button(a, "Оба", v -> tone.play(u, 0, 440, 3, false)),
                U.button(a, "Правый ▶", v -> tone.play(u, 1, 440, 3, false))));
        body.addView(U.row(a,
                U.primary(a, "Свип 80 Гц → 12 кГц", v -> tone.play(u, 0, 0, 8, true)),
                U.button(a, "Стоп", v -> tone.stopNow())));
        setNote("проверено на слух");
    }

    @Override
    public void stop() {
        tone.stopNow();
    }
}
