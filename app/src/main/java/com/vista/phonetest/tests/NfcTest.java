package com.vista.phonetest.tests;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class NfcTest extends BaseTest {
    private NfcAdapter nfc;
    private TextView big, det;
    private int reads;

    public NfcTest() {
        super("nfc", "NFC", "Поднести банковскую карту");
    }

    @Override
    public void build(LinearLayout body) {
        reads = 0;
        nfc = NfcAdapter.getDefaultAdapter(a);
        if (nfc == null) {
            body.addView(U.note(a, "NFC-модуль не найден системой. У S23 Ultra он есть — это плохой знак."));
            setNote("NFC не найден");
            return;
        }
        body.addView(U.howto(a, "Включите NFC в шторке. Поднесите банковскую карту, пропуск или проездной к задней крышке, в районе центра. Карта должна определиться за 1–2 секунды. Приложение ничего с карты не списывает и не читает номер — только видит, что метка есть."));
        big = U.big(a, nfc.isEnabled() ? "Поднесите карту" : "NFC выключен");
        big.setTextSize(24);
        big.setTextColor(nfc.isEnabled() ? U.FG : U.WARN);
        body.addView(big);
        det = U.text(a, "", 14, U.MUTED, false);
        body.addView(det);
        body.addView(U.button(a, "Открыть настройки NFC", v -> a.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS))));
        start();
    }

    private void start() {
        if (nfc == null || !nfc.isEnabled()) return;
        int flags = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_F
                | NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
        nfc.enableReaderMode(a, this::onTag, flags, null);
    }

    private void onTag(Tag tag) {
        reads++;
        StringBuilder tech = new StringBuilder();
        for (String t : tag.getTechList()) {
            tech.append(tech.length() > 0 ? ", " : "").append(t.substring(t.lastIndexOf('.') + 1));
        }
        ui(() -> {
            big.setText("Карта обнаружена ✓");
            big.setTextColor(U.OK);
            det.setText("Срабатываний: " + reads + "\nТип: " + tech);
            setNote("карта обнаружена " + reads + " раз");
        });
    }

    @Override
    public void stop() {
        try {
            if (nfc != null) nfc.disableReaderMode(a);
        } catch (Exception ignored) {
        }
    }
}
