package com.vista.phonetest.tests;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

/**
 * Seal test: pressing the screen of a sealed phone compresses the air inside,
 * so the internal barometer sees a short pressure rise. A leaky (opened,
 * badly re-glued) case lets the air escape and the rise is tiny or absent.
 */
public class BarometerTest extends BaseTest implements SensorEventListener {
    private static final int N = 300;
    private SensorManager sm;
    private TextView big, verdict, info;
    private Graph graph;
    private final float[] hist = new float[N];
    private int count;
    private double calSum;
    private int calN;
    private long calUntil;
    private float baseline = Float.NaN, maxDelta, cur;

    public BarometerTest() {
        super("seal", "Герметичность (барометр)", "Скачок давления при нажатии на экран");
    }

    @Override
    public void build(LinearLayout body) {
        sm = a.getSystemService(SensorManager.class);
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (s == null) {
            body.addView(U.note(a, "Барометр не найден. У S23 Ultra он есть, его отсутствие — плохой знак."));
            setNote("барометр не найден");
            return;
        }
        body.addView(U.howto(a, "1. Положите телефон экраном вверх на стол и не трогайте — 3 секунды калибровки.\n2. Надавите большим пальцем на центр экрана с умеренной силой, как на кнопку, держите 2–3 секунды и отпустите. Не давите на края и камеры.\n3. Повторите 2–3 раза.\n\nГерметичный корпус даёт скачок от 0,5 гПа. Меньше 0,2 гПа — воздух выходит, корпус вскрывали или он повреждён."));
        big = U.big(a, "калибровка…");
        big.setTextSize(28);
        body.addView(big);
        verdict = U.text(a, "", 17, U.FG, true);
        verdict.setGravity(android.view.Gravity.CENTER);
        body.addView(verdict);
        info = U.text(a, "", 13, U.MUTED, false);
        body.addView(info);
        graph = new Graph(a);
        body.addView(graph, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 200)));
        body.addView(U.button(a, "Начать заново", v -> calibrate()));
        sm.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);
        calibrate();
    }

    private void calibrate() {
        baseline = Float.NaN;
        maxDelta = 0;
        calSum = 0;
        calN = 0;
        count = 0;
        calUntil = SystemClock.elapsedRealtime() + 3000;
        if (big != null) {
            big.setText("калибровка…");
            verdict.setText("Не трогайте телефон");
            verdict.setTextColor(U.MUTED);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        cur = e.values[0];
        long now = SystemClock.elapsedRealtime();
        if (Float.isNaN(baseline)) {
            calSum += cur;
            calN++;
            if (now >= calUntil && calN > 3) {
                baseline = (float) (calSum / calN);
                verdict.setText("Надавите на центр экрана");
                verdict.setTextColor(U.FG);
            }
            info.setText("давление " + U.fmt(cur, 2) + " гПа");
            return;
        }
        float d = cur - baseline;
        hist[count % N] = d;
        count++;
        if (d > maxDelta) maxDelta = d;
        big.setText((d >= 0 ? "+" : "") + U.fmt(d, 2) + " гПа");
        info.setText("давление " + U.fmt(cur, 2) + " гПа, база " + U.fmt(baseline, 2) + ", максимальный скачок +" + U.fmt(maxDelta, 2) + " гПа");
        if (maxDelta >= 0.5f) {
            verdict.setText("Корпус герметичен");
            verdict.setTextColor(U.OK);
            setNote("скачок +" + U.fmt(maxDelta, 2) + " гПа, герметичен");
        } else if (maxDelta >= 0.2f) {
            verdict.setText("Слабый скачок — надавите сильнее или корпус под вопросом");
            verdict.setTextColor(U.WARN);
            setNote("слабый скачок +" + U.fmt(maxDelta, 2) + " гПа");
        } else {
            setNote("скачок +" + U.fmt(maxDelta, 2) + " гПа, реакции нет");
        }
        graph.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc) {}

    @Override
    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }

    private class Graph extends View {
        final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), grid = new Paint(), txt = new Paint(Paint.ANTI_ALIAS_FLAG);

        Graph(Context c) {
            super(c);
            setBackground(U.box(c, U.CARD, U.LINE, 10));
            line.setColor(U.ACCENT);
            line.setStrokeWidth(U.dp(c, 2));
            grid.setColor(0xFF3A4456);
            grid.setStrokeWidth(1);
            txt.setColor(U.MUTED);
            txt.setTextSize(U.dp(c, 11));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float range = Math.max(1.0f, maxDelta * 1.2f);
            float zeroY = h * 0.8f;
            float k = (h * 0.7f) / range;
            c.drawLine(0, zeroY, w, zeroY, grid);
            float y05 = zeroY - 0.5f * k;
            c.drawLine(0, y05, w, y05, grid);
            c.drawText("+0,5 гПа", U.dp(getContext(), 6), y05 - U.dp(getContext(), 3), txt);
            int n = Math.min(count, N);
            if (n < 2) return;
            float dx = w / (N - 1);
            int start = count - n;
            float px = 0, py = 0;
            for (int i = 0; i < n; i++) {
                float val = hist[(start + i) % N];
                float x = (N - n + i) * dx;
                float y = zeroY - val * k;
                if (i > 0) c.drawLine(px, py, x, y, line);
                px = x;
                py = y;
            }
        }
    }
}
