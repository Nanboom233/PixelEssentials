package top.nanboom233.pixelessentials.flashlight;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class FlashlightAccessibilityService extends AccessibilityService {
    private static final String TAG = ShortcutEntryActivity.TAG;
    private static final long MAX_INTERVAL_MS = 500L;
    private static final long DEBOUNCE_MS = 800L;

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn;
    private long volumeUpPressedTime = -1L;
    private long volumeDownPressedTime = -1L;
    private long lastToggleTime;

    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String camId, boolean enabled) {
            if (camId.equals(cameraId)) {
                isFlashOn = enabled;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = findFlashCameraId();
            if (cameraId == null) {
                YukiDiagnosticsLog.warn(TAG, "Flashlight: no camera with flash found");
            }
        } catch (CameraAccessException e) {
            YukiDiagnosticsLog.warn(TAG, "Flashlight: failed to enumerate cameras", e);
        }
        if (cameraManager != null) {
            cameraManager.registerTorchCallback(torchCallback, new Handler(Looper.getMainLooper()));
        }
        YukiDiagnosticsLog.info(TAG, "FlashlightAccessibilityService created, cameraId=" + cameraId);
    }

    private String findFlashCameraId() throws CameraAccessException {
        String[] ids = cameraManager.getCameraIdList();
        String fallback = null;
        for (String id : ids) {
            CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
            Boolean hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (!Boolean.TRUE.equals(hasFlash)) {
                continue;
            }
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
            if (fallback == null) {
                fallback = id;
            }
        }
        return fallback;
    }

    @Override
    public void onDestroy() {
        if (cameraManager != null) {
            cameraManager.unregisterTorchCallback(torchCallback);
        }
        YukiDiagnosticsLog.info(TAG, "FlashlightAccessibilityService destroyed");
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        YukiDiagnosticsLog.info(TAG, "FlashlightAccessibilityService connected, key filter enabled");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyEvent(event);
        }
        long now = SystemClock.elapsedRealtime();
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressedTime = now;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDownPressedTime = now;
        } else {
            return super.onKeyEvent(event);
        }

        if (volumeUpPressedTime < 0 || volumeDownPressedTime < 0) {
            return super.onKeyEvent(event);
        }
        if (Math.abs(volumeUpPressedTime - volumeDownPressedTime) > MAX_INTERVAL_MS) {
            return super.onKeyEvent(event);
        }
        if (now - lastToggleTime < DEBOUNCE_MS) {
            return true;
        }

        toggleFlashlight();
        lastToggleTime = now;
        volumeUpPressedTime = -1L;
        volumeDownPressedTime = -1L;
        return true;
    }

    private void toggleFlashlight() {
        if (cameraManager == null || cameraId == null) {
            return;
        }
        boolean target = !isFlashOn;
        try {
            cameraManager.setTorchMode(cameraId, target);
        } catch (CameraAccessException e) {
            YukiDiagnosticsLog.warn(TAG, "Flashlight: toggle failed", e);
        }
    }
}
