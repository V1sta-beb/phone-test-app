package com.vista.phonetest.tests;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Samsung phones often report proximity only through the wake-up variant of the
 * sensor, or only while something holds the proximity wake lock (as the phone
 * app does during a call). So this test listens to every proximity sensor the
 * system lists and can also enable "call mode", which turns the screen off when
 * the sensor is covered — exactly what happens at the ear.
 */
public class ProximityTest extends BaseTest implements SensorEventListener {
    private SensorManager sm;
    private Sensor light;
    private boolean sawNear, sawFar;
    private float luxMin = Float.MAX_VALUE, luxMax = 0;
    private int events;
    private TextView[] v;
    private TextView big;
    private PowerManager.WakeLock callLock;
    private Button callBtn;

    public ProximityTest() {
        super("prox", "Датчики приближения и света", "Гасит экран у уха, автояркость");
    }

    @Override
    public void build(LinearLayout body) {
        sawNear = sawFar = false;
        events = 0;
        luxMin = Float.MAX_VALUE;
        luxMax = 0;
        sm = a.getSystemService(SensorManager.class);
        body.addView(U.howto(a, "Закройте ладонью верхнюю часть экрана у фронтальной камеры на 1–2 секунды и уберите. Должно появиться «Близко», затем «Далеко».\n\nЕсли значения не меняются, включите «Режим звонка» и закройте датчик: экран должен погаснуть и включиться, когда уберёте руку. Так работает телефон у уха."));
        big = U.big(a, "—");
        body.addView(big);
        v = U.kv(a, body, "Приближение", "Событий от датчика", "Близко было", "Далеко было", "Освещённость", "Диапазон света", "Датчики");

        Set<Sensor> prox = new LinkedHashSet<>();
        Sensor d1 = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        Sensor d2 = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY, true);
        if (d1 != null) prox.add(d1);
        if (d2 != null) prox.add(d2);
        List<Sensor> all = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : all) {
            if (s.getType() == Sensor.TYPE_PROXIMITY || s.getName().toLowerCase().contains("proximity")) prox.add(s);
        }
        StringBuilder names = new StringBuilder();
        for (Sensor s : prox) {
            sm.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);
            names.append(names.length() > 0 ? "\n" : "").append(s.getName()).append(s.isWakeUpSensor() ? " (wake)" : "");
        }
        v[6].setText(prox.isEmpty() ? "приближение не найдено" : names.toString());
        if (prox.isEmpty()) v[0].setText("датчик не найден");

        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (light == null) v[4].setText("датчик не найден");
        else sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);

        callBtn = U.button(a, "Режим звонка: выкл", x -> toggleCallMode());
        body.addView(callBtn);
    }

    private void toggleCallMode() {
        PowerManager pm = a.getSystemService(PowerManager.class);
        if (callLock != null && callLock.isHeld()) {
            callLock.release();
            callBtn.setText("Режим звонка: выкл");
            return;
        }
        if (pm == null || !pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            callBtn.setText("Режим звонка не поддерживается");
            return;
        }
        callLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "phonetest:proximity");
        callLock.acquire(5 * 60 * 1000L);
        callBtn.setText("Режим звонка: вкл — закройте датчик");
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = e.values[0];
            luxMin = Math.min(luxMin, lux);
            luxMax = Math.max(luxMax, lux);
            v[4].setText(U.fmt(lux, 0) + " лк");
            v[5].setText(U.fmt(luxMin, 0) + " … " + U.fmt(luxMax, 0) + " лк");
        } else {
            events++;
            float d = e.values[0];
            float max = e.sensor.getMaximumRange();
            boolean near = d < max && d < 5f;
            if (near) sawNear = true;
            else sawFar = true;
            big.setText(near ? "Близко" : "Далеко");
            big.setTextColor(near ? U.ACCENT : U.FG);
            v[0].setText(U.fmt(d, 1) + " (макс. " + U.fmt(max, 1) + ")");
            v[1].setText(String.valueOf(events));
            v[2].setText(sawNear ? "✓" : "—");
            v[3].setText(sawFar ? "✓" : "—");
        }
        setNote("приближение " + (sawNear && sawFar ? "✓" : "по датчику не видно") + ", свет " + (luxMax > luxMin * 3 + 5 ? "меняется" : "не проверен"));
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc) {}

    @Override
    public void stop() {
        if (sm != null) sm.unregisterListener(this);
        if (callLock != null && callLock.isHeld()) callLock.release();
        callLock = null;
    }
}
