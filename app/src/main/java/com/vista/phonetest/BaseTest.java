package com.vista.phonetest;

import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

/** One check in the list. Subclasses build their own UI and release resources in stop(). */
public abstract class BaseTest {
    public final String id;
    public final String title;
    public final String desc;
    protected MainActivity a;
    private String note = "";

    protected BaseTest(String id, String title, String desc) {
        this.id = id;
        this.title = title;
        this.desc = desc;
    }

    public void attach(MainActivity a) {
        this.a = a;
        this.note = "";
    }

    /** Builds the test's content inside the given column. */
    public abstract void build(LinearLayout body);

    /** Releases sensors, cameras, audio. Called when the test closes. */
    public void stop() {}

    /** Runtime permissions this test needs; requested before build(). */
    public String[] permissions() {
        return new String[0];
    }

    /** Return true to consume a hardware key while this test is open. */
    public boolean onKey(KeyEvent e) {
        return false;
    }

    protected void setNote(String s) {
        note = s == null ? "" : s;
    }

    public String getNote() {
        return note;
    }

    protected void ui(Runnable r) {
        if (a != null) a.runOnUiThread(r);
    }

    protected void add(LinearLayout body, View v) {
        body.addView(v);
    }
}
