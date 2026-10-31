package com.vista.phonetest.tests;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

/**
 * Volume keys are detected two ways: directly as key events, and through the
 * system's volume-changed broadcast. Some One UI setups (Sound Assistant,
 * accessibility shortcuts) swallow the key event before the app sees it, but
 * the volume still changes, so the broadcast catches those presses.
 */
public class ButtonsTest extends BaseTest {
    private static final String VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_STREAM = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final String EXTRA_PREV = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";

    private boolean up, down, power;
    private int upN, downN;
    private TextView[] v;
    private TextView last;
    private BroadcastReceiver rcv;
    private long lastKeyAt;

    public ButtonsTest() {
        super("keys", "Кнопки", "Громкость и питание");
    }

    @Override
    public void build(LinearLayout body) {
        up = down = power = false;
        upN = downN = 0;
        a.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AudioManager am = a.getSystemService(AudioManager.class);
        int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC), max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (cur == 0 || cur == max) am.setStreamVolume(AudioManager.STREAM_MUSIC, max / 2, 0);

        body.addView(U.howto(a, "Нажмите «громче» и «тише» по несколько раз: каждое нажатие засчитывается. Громкость при этом будет меняться — так и должно быть. Затем коротко нажмите боковую кнопку — экран погаснет — и включите его снова. Кнопки должны нажиматься с чётким щелчком, не проваливаться и не залипать."));
        v = U.kv(a, body, "Громче", "Тише", "Питание");
        last = U.text(a, "", 13, U.MUTED, false);
        body.addView(last);
        update();

        rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String act = i.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(act)) {
                    power = true;
                    last.setText("Последнее: экран выключен кнопкой питания");
                    update();
                } else if (VOLUME_CHANGED.equals(act)) {
                    // Key events already counted this press.
                    if (System.currentTimeMillis() - lastKeyAt < 400) return;
                    int val = i.getIntExtra(EXTRA_VALUE, -1), prev = i.getIntExtra(EXTRA_PREV, -1);
                    if (val < 0 || prev < 0 || val == prev) return;
                    if (val > prev) {
                        up = true;
                        upN++;
                    } else {
                        down = true;
                        downN++;
                    }
                    last.setText("Последнее: громкость " + prev + " → " + val + " (поток " + i.getIntExtra(EXTRA_STREAM, -1) + ")");
                    update();
                }
            }
        };
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        f.addAction(VOLUME_CHANGED);
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
            lastKeyAt = System.currentTimeMillis();
            if (k == KeyEvent.KEYCODE_VOLUME_UP) {
                up = true;
                upN++;
            } else {
                down = true;
                downN++;
            }
            last.setText("Последнее: " + KeyEvent.keyCodeToString(k));
            update();
        }
        // Let the system handle it too, so the volume visibly changes.
        return false;
    }

    @Override
    public void stop() {
        a.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        if (rcv != null) {
            try {
                a.unregisterReceiver(rcv);
            } catch (Exception ignored) {
            }
            rcv = null;
        }
    }
}
