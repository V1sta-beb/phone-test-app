package com.vista.phonetest.tests;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class WirelessTest extends BaseTest {
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean running, wifiToggled, btToggled, wifiConnected;
    private int bestRssi = -200;
    private TextView[] v;
    private BroadcastReceiver rcv;

    public WirelessTest() {
        super("wifi", "Wi-Fi и Bluetooth", "Подключение, сигнал, включение модулей");
    }

    @Override
    public void build(LinearLayout body) {
        wifiToggled = btToggled = wifiConnected = false;
        bestRssi = -200;
        body.addView(U.howto(a, "Подключитесь к точке доступа со своего телефона. Затем в шторке выключите и снова включите Wi-Fi и Bluetooth: приложение должно увидеть оба переключения. Если модуль не включается или сразу выключается — он неисправен."));
        v = U.kv(a, body, "Wi-Fi", "Сигнал Wi-Fi", "Wi-Fi переключался", "Bluetooth", "Bluetooth переключался");
        rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String act = i.getAction();
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(act)) {
                    int s = i.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                    if (s == WifiManager.WIFI_STATE_ENABLED || s == WifiManager.WIFI_STATE_DISABLED) wifiToggled = true;
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(act)) {
                    int s = i.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (s == BluetoothAdapter.STATE_ON || s == BluetoothAdapter.STATE_OFF) btToggled = true;
                }
                refresh();
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= 33) a.registerReceiver(rcv, f, Context.RECEIVER_EXPORTED);
        else a.registerReceiver(rcv, f);
        // The sticky Wi-Fi state broadcast arrives immediately on register; don't count it as a toggle.
        h.postDelayed(() -> {
            wifiToggled = false;
            btToggled = false;
            refresh();
        }, 700);
        running = true;
        h.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            refresh();
            h.postDelayed(this, 1500);
        }
    };

    private void refresh() {
        if (v == null) return;
        WifiManager wm = a.getApplicationContext().getSystemService(WifiManager.class);
        boolean wOn = wm != null && wm.isWifiEnabled();
        ConnectivityManager cm = a.getSystemService(ConnectivityManager.class);
        Integer rssi = null;
        boolean connected = false;
        for (Network n : cm.getAllNetworks()) {
            NetworkCapabilities c = cm.getNetworkCapabilities(n);
            if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                connected = true;
                if (android.os.Build.VERSION.SDK_INT >= 31 && c.getTransportInfo() instanceof WifiInfo) {
                    rssi = ((WifiInfo) c.getTransportInfo()).getRssi();
                } else if (c.getSignalStrength() != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                    rssi = c.getSignalStrength();
                }
            }
        }
        if (connected) wifiConnected = true;
        if (rssi != null && rssi > bestRssi && rssi < 0) bestRssi = rssi;
        v[0].setText(!wOn ? "выключен" : connected ? "подключён ✓" : "включён, не подключён");
        v[1].setText(rssi == null || rssi >= 0 ? "—" : rssi + " дБм (" + (rssi > -60 ? "отлично" : rssi > -75 ? "нормально" : "слабо") + ")");
        v[2].setText(wifiToggled ? "✓" : "выключите и включите");

        String bt = "недоступен";
        try {
            BluetoothManager bm = a.getSystemService(BluetoothManager.class);
            BluetoothAdapter ad = bm == null ? null : bm.getAdapter();
            if (ad != null) bt = ad.isEnabled() ? "включён" : "выключен";
        } catch (Exception e) {
            bt = "состояние скрыто системой";
        }
        v[3].setText(bt);
        v[4].setText(btToggled ? "✓" : "выключите и включите");
        v[0].setTextColor(connected ? U.OK : U.FG);
        v[2].setTextColor(wifiToggled ? U.OK : U.FG);
        v[4].setTextColor(btToggled ? U.OK : U.FG);
        setNote("Wi-Fi " + (wifiConnected ? "подключался" : "не подключался") + (bestRssi > -200 ? " (" + bestRssi + " дБм)" : "")
                + ", переключение Wi-Fi " + (wifiToggled ? "✓" : "✕") + ", Bluetooth " + (btToggled ? "✓" : "✕"));
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
