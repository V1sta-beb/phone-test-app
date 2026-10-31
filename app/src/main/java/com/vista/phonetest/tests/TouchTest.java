package com.vista.phonetest.tests;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class TouchTest extends BaseTest {
    private TextView result;

    public TouchTest() {
        super("touch", "Сенсор: вся площадь", "Закрасить сетку пальцем");
    }

    @Override
    public void build(LinearLayout body) {
        body.addView(U.howto(a, "Проведите пальцем по всем клеткам, включая изогнутые края и углы. Клетка, которая не закрашивается при касании, — мёртвая зона сенсора. Выйти раньше: кнопка «назад» или жест."));
        result = U.text(a, "", 15, U.MUTED, false);
        body.addView(result);
        body.addView(U.primary(a, "Начать", v -> run()));
    }

    private void run() {
        Grid g = new Grid(a);
        a.enterFullscreen(g, () -> {
            int total = g.cols * g.rows;
            if (g.left == 0) {
                result.setText("Все " + total + " клеток закрашены.");
                result.setTextColor(U.OK);
                setNote("вся сетка закрашена");
            } else {
                result.setText("Не закрашено " + g.left + " из " + total + ". Если они не закрашивались при касании — это мёртвая зона.");
                result.setTextColor(U.WARN);
                setNote("не закрашено " + g.left + " клеток");
            }
        });
    }

    private class Grid extends View {
        final Paint line = new Paint(), fill = new Paint();
        int cell, cols, rows, left;
        boolean[] done;

        Grid(Context c) {
            super(c);
            setBackgroundColor(U.BG);
            line.setColor(Color.parseColor("#3A4456"));
            line.setStrokeWidth(1);
            fill.setColor(U.OK);
            cell = U.dp(c, 36);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            cols = (int) Math.ceil(w / (double) cell);
            rows = (int) Math.ceil(h / (double) cell);
            done = new boolean[cols * rows];
            left = cols * rows;
        }

        @Override
        protected void onDraw(Canvas c) {
            if (done == null) return;
            for (int y = 0; y < rows; y++)
                for (int x = 0; x < cols; x++)
                    if (done[y * cols + x]) c.drawRect(x * cell + 1, y * cell + 1, (x + 1) * cell, (y + 1) * cell, fill);
            for (int x = 0; x <= cols; x++) c.drawLine(x * cell, 0, x * cell, getHeight(), line);
            for (int y = 0; y <= rows; y++) c.drawLine(0, y * cell, getWidth(), y * cell, line);
        }

        private void hit(float fx, float fy) {
            int x = (int) (fx / cell), y = (int) (fy / cell);
            if (x < 0 || y < 0 || x >= cols || y >= rows) return;
            int k = y * cols + x;
            if (!done[k]) {
                done[k] = true;
                left--;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (done == null) return true;
            for (int p = 0; p < e.getPointerCount(); p++) {
                for (int h = 0; h < e.getHistorySize(); h++) hit(e.getHistoricalX(p, h), e.getHistoricalY(p, h));
                hit(e.getX(p), e.getY(p));
            }
            invalidate();
            if (left == 0) post(() -> a.exitFullscreen());
            return true;
        }
    }
}
