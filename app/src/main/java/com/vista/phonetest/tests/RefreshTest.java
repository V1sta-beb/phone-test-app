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

public class RefreshTest extends BaseTest {
    private boolean running;
    private final ArrayDeque<Long> frames = new ArrayDeque<>();
    private boolean touching;
    private int max;
    private TextView big, mx;
    private Pad pad;

    public RefreshTest() {
        super("hz", "Частота обновления", "Должно быть до 120 Гц");
    }

    private final Choreographer.FrameCallback cb = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long t) {
            if (!running) return;
            frames.addLast(t);
            while (!frames.isEmpty() && t - frames.peekFirst() > 1_000_000_000L) frames.removeFirst();
            if (frames.size() > 2) {
                long span = frames.peekLast() - frames.peekFirst();
                int hz = Math.round((frames.size() - 1) * 1e9f / span);
                big.setText(hz + " Гц");
                if (touching && span > 800_000_000L && hz > max) {
                    max = hz;
                    mx.setText("максимум при касании: " + max + " Гц");
                    setNote("максимум " + max + " Гц");
                }
            }
            pad.invalidate();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    @Override
    public void build(LinearLayout body) {
        max = 0;
        body.addView(U.howto(a, "Samsung держит 60 Гц, пока экран не трогают, и поднимает до 120 Гц при касании. Водите пальцем по полю 3–4 секунды и смотрите на максимум. Если выше 60 не поднимается: Дисплей → Плавність руху → Адаптивна, энергосбережение выключено."));
        big = U.big(a, "—");
        mx = U.text(a, "максимум при касании: —", 15, U.MUTED, false);
        mx.setGravity(android.view.Gravity.CENTER);
        body.addView(big);
        body.addView(mx);
        pad = new Pad(a);
        body.addView(pad, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 320)));
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
}
