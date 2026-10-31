package com.vista.phonetest;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small UI helpers so the whole app is built in code, without XML layouts. */
public final class U {
    public static final int BG = Color.parseColor("#0B0E13");
    public static final int CARD = Color.parseColor("#151A22");
    public static final int CARD2 = Color.parseColor("#1C2330");
    public static final int FG = Color.parseColor("#E9EDF3");
    public static final int MUTED = Color.parseColor("#8E98A9");
    public static final int LINE = Color.parseColor("#283142");
    public static final int ACCENT = Color.parseColor("#5B8CFF");
    public static final int OK = Color.parseColor("#3FC97C");
    public static final int BAD = Color.parseColor("#FF6363");
    public static final int WARN = Color.parseColor("#F2B247");

    private U() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable box(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(dp(c, 1), stroke);
        return g;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    public static TextView big(Context c, String s) {
        TextView t = text(c, s, 40, FG, true);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    public static TextView howto(Context c, String s) {
        TextView t = text(c, s, 14, MUTED, false);
        int p = dp(c, 12);
        t.setPadding(p, p, p, p);
        t.setBackground(box(c, CARD, LINE, 10));
        return t;
    }

    public static TextView note(Context c, String s) {
        return text(c, s, 13, WARN, false);
    }

    public static Button button(Context c, String label, View.OnClickListener l) {
        return button(c, label, CARD2, FG, l);
    }

    public static Button primary(Context c, String label, View.OnClickListener l) {
        return button(c, label, ACCENT, Color.WHITE, l);
    }

    public static Button button(Context c, String label, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setBackground(box(c, bg, bg == CARD2 ? LINE : 0, 10));
        b.setMinHeight(dp(c, 48));
        b.setStateListAnimator(null);
        b.setOnClickListener(l);
        return b;
    }

    public static LinearLayout col(Context c, int gapDp) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        l.setDividerDrawable(spacer(c, gapDp));
        return l;
    }

    public static LinearLayout row(Context c, View... views) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        l.setDividerDrawable(spacer(c, 8));
        for (View v : views) {
            l.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        return l;
    }

    private static GradientDrawable spacer(Context c, int gapDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.TRANSPARENT);
        d.setSize(dp(c, gapDp), dp(c, gapDp));
        return d;
    }

    /** A two-column list of label/value rows. Returns the value TextViews in order. */
    public static TextView[] kv(Context c, LinearLayout parent, String... labels) {
        LinearLayout box = col(c, 6);
        int p = dp(c, 12);
        box.setPadding(p, p, p, p);
        box.setBackground(box(c, CARD, LINE, 10));
        TextView[] vals = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            LinearLayout r = new LinearLayout(c);
            r.setOrientation(LinearLayout.HORIZONTAL);
            TextView k = text(c, labels[i], 14, MUTED, false);
            TextView v = text(c, "—", 14, FG, false);
            v.setTypeface(Typeface.MONOSPACE);
            v.setGravity(Gravity.END);
            r.addView(k, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
            box.addView(r);
            vals[i] = v;
        }
        parent.addView(box);
        return vals;
    }

    public static String fmt(float v, int digits) {
        return String.format(java.util.Locale.US, "%." + digits + "f", v);
    }
}
