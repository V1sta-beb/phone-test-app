package com.vista.phonetest.tests;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class MotionTest extends BaseTest implements SensorEventListener {
    private SensorManager sm;
    private TextView[] v;
    private Level level;
    private boolean acc, gyr, mag;
    private float ax, ay;

    public MotionTest() {
        super("motion", "Датчики движения", "Акселерометр, гироскоп, компас");
    }

    @Override
    public void build(LinearLayout body) {
        acc = gyr = mag = false;
        sm = a.getSystemService(SensorManager.class);
        body.addView(U.howto(a, "Покрутите телефон в руках: все значения должны плавно меняться. Положите на стол — шарик встанет в центр, ускорение по Z около 9,8. Компас: при повороте телефона значения магнитного поля меняются."));
        v = U.kv(a, body, "Ускорение X/Y/Z", "Гироскоп X/Y/Z", "Магнитное поле", "Состояние");
        level = new Level(a);
        body.addView(level, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 240)));
        reg(Sensor.TYPE_ACCELEROMETER, 0);
        reg(Sensor.TYPE_GYROSCOPE, 1);
        reg(Sensor.TYPE_MAGNETIC_FIELD, 2);
    }

    private void reg(int type, int row) {
        Sensor s = sm.getDefaultSensor(type);
        if (s == null) v[row].setText("датчик не найден");
        else sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float[] x = e.values;
        String s = U.fmt(x[0], 2) + " / " + U.fmt(x[1], 2) + " / " + U.fmt(x[2], 2);
        switch (e.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                v[0].setText(s);
                acc = true;
                ax = x[0];
                ay = x[1];
                level.invalidate();
                break;
            case Sensor.TYPE_GYROSCOPE:
                v[1].setText(s);
                if (Math.abs(x[0]) + Math.abs(x[1]) + Math.abs(x[2]) > 0.05f) gyr = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                float m = (float) Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
                v[2].setText(U.fmt(m, 0) + " мкТл");
                mag = true;
                break;
            default:
                break;
        }
        v[3].setText((acc ? "акс ✓ " : "акс … ") + (gyr ? "гиро ✓ " : "гиро … ") + (mag ? "магн ✓" : "магн …"));
        setNote("акселерометр " + (acc ? "✓" : "✕") + ", гироскоп " + (gyr ? "✓" : "✕") + ", компас " + (mag ? "✓" : "✕"));
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc) {}

    @Override
    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }

    private class Level extends View {
        final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG), ball = new Paint(Paint.ANTI_ALIAS_FLAG);

        Level(Context c) {
            super(c);
            setBackground(U.box(c, U.CARD, U.LINE, 10));
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(U.dp(c, 2));
            ring.setColor(0xFF3A4456);
            ball.setColor(U.OK);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) * 0.42f;
            c.drawCircle(w / 2, h / 2, r, ring);
            c.drawCircle(w / 2, h / 2, U.dp(getContext(), 16), ring);
            float k = r / 9.8f;
            c.drawCircle(w / 2 - ax * k, h / 2 + ay * k, U.dp(getContext(), 13), ball);
        }
    }
}
