package com.vista.phonetest.tests;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class EarpieceTest extends BaseTest {
    private final Tone tone = new Tone();
    private AudioManager am;
    private int prevMode;
    private TextView st;

    public EarpieceTest() {
        super("ear", "Разговорный динамик", "Звук как при звонке, без SIM");
    }

    @Override
    public void build(LinearLayout body) {
        am = a.getSystemService(AudioManager.class);
        prevMode = am.getMode();
        body.addView(U.howto(a, "Нажмите «Играть» и поднесите телефон к уху. Писк должен быть слышен только из узкой щели над экраном, чисто, без хрипа. Из нижнего динамика звука быть не должно. Громкость регулируется кнопками во время писка."));
        st = U.text(a, "", 14, U.MUTED, false);
        body.addView(st);
        body.addView(U.row(a,
                U.primary(a, "Играть", v -> play()),
                U.button(a, "Стоп", v -> tone.stopNow())));
    }

    private void play() {
        try {
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            boolean routed = false;
            if (Build.VERSION.SDK_INT >= 31) {
                for (AudioDeviceInfo d : am.getAvailableCommunicationDevices()) {
                    if (d.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                        routed = am.setCommunicationDevice(d);
                        break;
                    }
                }
            }
            if (!routed) am.setSpeakerphoneOn(false);
            a.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            st.setText(routed ? "Звук направлен в разговорный динамик." : "Звук направлен в динамик звонка (старый способ).");
            tone.play(AudioAttributes.USAGE_VOICE_COMMUNICATION, 0, 1000, 6, false);
            setNote("проверено на слух");
        } catch (Exception e) {
            st.setText("Не удалось включить режим звонка: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        tone.stopNow();
        try {
            if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice();
            am.setMode(prevMode);
        } catch (Exception ignored) {
        }
        a.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
    }
}
