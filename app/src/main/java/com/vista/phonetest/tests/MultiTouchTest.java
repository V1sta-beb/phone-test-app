package com.vista.phonetest.tests;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class MultiTouchTest extends BaseTest {
    private int max;

    public MultiTouchTest() {
        super("multi", "Мультитач", "Сколько пальцев видит экран");
    }

    @Override
    public void build(LinearLayout body) {
        max = 0;
        body.addView(U.howto(a, "Положите на поле все пальцы обеих рук. Флагман Samsung видит 10 касаний одновременно."));
        TextView now = U.big(a, "0");
        TextView mx = U.text(a, "максимум: 0", 15, U.MUTED, false);
        mx.setGravity(android.view.Gravity.CENTER);
        body.addView(now);
        body.addView(mx);
        Pad pad = new Pad(a, now, mx);
        body.addView(pad, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 380)));
    }

    private class Pad extends View {
        final int[] cols = {0xFF5B8CFF, 0xFF3FC97C, 0xFFF2B247, 0xFFFF6363, 0xFFC77DFF, 0xFF4DD4E8, 0xFFFF9F43, 0xFFA3E635, 0xFFF472B6, 0xFFE9EDF3};
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextView now, mx;
        MotionEvent last;

        Pad(Context c, TextView now, TextView mx) {
            super(c);
            this.now = now;
            this.mx = mx;
            setBackground(U.box(c, U.CARD, U.LINE, 10));
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (last != null) last.recycle();
            int act = e.getActionMasked();
            last = (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) ? null : MotionEvent.obtain(e);
            int n = last == null ? 0 : e.getPointerCount() - (act == MotionEvent.ACTION_POINTER_UP ? 1 : 0);
            now.setText(String.valueOf(n));
            if (n > max) {
                max = n;
                mx.setText("максимум: " + max);
                setNote("максимум " + max + " касаний");
            }
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            if (last == null) return;
            for (int i = 0; i < last.getPointerCount(); i++) {
                p.setColor(cols[last.getPointerId(i) % cols.length]);
                c.drawCircle(last.getX(i), last.getY(i), U.dp(getContext(), 34), p);
            }
            p.setColor(Color.WHITE);
        }
    }
}
