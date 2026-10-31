package com.vista.phonetest.tests;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.util.List;

public class SimTest extends BaseTest {
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean running;
    private TextView out, net;
    private final boolean[] slotSeen = new boolean[2];

    public SimTest() {
        super("sim", "SIM и сотовая сеть", "Оба слота, оператор, сигнал, интернет");
    }

    @Override
    public String[] permissions() {
        return new String[]{Manifest.permission.READ_PHONE_STATE};
    }

    @Override
    public void build(LinearLayout body) {
        slotSeen[0] = slotSeen[1] = false;
        body.addView(U.howto(a, "Вставьте свою SIM в первый слот лотка: через 10–20 секунд появятся оператор и сигнал. Потом переставьте во второй слот. Подойдёт и SIM только с интернетом. Отключите Wi-Fi, чтобы проверить мобильный интернет.\n\nБез SIM модем всё равно должен видеть сети: на экране блокировки — «Тільки екстрені виклики»."));
        out = U.text(a, "", 14, U.FG, false);
        out.setTypeface(android.graphics.Typeface.MONOSPACE);
        int p = U.dp(a, 12);
        out.setPadding(p, p, p, p);
        out.setBackground(U.box(a, U.CARD, U.LINE, 10));
        body.addView(out);
        net = U.text(a, "", 15, U.FG, true);
        body.addView(net);
        running = true;
        h.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            refresh();
            h.postDelayed(this, 2000);
        }
    };

    @SuppressLint("MissingPermission")
    private void refresh() {
        TelephonyManager tm = a.getSystemService(TelephonyManager.class);
        if (tm == null) {
            out.setText("Телефонный модуль недоступен.");
            return;
        }
        boolean perm = a.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        StringBuilder sb = new StringBuilder();
        int slots = 1;
        try {
            slots = tm.getActiveModemCount();
        } catch (Exception ignored) {
        }
        sb.append("Слотов SIM: ").append(slots).append('\n');
        for (int i = 0; i < slots; i++) {
            int st = TelephonyManager.SIM_STATE_UNKNOWN;
            try {
                st = tm.getSimState(i);
            } catch (Exception ignored) {
            }
            sb.append("Слот ").append(i + 1).append(": ").append(simState(st)).append('\n');
            if (st == TelephonyManager.SIM_STATE_READY && i < 2) slotSeen[i] = true;
        }
        if (perm) {
            try {
                SubscriptionManager sm = a.getSystemService(SubscriptionManager.class);
                List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
                if (subs != null) {
                    for (SubscriptionInfo s : subs) {
                        TelephonyManager t = tm.createForSubscriptionId(s.getSubscriptionId());
                        sb.append('\n').append(s.isEmbedded() ? "eSIM" : "SIM в слоте " + (s.getSimSlotIndex() + 1)).append(": ").append(s.getCarrierName()).append('\n');
                        String op = t.getNetworkOperatorName();
                        sb.append("  сеть: ").append(op == null || op.isEmpty() ? "нет регистрации" : op).append('\n');
                        sb.append("  тип данных: ").append(netType(t.getDataNetworkType())).append('\n');
                        SignalStrength ss = t.getSignalStrength();
                        if (ss != null) sb.append("  сигнал: ").append(ss.getLevel()).append(" из 4").append('\n');
                    }
                }
            } catch (Exception e) {
                sb.append("\nОшибка чтения SIM: ").append(e.getMessage());
            }
        } else {
            sb.append("\nНет разрешения «Телефон»: оператор и сигнал не показываются.");
        }
        out.setText(sb.toString().trim());

        ConnectivityManager cm = a.getSystemService(ConnectivityManager.class);
        boolean cell = false, wifi = false;
        Network n = cm.getActiveNetwork();
        if (n != null) {
            NetworkCapabilities c = cm.getNetworkCapabilities(n);
            if (c != null) {
                cell = c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                wifi = c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }
        }
        net.setText(cell ? "Интернет через SIM работает ✓" : wifi ? "Сейчас интернет через Wi-Fi — отключите Wi-Fi для проверки SIM" : "Мобильного интернета нет");
        net.setTextColor(cell ? U.OK : U.WARN);
        setNote("слот 1 " + (slotSeen[0] ? "✓" : "✕") + ", слот 2 " + (slotSeen[1] ? "✓" : "✕") + ", моб. интернет " + (cell ? "✓" : "✕"));
    }

    private static String simState(int s) {
        switch (s) {
            case TelephonyManager.SIM_STATE_ABSENT: return "пусто";
            case TelephonyManager.SIM_STATE_READY: return "SIM готова ✓";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "нужен PIN";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "нужен PUK";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "ЗАБЛОКИРОВАН под оператора";
            case TelephonyManager.SIM_STATE_NOT_READY: return "загружается…";
            case TelephonyManager.SIM_STATE_PERM_DISABLED: return "SIM отключена";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR: return "ошибка карты/слота";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED: return "карта ограничена";
            default: return "неизвестно";
        }
    }

    private static String netType(int t) {
        switch (t) {
            case TelephonyManager.NETWORK_TYPE_NR: return "5G";
            case TelephonyManager.NETWORK_TYPE_LTE: return "4G LTE";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_UMTS: return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_GSM: return "2G";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN: return "нет";
            default: return "код " + t;
        }
    }

    @Override
    public void stop() {
        running = false;
        h.removeCallbacks(tick);
    }
}
