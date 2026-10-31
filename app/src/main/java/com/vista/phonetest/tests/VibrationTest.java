package com.vista.phonetest.tests;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.LinearLayout;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class VibrationTest extends BaseTest {
    private Vibrator vib;

    public VibrationTest() {
        super("vib", "Вибрация", "Короткая, длинная, серия");
    }

    @Override
    public void build(LinearLayout body) {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = a.getSystemService(VibratorManager.class);
            vib = vm.getDefaultVibrator();
        } else {
            vib = a.getSystemService(Vibrator.class);
        }
        if (vib == null || !vib.hasVibrator()) {
            body.addView(U.note(a, "Система сообщает, что вибромотора нет."));
            setNote("вибромотор не найден");
            return;
        }
        body.addView(U.howto(a, "Вибрация должна быть чёткой и сильной, без дребезга и посторонних звуков. Если ничего не происходит — проверьте, что в настройках звука вибрация не отключена."));
        body.addView(U.row(a,
                U.button(a, "Короткая", v -> vib.vibrate(VibrationEffect.createOneShot(80, 255))),
                U.button(a, "Длинная", v -> vib.vibrate(VibrationEffect.createOneShot(1200, 255))),
                U.button(a, "Серия", v -> vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 150, 100, 150, 100, 400}, -1)))));
        setNote("проверено на ощупь");
    }

    @Override
    public void stop() {
        if (vib != null) vib.cancel();
    }
}
