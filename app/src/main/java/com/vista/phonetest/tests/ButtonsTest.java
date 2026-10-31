package com.vista.phonetest.tests;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class ButtonsTest extends BaseTest {
    private boolean up, down, power;
    private int upN, downN;
    private TextView[] v;
    private BroadcastReceiver rcv;

    public ButtonsTest() {
        super("keys", "Кнопки", "Громкость и питание");
    }

    @Override
    public void build(LinearLayout body) {
        up = down = power = false;
        upN = downN = 0;
        body.addView(U.howto(a, "Нажмите «громче» и «тише» по несколько раз: каждое нажатие засчитывается. Затем коротко нажмите боковую кнопку — экран погаснет — и включите его снова. Кнопки должны нажиматься с чётким щелчком, не проваливаться и не залипать."));
        v = U.kv(a, body, "Громче", "Тише", "Питание");
        update();
        rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
                    power = true;
                    update();
                }
            }
        };
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (android.os.Build.VERSION.SDK_INT >= 33) a.registerReceiver(rcv, f, Context.RECEIVER_EXPORTED);
        else a.registerReceiver(rcv, f);
    }

    private void update() {
        v[0].setText(up ? "✓ (" + upN + ")" : "нажмите");
        v[1].setText(down ? "✓ (" + downN + ")" : "нажмите");
        v[2].setText(power ? "✓" : "нажмите");
        v[0].setTextColor(up ? U.OK : U.FG);
        v[1].setTextColor(down ? U.OK : U.FG);
        v[2].setTextColor(power ? U.OK : U.FG);
        setNote("громче " + (up ? "✓" : "✕") + ", тише " + (down ? "✓" : "✕") + ", питание " + (power ? "✓" : "✕"));
    }

    @Override
    public boolean onKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (k != KeyEvent.KEYCODE_VOLUME_UP && k != KeyEvent.KEYCODE_VOLUME_DOWN) return false;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            if (k == KeyEvent.KEYCODE_VOLUME_UP) {
                up = true;
                upN++;
            } else {
                down = true;
                downN++;
            }
            update();
        }
        return true;
    }

    @Override
    public void stop() {
        if (rcv != null) {
            try {
                a.unregisterReceiver(rcv);
            } catch (Exception ignored) {
            }
            rcv = null;
        }
    }
}
