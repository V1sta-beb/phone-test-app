package com.vista.phonetest.tests;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class ProximityTest extends BaseTest implements SensorEventListener {
    private SensorManager sm;
    private Sensor prox, light;
    private boolean sawNear, sawFar;
    private float luxMin = Float.MAX_VALUE, luxMax = 0;
    private TextView[] v;
    private TextView big;

    public ProximityTest() {
        super("prox", "Датчики приближения и света", "Гасит экран у уха, автояркость");
    }

    @Override
    public void build(LinearLayout body) {
        sawNear = sawFar = false;
        luxMin = Float.MAX_VALUE;
        luxMax = 0;
        sm = a.getSystemService(SensorManager.class);
        prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        body.addView(U.howto(a, "Закройте ладонью верхнюю часть экрана у фронтальной камеры на 1–2 секунды и уберите. Должно появиться «Близко», затем «Далеко». Освещённость должна заметно падать, когда закрываете, и расти на свету."));
        big = U.big(a, "—");
        body.addView(big);
        v = U.kv(a, body, "Приближение", "Близко было", "Далеко было", "Освещённость", "Диапазон света");
        if (prox == null) v[0].setText("датчик не найден");
        else sm.registerListener(this, prox, SensorManager.SENSOR_DELAY_UI);
        if (light == null) v[3].setText("датчик не найден");
        else sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float d = e.values[0];
            boolean near = d < e.sensor.getMaximumRange() && d < 5f;
            if (near) sawNear = true;
            else sawFar = true;
            big.setText(near ? "Близко" : "Далеко");
            big.setTextColor(near ? U.ACCENT : U.FG);
            v[0].setText(U.fmt(d, 1) + " (макс. " + U.fmt(e.sensor.getMaximumRange(), 1) + ")");
            v[1].setText(sawNear ? "✓" : "—");
            v[2].setText(sawFar ? "✓" : "—");
        } else {
            float lux = e.values[0];
            luxMin = Math.min(luxMin, lux);
            luxMax = Math.max(luxMax, lux);
            v[3].setText(U.fmt(lux, 0) + " лк");
            v[4].setText(U.fmt(luxMin, 0) + " … " + U.fmt(luxMax, 0) + " лк");
        }
        setNote("приближение " + (sawNear && sawFar ? "✓" : "✕") + ", свет " + (luxMax > luxMin * 3 + 5 ? "меняется" : "не проверен"));
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc) {}

    @Override
    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }
}
