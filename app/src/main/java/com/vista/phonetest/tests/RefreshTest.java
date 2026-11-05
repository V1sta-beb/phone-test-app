package com.vista.phonetest.tests;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.util.ArrayDeque;

/**
 * Measures the refresh rate the system gives this window, and shows a visual
 * comparison: three dots move at the same speed, but one is redrawn every
 * frame, one every 2nd frame and one every 4th. On a real 120 Hz panel the
 * first dot is visibly smoother than the second; if they look the same, the
 * screen is effectively showing 60 Hz.
 */
public class RefreshTest extends BaseTest {
    private boolean running;
    private final ArrayDeque<Long> frames = new ArrayDeque<>();
    private boolean touching;
    private int max, curHz;
    private long frameNo, dropped, lastT;
    private TextView big, mx, drops;
    private Pad pad;
    private Compare cmp;

    public RefreshTest() {
        super("hz", "Частота обновления", "До 120 Гц + визуальная проверка");
    }

    private final Choreographer.FrameCallback cb = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long t) {
            if (!running) return;
            frameNo++;
            frames.addLast(t);
            while (!frames.isEmpty() && t - frames.peekFirst() > 1_000_000_000L) frames.removeFirst();
            if (frames.size() > 2) {
                long span = frames.peekLast() - frames.peekFirst();
                curHz = Math.round((frames.size() - 1) * 1e9f / span);
                big.setText(curHz + " Гц");
                if (touching && span > 800_000_000L && curHz > max) {
                    max = curHz;
                    mx.setText("максимум при касании: " + max + " Гц");
                }
                // A gap longer than 1.5 frame periods means frames were skipped.
                if (lastT != 0 && curHz > 0) {
                    double period = 1e9 / curHz;
                    long gap = t - lastT;
                    if (gap > period * 1.5) dropped += Math.round(gap / period) - 1;
                }
                drops.setText("пропущено кадров: " + dropped);
            }
            lastT = t;
            setNote("максимум " + (max > 0 ? max : curHz) + " Гц, пропущено кадров " + dropped);
            pad.invalidate();
            cmp.tick(t);
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    @Override
    public void build(LinearLayout body) {
        max = 0;
        frameNo = 0;
        dropped = 0;
        lastT = 0;
        frames.clear();
        body.addView(U.howto(a, "Samsung держит 60 Гц, пока экран не трогают, и поднимает до 120 Гц при касании. Водите пальцем по полю 3–4 секунды и смотрите на максимум. Если выше 60 не поднимается: Дисплей → Плавність руху → Адаптивна, энергосбережение выключено."));
        big = U.big(a, "—");
        mx = U.text(a, "максимум при касании: —", 15, U.MUTED, false);
        mx.setGravity(android.view.Gravity.CENTER);
        drops = U.text(a, "пропущено кадров: 0", 13, U.MUTED, false);
        drops.setGravity(android.view.Gravity.CENTER);
        body.addView(big);
        body.addView(mx);
        body.addView(drops);
        pad = new Pad(a);
        body.addView(pad, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 220)));

        body.addView(U.text(a, "Визуальная проверка", 17, U.FG, true));
        body.addView(U.howto(a, "Три шарика едут с одинаковой скоростью, но перерисовываются с разной частотой. Держите палец на экране где угодно (чтобы Samsung не снизил частоту) и смотрите на шарики.\n\nНа настоящих 120 Гц верхний шарик заметно плавнее и чётче среднего, а нижний явно дёргается. Если верхний и средний выглядят одинаково — экран фактически показывает 60 Гц."));
        cmp = new Compare(a);
        body.addView(cmp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 240)));

        running = true;
        Choreographer.getInstance().postFrameCallback(cb);
    }

    @Override
    public void stop() {
        running = false;
    }

    private class Pad extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float x = -1, y = -1, ang;

        Pad(Context c) {
            super(c);
            setBackground(U.box(c, U.CARD, U.LINE, 10));
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int act = e.getActionMasked();
            touching = act != MotionEvent.ACTION_UP && act != MotionEvent.ACTION_CANCEL;
            x = e.getX();
            y = e.getY();
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            ang += 0.06f;
            p.setColor(0xFF3A4456);
            c.drawCircle(getWidth() / 2f + (float) Math.cos(ang) * getWidth() * .35f,
                    getHeight() / 2f + (float) Math.sin(ang) * getHeight() * .35f, U.dp(getContext(), 10), p);
            if (touching) {
                p.setColor(U.ACCENT);
                c.drawCircle(x, y, U.dp(getContext(), 28), p);
            } else {
                p.setColor(U.MUTED);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(U.dp(getContext(), 15));
                c.drawText("водите пальцем здесь", getWidth() / 2f, getHeight() / 2f, p);
            }
        }
    }

    /** Three dots at the same speed, updated every 1st, 2nd and 4th frame. */
    private class Compare extends View {
        final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG), lane = new Paint(), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int[] every = {1, 2, 4};
        final float[] shownT = new float[3];
        long t0;
        boolean holding;

        Compare(Context c) {
            super(c);
            setBackground(U.box(c, U.CARD, U.LINE, 10));
            dot.setColor(U.FG);
            lane.setColor(0xFF283142);
            lane.setStrokeWidth(U.dp(c, 2));
            txt.setColor(U.MUTED);
            txt.setTextSize(U.dp(c, 13));
        }

        void tick(long t) {
            if (t0 == 0) t0 = t;
            float sec = (t - t0) / 1e9f;
            for (int i = 0; i < 3; i++) if (frameNo % every[i] == 0) shownT[i] = sec;
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int act = e.getActionMasked();
            holding = act != MotionEvent.ACTION_UP && act != MotionEvent.ACTION_CANCEL;
            touching = holding;
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float pad = U.dp(getContext(), 22);
            float r = U.dp(getContext(), 11);
            float track = w - pad * 2;
            // One full sweep across the lane per 1.2 s: fast enough to show judder.
            float speed = track / 1.2f;
            int hz = curHz > 0 ? curHz : 60;
            String[] labels = {"каждый кадр · " + hz + " Гц", "через кадр · " + (hz / 2) + " Гц", "через 3 кадра · " + (hz / 4) + " Гц"};
            for (int i = 0; i < 3; i++) {
                float y = h * (i + 1) / 4f;
                c.drawLine(pad, y, w - pad, y, lane);
                float d = (shownT[i] * speed) % (track * 2);
                float x = d <= track ? pad + d : pad + track * 2 - d;
                c.drawCircle(x, y, r, dot);
                c.drawText(labels[i], pad, y - r - U.dp(getContext(), 6), txt);
            }
        }
    }
}
