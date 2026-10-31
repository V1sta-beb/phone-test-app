package com.vista.phonetest.tests;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.view.Display;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.TreeSet;

public class InfoTest extends BaseTest {
    public InfoTest() {
        super("info", "Сведения об устройстве", "Модель, регион, память, экран, батарея");
    }

    public static String prop(String key) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"getprop", key});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s = r.readLine();
            r.close();
            return s == null ? "" : s.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String firstNonEmpty(String... keys) {
        for (String k : keys) {
            String v = prop(k);
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    @Override
    public void build(LinearLayout body) {
        body.addView(U.howto(a, "Сверьте модель с объявлением. Для европейской S23 Ultra — SM-S918B. Регион: SEK — Украина, EUX — Европа. Память показана реальная."));
        TextView[] v = U.kv(a, body, "Модель", "Устройство", "Регион (CSC)", "Android / One UI", "Патч безопасности", "ОЗУ", "Хранилище", "Экран", "Частоты экрана", "Батарея", "Циклов заряда", "Датчики");

        v[0].setText(Build.MANUFACTURER + " " + Build.MODEL);
        v[1].setText(Build.DEVICE);

        String csc = firstNonEmpty("ro.csc.sales_code", "persist.omc.sales_code", "ro.boot.sales_code", "persist.sys.omc_salescode");
        String country = firstNonEmpty("ro.csc.country_code", "ro.csc.countryiso_code");
        v[2].setText(csc.isEmpty() ? "не удалось прочитать" : csc + (country.isEmpty() ? "" : " (" + country + ")"));

        String oneui = prop("ro.build.version.oneui");
        String oneuiTxt = "";
        try {
            if (!oneui.isEmpty()) {
                int n = Integer.parseInt(oneui);
                oneuiTxt = " / One UI " + (n / 10000) + "." + ((n / 100) % 100);
            }
        } catch (NumberFormatException ignored) {
        }
        v[3].setText(Build.VERSION.RELEASE + oneuiTxt);
        v[4].setText(Build.VERSION.SECURITY_PATCH);

        ActivityManager am = a.getSystemService(ActivityManager.class);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        double gb = mi.totalMem / 1073741824.0;
        v[5].setText(String.format(Locale.US, "%.1f ГБ (≈%d ГБ)", gb, roundRam(gb)));

        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        double total = fs.getTotalBytes() / 1e9;
        double free = fs.getAvailableBytes() / 1e9;
        v[6].setText(String.format(Locale.US, "%.0f ГБ, свободно %.0f (≈%d ГБ)", total, free, roundStorage(total)));

        Display d = a.getDisplay();
        if (d != null) {
            Display.Mode m = d.getMode();
            v[7].setText(m.getPhysicalWidth() + "×" + m.getPhysicalHeight());
            TreeSet<Integer> rates = new TreeSet<>();
            for (Display.Mode mm : d.getSupportedModes()) rates.add(Math.round(mm.getRefreshRate()));
            v[8].setText(rates.toString().replace("[", "").replace("]", "") + " Гц");
        }

        Intent bi = a.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bi != null) {
            int level = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bi.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int health = bi.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
            float temp = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
            v[9].setText((level * 100 / Math.max(scale, 1)) + "%, " + temp + "°C, " + healthName(health));
            int cycles = bi.getIntExtra("android.os.extra.CYCLE_COUNT", -1);
            v[10].setText(cycles >= 0 ? String.valueOf(cycles) : "не отдаётся");
            if (cycles >= 0) setNote(Build.MODEL + ", CSC " + (csc.isEmpty() ? "?" : csc) + ", циклов " + cycles);
            else setNote(Build.MODEL + ", CSC " + (csc.isEmpty() ? "?" : csc));
        }

        SensorManager sm = a.getSystemService(SensorManager.class);
        StringBuilder sb = new StringBuilder();
        sb.append(sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null ? "барометр ✓ " : "барометр ✕ ");
        sb.append(sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null ? "приближение ✓ " : "приближение ✕ ");
        sb.append(sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ? "гироскоп ✓" : "гироскоп ✕");
        v[11].setText(sb.toString());

        body.addView(U.text(a, "Быстрые переходы (IMEI и здоровье батареи Android приложениям не отдаёт):", 14, U.FG, true));
        body.addView(U.row(a,
                U.primary(a, "IMEI", x -> open(
                        new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
                        new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode("*#06#"))))),
                U.button(a, "Набрать *#06#", x -> open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode("*#06#")))))));
        body.addView(U.row(a,
                U.primary(a, "Батарея", x -> open(
                        new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
                        new Intent(Intent.ACTION_POWER_USAGE_SUMMARY))),
                U.button(a, "Samsung Members", x -> {
                    Intent m = a.getPackageManager().getLaunchIntentForPackage("com.samsung.android.voc");
                    if (m != null) open(m);
                    else a.toast("Samsung Members не установлен");
                })));
        body.addView(U.text(a, "«IMEI» и «Батарея» открывают Настройки → Про телефон: IMEI видно сразу, здоровье — в пункте «Відомості про батарею» (One UI 6.1 и новее). Если через *#06# IMEI не появился, сотрите последний # и наберите его заново. В Samsung Members: Підтримка → Діагностика телефону → Стан батареї.", 13, U.MUTED, false));

        body.addView(U.text(a, "Циклы заряда: до 300 — отлично, 300–600 — нормально, больше 800 — аккумулятор заметно изношен. Здоровье в процентах смотрите в Настройки → Про телефон → Відомості про батарею.", 13, U.MUTED, false));
    }

    private void open(Intent... intents) {
        for (Intent i : intents) {
            try {
                a.startActivity(i);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
            }
        }
        a.toast("Не удалось открыть");
    }

    private static int roundRam(double gb) {
        int[] sizes = {2, 3, 4, 6, 8, 12, 16, 24};
        for (int s : sizes) if (gb <= s) return s;
        return (int) Math.ceil(gb);
    }

    private static int roundStorage(double gb) {
        int[] sizes = {16, 32, 64, 128, 256, 512, 1024};
        for (int s : sizes) if (gb <= s) return s;
        return (int) Math.ceil(gb);
    }

    static String healthName(int h) {
        switch (h) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "в норме";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "перегрев";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "неисправна";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "перенапряжение";
            case BatteryManager.BATTERY_HEALTH_COLD: return "переохлаждение";
            default: return "статус неизвестен";
        }
    }
}
