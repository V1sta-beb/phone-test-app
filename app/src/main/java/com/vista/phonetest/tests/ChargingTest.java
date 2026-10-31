package com.vista.phonetest.tests;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.util.Locale;

public class ChargingTest extends BaseTest {
    private final Handler h = new Handler(Looper.getMainLooper());
    private BroadcastReceiver rcv;
    private boolean running, sawUsb, sawWireless;
    private int drops, maxMa;
    private boolean lastPlugged;
    private TextView big;
    private TextView[] v;

    public ChargingTest() {
        super("chg", "Зарядка", "Кабель, беспроводная, ток заряда");
    }

    @Override
    public void build(LinearLayout body) {
        sawUsb = sawWireless = false;
        drops = 0;
        maxMa = 0;
        body.addView(U.howto(a, "1. Подключите кабель к повербанку или зарядке. Пошевелите штекер в разъёме: зарядка не должна прерываться, счётчик обрывов должен остаться 0.\n2. Отключите кабель и положите телефон спиной на беспроводную зарядку — или включите на своём телефоне «Бездротовий обмін енергією» и положите S23 на его спину.\n\nТок заряда зависит от зарядки и уровня батареи: от повербанка обычно 1–2,5 А, от быстрой зарядки Samsung больше."));
        big = U.big(a, "—");
        big.setTextSize(26);
        body.addView(big);
        v = U.kv(a, body, "Уровень", "Ток", "Максимальный ток", "Напряжение", "Температура", "Обрывов зарядки", "Кабель", "Беспроводная");
        rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                update(i);
            }
        };
        Intent sticky;
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= 33) sticky = a.registerReceiver(rcv, f, Context.RECEIVER_EXPORTED);
        else sticky = a.registerReceiver(rcv, f);
        if (sticky != null) {
            lastPlugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            update(sticky);
        }
        running = true;
        h.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            Intent i = a.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i != null) update(i);
            h.postDelayed(this, 1000);
        }
    };

    private void update(Intent i) {
        if (v == null) return;
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean isPlugged = plugged != 0;
        if (lastPlugged && !isPlugged) drops++;
        lastPlugged = isPlugged;
        if (plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_AC) sawUsb = true;
        if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) sawWireless = true;

        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / Math.max(1, i.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int mv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        float temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
        BatteryManager bm = a.getSystemService(BatteryManager.class);
        long ua = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        int ma = (int) (Math.abs(ua) > 20000 ? ua / 1000 : ua);
        if (isPlugged && ma > maxMa) maxMa = ma;

        String type = plugged == BatteryManager.BATTERY_PLUGGED_AC ? "по кабелю (зарядка)"
                : plugged == BatteryManager.BATTERY_PLUGGED_USB ? "по кабелю (USB)"
                : plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ? "беспроводная"
                : "не заряжается";
        big.setText(type);
        big.setTextColor(isPlugged ? U.OK : U.FG);
        v[0].setText(level + "%");
        v[1].setText(ma + " мА");
        v[2].setText(maxMa + " мА");
        v[3].setText(String.format(Locale.US, "%.2f В", mv / 1000f));
        v[4].setText(temp + " °C" + (temp > 42 ? " — горячая!" : ""));
        v[5].setText(String.valueOf(drops));
        v[6].setText(sawUsb ? "✓" : "—");
        v[7].setText(sawWireless ? "✓" : "—");
        v[5].setTextColor(drops > 0 ? U.BAD : U.FG);
        v[6].setTextColor(sawUsb ? U.OK : U.FG);
        v[7].setTextColor(sawWireless ? U.OK : U.FG);
        setNote("кабель " + (sawUsb ? "✓" : "✕") + ", беспроводная " + (sawWireless ? "✓" : "✕") + ", макс. ток " + maxMa + " мА, обрывов " + drops);
    }

    @Override
    public void stop() {
        running = false;
        h.removeCallbacksAndMessages(null);
        if (rcv != null) {
            try {
                a.unregisterReceiver(rcv);
            } catch (Exception ignored) {
            }
            rcv = null;
        }
    }
}
