package com.vista.phonetest.tests;

import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

public class FingerprintTest extends BaseTest {
    private int ok, fail;
    private TextView st;
    private CancellationSignal cancel;

    public FingerprintTest() {
        super("finger", "Сканер отпечатка", "Ультразвуковой сканер под экраном");
    }

    @Override
    public String[] permissions() {
        return new String[0];
    }

    @Override
    public void build(LinearLayout body) {
        ok = 0;
        fail = 0;
        body.addView(U.howto(a, "Сначала добавьте свой отпечаток: Настройки → Безпека та конфіденційність → Біометричні дані → Відбитки пальців. Потом нажмите «Проверить» и приложите палец 3 раза. После замены экрана на неоригинальный сканер обычно не работает вообще. После проверки удалите свой отпечаток."));
        st = U.big(a, "—");
        st.setTextSize(22);
        body.addView(st);
        body.addView(U.primary(a, "Проверить", v -> check()));
        body.addView(U.button(a, "Открыть настройки отпечатков", v -> {
            try {
                a.startActivity(new Intent(Settings.ACTION_BIOMETRIC_ENROLL));
            } catch (Exception e) {
                a.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            }
        }));
        refreshAvail();
    }

    private void refreshAvail() {
        BiometricManager bm = a.getSystemService(BiometricManager.class);
        int r = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (r == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            st.setText("Отпечаток не добавлен");
            st.setTextColor(U.WARN);
        } else if (r == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE || r == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
            st.setText("Сканер недоступен");
            st.setTextColor(U.BAD);
            setNote("сканер недоступен системе");
        }
    }

    private void check() {
        BiometricManager bm = a.getSystemService(BiometricManager.class);
        int r = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (r != BiometricManager.BIOMETRIC_SUCCESS) {
            refreshAvail();
            return;
        }
        cancel = new CancellationSignal();
        BiometricPrompt p = new BiometricPrompt.Builder(a)
                .setTitle("Проверка сканера")
                .setSubtitle("Приложите палец (" + (ok + 1) + " из 3)")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButton("Отмена", a.getMainExecutor(), (d, w) -> {})
                .build();
        p.authenticate(cancel, a.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                ok++;
                show();
                if (ok < 3) st.postDelayed(() -> check(), 400);
            }

            @Override
            public void onAuthenticationFailed() {
                fail++;
                show();
            }

            @Override
            public void onAuthenticationError(int code, CharSequence msg) {
                if (code != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED && code != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                    st.setText(String.valueOf(msg));
                    st.setTextColor(U.WARN);
                }
            }
        });
    }

    private void show() {
        st.setText("Успешно " + ok + " / ошибок " + fail);
        st.setTextColor(ok >= 3 ? U.OK : U.FG);
        setNote("распознан " + ok + " раз, не распознан " + fail);
    }

    @Override
    public void stop() {
        if (cancel != null) cancel.cancel();
    }
}
