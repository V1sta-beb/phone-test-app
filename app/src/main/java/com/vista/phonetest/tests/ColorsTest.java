package com.vista.phonetest.tests;

import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class ColorsTest extends BaseTest {
    private final boolean burn;
    private static final int[] COLORS = {
            Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK,
            Color.rgb(128, 128, 128), Color.YELLOW, Color.CYAN, Color.MAGENTA};
    private static final String[] NAMES = {"Красный", "Зелёный", "Синий", "Белый", "Чёрный", "Серый", "Жёлтый", "Голубой", "Пурпурный"};
    private static final int[] BURN = {
            Color.rgb(127, 127, 127), Color.rgb(58, 58, 58), Color.rgb(26, 26, 26),
            Color.rgb(191, 191, 191), Color.rgb(255, 59, 59), Color.rgb(59, 59, 255)};

    public ColorsTest(boolean burn) {
        super(burn ? "burn" : "colors",
                burn ? "Экран: выгорание" : "Экран: цвета и пиксели",
                burn ? "Серые и тёмные фоны" : "Заливки на весь экран");
        this.burn = burn;
    }

    @Override
    public void build(LinearLayout body) {
        body.addView(U.howto(a, burn
                ? "Яркость на максимум. Выгорание OLED видно на ровном сером: силуэты клавиатуры, строки состояния, кнопок навигации. Тап — следующий фон, «назад» — выход."
                : "Яркость на максимум. Тап — следующий цвет. Ищите точки другого цвета, полосы, пятна, жёлтые или розовые края. На чёрном не должно быть светящихся точек."));
        body.addView(U.primary(a, "Начать", v -> run()));
    }

    private void run() {
        final int[] list = burn ? BURN : COLORS;
        final int[] i = {0};
        FrameLayout f = new FrameLayout(a);
        TextView label = U.text(a, "", 14, Color.WHITE, false);
        label.setBackgroundColor(0xAA000000);
        int p = U.dp(a, 10);
        label.setPadding(p, p / 2, p, p / 2);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        f.addView(label, lp);
        Runnable show = () -> {
            f.setBackgroundColor(list[i[0]]);
            label.setText((burn ? "Фон " : NAMES[i[0]] + " · ") + (burn ? (i[0] + 1) + "/" + list.length : (i[0] + 1) + "/" + list.length));
            label.setAlpha(1f);
            label.animate().alpha(0f).setStartDelay(900).setDuration(300).start();
        };
        show.run();
        f.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                i[0]++;
                if (i[0] >= list.length) {
                    a.exitFullscreen();
                } else {
                    show.run();
                }
            }
            return true;
        });
        a.enterFullscreen(f, () -> setNote(burn ? "фоны просмотрены" : "все цвета просмотрены"));
    }
}
