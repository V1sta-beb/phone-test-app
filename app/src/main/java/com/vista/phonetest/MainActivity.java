package com.vista.phonetest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.vista.phonetest.tests.BarometerTest;
import com.vista.phonetest.tests.ButtonsTest;
import com.vista.phonetest.tests.CameraTest;
import com.vista.phonetest.tests.ChargingTest;
import com.vista.phonetest.tests.ColorsTest;
import com.vista.phonetest.tests.EarpieceTest;
import com.vista.phonetest.tests.FingerprintTest;
import com.vista.phonetest.tests.GpsTest;
import com.vista.phonetest.tests.InfoTest;
import com.vista.phonetest.tests.MicTest;
import com.vista.phonetest.tests.MotionTest;
import com.vista.phonetest.tests.MultiTouchTest;
import com.vista.phonetest.tests.NfcTest;
import com.vista.phonetest.tests.ProximityTest;
import com.vista.phonetest.tests.RefreshTest;
import com.vista.phonetest.tests.SPenTest;
import com.vista.phonetest.tests.SimTest;
import com.vista.phonetest.tests.SpeakerTest;
import com.vista.phonetest.tests.TouchTest;
import com.vista.phonetest.tests.VibrationTest;
import com.vista.phonetest.tests.WirelessTest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 42;

    private final List<BaseTest> tests = new ArrayList<>();
    private SharedPreferences prefs;
    private BaseTest cur;
    private View testScreen;
    private boolean inFullscreen;
    private Runnable fullscreenExit;
    private boolean inReport;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("results", MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(U.BG);
        getWindow().setNavigationBarColor(U.BG);
        preferHighestRefreshRate();

        tests.add(new InfoTest());
        tests.add(new ColorsTest(false));
        tests.add(new ColorsTest(true));
        tests.add(new TouchTest());
        tests.add(new MultiTouchTest());
        tests.add(new RefreshTest());
        tests.add(new FingerprintTest());
        tests.add(new SPenTest());
        tests.add(new SpeakerTest());
        tests.add(new EarpieceTest());
        tests.add(new MicTest());
        tests.add(new VibrationTest());
        tests.add(new ButtonsTest());
        tests.add(new ProximityTest());
        tests.add(new MotionTest());
        tests.add(new BarometerTest());
        tests.add(new CameraTest());
        tests.add(new SimTest());
        tests.add(new WirelessTest());
        tests.add(new GpsTest());
        tests.add(new NfcTest());
        tests.add(new ChargingTest());

        showHome();
    }

    private void preferHighestRefreshRate() {
        try {
            Display d = getDisplay();
            if (d == null) return;
            Display.Mode best = d.getMode();
            for (Display.Mode m : d.getSupportedModes()) {
                if (m.getPhysicalWidth() == best.getPhysicalWidth()
                        && m.getPhysicalHeight() == best.getPhysicalHeight()
                        && m.getRefreshRate() > best.getRefreshRate()) {
                    best = m;
                }
            }
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.preferredDisplayModeId = best.getModeId();
            getWindow().setAttributes(lp);
        } catch (Exception ignored) {
        }
    }

    /* ---------------- home ---------------- */

    private void showHome() {
        cur = null;
        inReport = false;
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(U.BG);
        sv.setFillViewport(true);
        LinearLayout col = U.col(this, 8);
        int p = U.dp(this, 16);
        col.setPadding(p, U.dp(this, 20), p, U.dp(this, 28));
        sv.addView(col);

        col.addView(U.text(this, "Тест телефона", 24, U.FG, true));
        int done = 0;
        for (BaseTest t : tests) if (result(t) != null) done++;
        col.addView(U.text(this, "Пройдено " + done + " из " + tests.size() + ". Идите по порядку, в конце откройте отчёт.", 14, U.MUTED, false));

        for (BaseTest t : tests) col.addView(tile(t));

        Button report = U.primary(this, "Отчёт", v -> showReport());
        Button reset = U.button(this, "Сбросить результаты", null);
        reset.setOnClickListener(v -> {
            if (!"armed".equals(reset.getTag())) {
                reset.setTag("armed");
                reset.setText("Точно сбросить?");
                reset.postDelayed(() -> {
                    reset.setTag(null);
                    reset.setText("Сбросить результаты");
                }, 3000);
            } else {
                prefs.edit().clear().apply();
                showHome();
            }
        });
        col.addView(U.row(this, report, reset));
        col.addView(U.text(this, "IMEI и здоровье аккумулятора в процентах Android не отдаёт приложениям: смотрите в Настройки → Про телефон. Phone Test by Vista.", 12, U.MUTED, false));
        setContentView(sv);
    }

    private View tile(BaseTest t) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        int p = U.dp(this, 14);
        r.setPadding(p, U.dp(this, 12), p, U.dp(this, 12));
        r.setBackground(U.box(this, U.CARD, U.LINE, 12));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(U.text(this, t.title, 16, U.FG, true));
        tx.addView(U.text(this, t.desc, 13, U.MUTED, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        String[] res = result(t);
        TextView st = U.text(this, res == null ? "" : ("ok".equals(res[0]) ? "✓" : "✕"), 20, res != null && "ok".equals(res[0]) ? U.OK : U.BAD, true);
        r.addView(st);
        r.setOnClickListener(v -> openTest(t));
        r.setClickable(true);
        r.setFocusable(true);
        return r;
    }

    /* ---------------- results ---------------- */

    private String[] result(BaseTest t) {
        String v = prefs.getString("r_" + t.id, null);
        if (v == null) return null;
        int i = v.indexOf('|');
        return i < 0 ? new String[]{v, ""} : new String[]{v.substring(0, i), v.substring(i + 1)};
    }

    private void saveResult(BaseTest t, String verdict) {
        prefs.edit().putString("r_" + t.id, verdict + "|" + t.getNote()).apply();
    }

    /* ---------------- test screen ---------------- */

    private void openTest(BaseTest t) {
        cur = t;
        t.attach(this);
        List<String> missing = new ArrayList<>();
        for (String perm : t.permissions()) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) missing.add(perm);
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQ_PERMS);
        } else {
            showTest(t);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS && cur != null) showTest(cur);
    }

    private void showTest(BaseTest t) {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(U.BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        int p = U.dp(this, 12);
        header.setPadding(p, p, p, p);
        Button back = U.button(this, "←", v -> closeTest());
        header.addView(back, new LinearLayout.LayoutParams(U.dp(this, 52), U.dp(this, 48)));
        TextView title = U.text(this, t.title, 19, U.FG, true);
        title.setPadding(U.dp(this, 12), 0, 0, 0);
        header.addView(title);
        screen.addView(header);

        ScrollView sv = new ScrollView(this);
        LinearLayout body = U.col(this, 14);
        body.setPadding(U.dp(this, 16), U.dp(this, 8), U.dp(this, 16), U.dp(this, 16));
        sv.addView(body);
        screen.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button bad = U.button(this, "✕ Проблема", U.BAD, U.BG, v -> {
            saveResult(t, "bad");
            closeTest();
        });
        Button ok = U.button(this, "✓ Работает", U.OK, U.BG, v -> {
            saveResult(t, "ok");
            closeTest();
        });
        bad.setTypeface(Typeface.DEFAULT_BOLD);
        ok.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout footer = U.row(this, bad, ok);
        footer.setPadding(p, p, p, U.dp(this, 16));
        screen.addView(footer);

        testScreen = screen;
        setContentView(screen);
        try {
            t.build(body);
        } catch (Exception e) {
            body.addView(U.note(this, "Ошибка теста: " + e));
        }
    }

    private void closeTest() {
        if (inFullscreen) exitFullscreen();
        if (cur != null) {
            try {
                cur.stop();
            } catch (Exception ignored) {
            }
        }
        cur = null;
        testScreen = null;
        showHome();
    }

    /* ---------------- fullscreen for screen tests ---------------- */

    public void enterFullscreen(View v, Runnable onExit) {
        inFullscreen = true;
        fullscreenExit = onExit;
        setContentView(v);
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getWindow().setAttributes(getWindow().getAttributes());
    }

    public void exitFullscreen() {
        if (!inFullscreen) return;
        inFullscreen = false;
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) c.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        if (testScreen != null) setContentView(testScreen);
        Runnable r = fullscreenExit;
        fullscreenExit = null;
        if (r != null) r.run();
    }

    @Override
    public void onBackPressed() {
        if (inFullscreen) {
            exitFullscreen();
        } else if (cur != null) {
            closeTest();
        } else if (inReport) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (cur != null) {
            try {
                if (cur.onKey(e)) return true;
            } catch (Exception ignored) {
            }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    protected void onDestroy() {
        if (cur != null) {
            try {
                cur.stop();
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    public void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /* ---------------- report ---------------- */

    private void showReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Тест телефона — ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(new Date())).append('\n');
        sb.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(", Android ").append(Build.VERSION.RELEASE).append("\n\n");
        int ok = 0, bad = 0;
        StringBuilder badList = new StringBuilder();
        for (BaseTest t : tests) {
            String[] r = result(t);
            String mark = r == null ? "·" : ("ok".equals(r[0]) ? "✓" : "✕");
            if (r != null && "ok".equals(r[0])) ok++;
            if (r != null && "bad".equals(r[0])) {
                bad++;
                badList.append(badList.length() > 0 ? ", " : "").append(t.title);
            }
            sb.append(mark).append(' ').append(t.title);
            if (r != null && !r[1].isEmpty()) sb.append(" — ").append(r[1]);
            sb.append('\n');
        }
        sb.append("\nPhone Test by Vista");
        String report = sb.toString();

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(U.BG);
        ScrollView sv = new ScrollView(this);
        LinearLayout col = U.col(this, 12);
        int p = U.dp(this, 16);
        col.setPadding(p, U.dp(this, 20), p, p);
        sv.addView(col);
        screen.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        col.addView(U.text(this, "Отчёт", 24, U.FG, true));
        TextView sum = U.big(this, "✓ " + ok + "   ✕ " + bad);
        sum.setTextSize(28);
        col.addView(sum);
        if (bad > 0) {
            TextView w = U.text(this, "Проблемы: " + badList, 15, U.BAD, true);
            col.addView(w);
        }
        TextView body = U.text(this, report, 13, U.FG, false);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextIsSelectable(true);
        int q = U.dp(this, 12);
        body.setPadding(q, q, q, q);
        body.setBackground(U.box(this, U.CARD, U.LINE, 10));
        col.addView(body);

        Button copy = U.primary(this, "Скопировать", v -> {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Отчёт", report));
            toast("Скопировано");
        });
        Button share = U.button(this, "Поделиться", v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(i, "Отправить отчёт"));
        });
        Button back = U.button(this, "Назад", v -> showHome());
        LinearLayout footer = U.row(this, back, share, copy);
        footer.setPadding(p, p, p, U.dp(this, 16));
        screen.addView(footer);
        setContentView(screen);
        cur = null;
        inReport = true;
    }

    public FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
