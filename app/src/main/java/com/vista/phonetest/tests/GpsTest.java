package com.vista.phonetest.tests;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.util.Locale;

public class GpsTest extends BaseTest {
    private LocationManager lm;
    private TextView[] v;
    private long t0, fixAt;
    private float best = Float.MAX_VALUE;
    private int maxUsed;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean running;

    private final LocationListener ll = new LocationListener() {
        @Override
        public void onLocationChanged(Location l) {
            if (fixAt == 0) fixAt = SystemClock.elapsedRealtime();
            best = Math.min(best, l.getAccuracy());
            v[2].setText(String.format(Locale.US, "%.0f м (лучшая %.0f м)", l.getAccuracy(), best));
            v[3].setText(String.format(Locale.US, "%.5f, %.5f", l.getLatitude(), l.getLongitude()));
            v[1].setText(((fixAt - t0) / 1000) + " с");
            note();
        }

        @Override
        public void onProviderDisabled(String p) {
            v[0].setText("геолокация выключена");
        }

        @Override
        public void onProviderEnabled(String p) {
            v[0].setText("ищу спутники…");
        }
    };

    private final GnssStatus.Callback gc = new GnssStatus.Callback() {
        @Override
        public void onSatelliteStatusChanged(GnssStatus s) {
            int vis = s.getSatelliteCount(), used = 0, gps = 0, glo = 0, gal = 0, bds = 0;
            for (int i = 0; i < vis; i++) {
                if (s.usedInFix(i)) used++;
                switch (s.getConstellationType(i)) {
                    case GnssStatus.CONSTELLATION_GPS: gps++; break;
                    case GnssStatus.CONSTELLATION_GLONASS: glo++; break;
                    case GnssStatus.CONSTELLATION_GALILEO: gal++; break;
                    case GnssStatus.CONSTELLATION_BEIDOU: bds++; break;
                    default: break;
                }
            }
            maxUsed = Math.max(maxUsed, used);
            v[0].setText("видно " + vis + ", в решении " + used);
            v[4].setText("GPS " + gps + " · ГЛОНАСС " + glo + " · Galileo " + gal + " · BeiDou " + bds);
            note();
        }
    };

    public GpsTest() {
        super("gps", "GPS", "Спутники, точность, время до фиксации");
    }

    @Override
    public String[] permissions() {
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    @SuppressLint("MissingPermission")
    @Override
    public void build(LinearLayout body) {
        best = Float.MAX_VALUE;
        fixAt = 0;
        maxUsed = 0;
        if (a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            body.addView(U.note(a, "Нет разрешения на точную геолокацию. Закройте тест и откройте снова, выберите «Точное»."));
            return;
        }
        body.addView(U.howto(a, "Включите местоположение в шторке, встаньте у окна или выйдите на улицу. Через 10–60 секунд должны появиться спутники и координаты. Исправный модуль в помещении у окна видит 10–30 спутников и даёт точность 5–30 м."));
        v = U.kv(a, body, "Спутники", "Время до фиксации", "Точность", "Координаты", "Системы");
        lm = a.getSystemService(LocationManager.class);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) v[0].setText("геолокация выключена");
        t0 = SystemClock.elapsedRealtime();
        lm.registerGnssStatusCallback(a.getMainExecutor(), gc);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, ll, Looper.getMainLooper());
        running = true;
        h.post(new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                if (fixAt == 0) v[1].setText("ждём… " + ((SystemClock.elapsedRealtime() - t0) / 1000) + " с");
                h.postDelayed(this, 1000);
            }
        });
    }

    private void note() {
        String fix = fixAt == 0 ? "нет фиксации" : "фиксация за " + ((fixAt - t0) / 1000) + " с, точность " + Math.round(best) + " м";
        setNote(fix + ", спутников в решении до " + maxUsed);
    }

    @Override
    public void stop() {
        running = false;
        h.removeCallbacksAndMessages(null);
        if (lm != null) {
            lm.removeUpdates(ll);
            lm.unregisterGnssStatusCallback(gc);
        }
    }
}
