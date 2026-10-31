package com.vista.phonetest.tests;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vista.phonetest.BaseTest;
import com.vista.phonetest.U;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CameraTest extends BaseTest {
    private CameraManager cm;
    private HandlerThread th;
    private Handler h;
    private CameraDevice dev;
    private CameraCaptureSession session;
    private CaptureRequest.Builder req;
    private TextureView tv;
    private TextView info;
    private LinearLayout zoomRow;
    private Button torchBtn;
    private boolean torch;
    private final Set<String> opened = new LinkedHashSet<>();
    private int total;
    private String curLabel = "";

    private static class Cam {
        String id, physical, label;
    }

    public CameraTest() {
        super("cam", "Камеры и вспышка", "Каждый модуль отдельно, фокус, зум");
    }

    @Override
    public String[] permissions() {
        return new String[]{Manifest.permission.CAMERA};
    }

    @Override
    public void build(LinearLayout body) {
        opened.clear();
        if (a.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            body.addView(U.note(a, "Нет разрешения на камеру. Закройте тест и откройте снова, разрешите доступ."));
            return;
        }
        body.addView(U.howto(a, "Переключите каждую камеру. Наведите на текст с 10–15 см: картинка должна сфокусироваться. Проверьте, нет ли пятен, пыли, мутности. У основной камеры попробуйте кнопки зума — на 3× и 10× включаются телеобъективы. Мп в названии — реальное разрешение модуля."));
        cm = a.getSystemService(CameraManager.class);
        th = new HandlerThread("cam");
        th.start();
        h = new Handler(th.getLooper());

        List<Cam> cams = listCameras();
        total = cams.size();
        HorizontalScrollView hs = new HorizontalScrollView(a);
        LinearLayout chips = new LinearLayout(a);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(chips);
        body.addView(hs);
        for (Cam c : cams) {
            Button b = U.button(a, c.label, v -> open(c));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = U.dp(a, 6);
            chips.addView(b, lp);
        }

        tv = new TextureView(a);
        body.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, U.dp(a, 420)));
        info = U.text(a, "", 13, U.MUTED, false);
        body.addView(info);
        zoomRow = new LinearLayout(a);
        zoomRow.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(zoomRow);
        torchBtn = U.button(a, "Вспышка: выкл", v -> toggleTorch());
        body.addView(torchBtn);

        if (!cams.isEmpty()) {
            Cam first = cams.get(0);
            if (tv.isAvailable()) open(first);
            else tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int hh) {
                    open(first);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int hh) {}

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture st) {}
            });
        } else {
            info.setText("Система не отдала ни одной камеры.");
        }
    }

    private List<Cam> listCameras() {
        List<Cam> out = new ArrayList<>();
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                Cam c = new Cam();
                c.id = id;
                c.label = describe(ch, id);
                out.add(c);
                Set<String> phys = ch.getPhysicalCameraIds();
                if (phys != null && phys.size() > 1) {
                    List<String> sorted = new ArrayList<>(phys);
                    Collections.sort(sorted);
                    for (String p : sorted) {
                        try {
                            CameraCharacteristics pc = cm.getCameraCharacteristics(p);
                            Cam pcam = new Cam();
                            pcam.id = id;
                            pcam.physical = p;
                            pcam.label = "  ↳ " + describe(pc, p);
                            out.add(pcam);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            // handled by empty list
        }
        return out;
    }

    private String describe(CameraCharacteristics ch, String id) {
        Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
        String f = facing == null ? "?" : facing == CameraMetadata.LENS_FACING_FRONT ? "Фронт" : facing == CameraMetadata.LENS_FACING_BACK ? "Задняя" : "Внешняя";
        Size px = ch.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        float[] fl = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        String mp = px == null ? "" : String.format(Locale.US, " %.0f Мп", px.getWidth() * (double) px.getHeight() / 1e6);
        String mm = fl == null || fl.length == 0 ? "" : String.format(Locale.US, " %.1f мм", fl[0]);
        return f + " #" + id + mp + mm;
    }

    @SuppressLint("MissingPermission")
    private void open(Cam c) {
        closeCamera();
        curLabel = c.label.trim();
        info.setText("Открываю " + curLabel + "…");
        try {
            cm.openCamera(c.id, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice d) {
                    dev = d;
                    startPreview(c);
                }

                @Override
                public void onDisconnected(CameraDevice d) {
                    d.close();
                }

                @Override
                public void onError(CameraDevice d, int error) {
                    d.close();
                    ui(() -> info.setText("Ошибка камеры " + curLabel + ": код " + error + ". Код 3–4 может означать неисправный модуль."));
                }
            }, h);
        } catch (Exception e) {
            info.setText("Не удалось открыть: " + e.getMessage());
        }
    }

    private void startPreview(Cam c) {
        try {
            CameraCharacteristics ch = cm.getCameraCharacteristics(c.physical != null ? c.physical : c.id);
            StreamConfigurationMap map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size best = new Size(1280, 720);
            if (map != null) {
                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                long bestArea = 0;
                for (Size s : sizes) {
                    long area = (long) s.getWidth() * s.getHeight();
                    if (s.getWidth() <= 1920 && s.getHeight() <= 1440 && area > bestArea) {
                        bestArea = area;
                        best = s;
                    }
                }
            }
            final Size size = best;
            SurfaceTexture st = tv.getSurfaceTexture();
            if (st == null) return;
            st.setDefaultBufferSize(size.getWidth(), size.getHeight());
            Surface surface = new Surface(st);
            ui(() -> {
                int w = tv.getWidth();
                ViewGroup.LayoutParams lp = tv.getLayoutParams();
                lp.height = Math.max(U.dp(a, 200), w * size.getWidth() / size.getHeight());
                tv.setLayoutParams(lp);
            });

            req = dev.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            req.addTarget(surface);
            req.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            int[] afModes = ch.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (afModes != null) for (int m : afModes)
                if (m == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    req.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            torch = false;

            OutputConfiguration oc = new OutputConfiguration(surface);
            if (c.physical != null) oc.setPhysicalCameraId(c.physical);
            List<OutputConfiguration> outs = new ArrayList<>();
            outs.add(oc);
            SessionConfiguration sc = new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outs, a.getMainExecutor(), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession s) {
                    session = s;
                    try {
                        s.setRepeatingRequest(req.build(), null, h);
                        opened.add(c.label);
                        info.setText(curLabel + " · превью " + size.getWidth() + "×" + size.getHeight());
                        setNote("открыто камер " + opened.size() + " из " + total);
                        buildZoom(c);
                        Boolean flash = cm.getCameraCharacteristics(c.id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        torchBtn.setEnabled(flash != null && flash);
                        torchBtn.setText(flash != null && flash ? "Вспышка: выкл" : "Вспышки у этой камеры нет");
                    } catch (Exception e) {
                        info.setText("Превью не запустилось: " + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession s) {
                    info.setText(curLabel + ": система не даёт открыть этот модуль отдельно. Проверьте его зумом основной камеры или в штатной «Камере».");
                }
            });
            dev.createCaptureSession(sc);
        } catch (Exception e) {
            ui(() -> info.setText("Ошибка: " + e.getMessage()));
        }
    }

    private void buildZoom(Cam c) {
        zoomRow.removeAllViews();
        if (c.physical != null) return;
        try {
            CameraCharacteristics ch = cm.getCameraCharacteristics(c.id);
            Range<Float> r = ch.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
            if (r == null) return;
            float[] presets = {0.6f, 1f, 3f, 10f, 30f, 100f};
            for (float z : presets) {
                float zz = Math.max(r.getLower(), Math.min(r.getUpper(), z));
                if (z < r.getLower() - 0.05f || z > r.getUpper() + 0.05f) continue;
                String lbl = (zz < 1 ? U.fmt(zz, 1) : String.valueOf(Math.round(zz))) + "×";
                Button b = U.button(a, lbl, v -> {
                    try {
                        req.set(CaptureRequest.CONTROL_ZOOM_RATIO, zz);
                        session.setRepeatingRequest(req.build(), null, h);
                        info.setText(curLabel + " · зум " + lbl);
                    } catch (Exception e) {
                        info.setText("Зум не применился: " + e.getMessage());
                    }
                });
                zoomRow.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
        } catch (Exception ignored) {
        }
    }

    private void toggleTorch() {
        if (session == null || req == null) return;
        torch = !torch;
        try {
            req.set(CaptureRequest.FLASH_MODE, torch ? CameraMetadata.FLASH_MODE_TORCH : CameraMetadata.FLASH_MODE_OFF);
            session.setRepeatingRequest(req.build(), null, h);
            torchBtn.setText(torch ? "Вспышка: вкл" : "Вспышка: выкл");
        } catch (Exception e) {
            info.setText("Вспышка не включилась: " + e.getMessage());
        }
    }

    private void closeCamera() {
        try {
            if (session != null) session.close();
        } catch (Exception ignored) {
        }
        session = null;
        if (dev != null) dev.close();
        dev = null;
    }

    @Override
    public void stop() {
        closeCamera();
        if (th != null) th.quitSafely();
        th = null;
    }
}
