package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;

import com.highcapable.yukihookapi.YukiHookAPI;

public final class KeepScreenOnRouteResolver {
    public static final long STATUS_REQUEST_GRACE_PERIOD_MS = 800L;

    private final Context appContext;
    private final KeepScreenOnRouteSelectionModel selectionModel;
    private final KeepScreenOnXposedRouteSnapshotStore snapshotStore;
    private final KeepScreenOnModuleBridge moduleBridge;
    private final KeepScreenOnSessionManager sessionManager;
    private final KeepScreenOnXposedSessionManager xposedSessionManager;

    public KeepScreenOnRouteResolver(Context context) {
        appContext = context.getApplicationContext();
        selectionModel = new KeepScreenOnRouteSelectionModel();
        snapshotStore = new KeepScreenOnXposedRouteSnapshotStore(appContext);
        moduleBridge = new KeepScreenOnModuleBridge();
        sessionManager = new KeepScreenOnSessionManager(appContext);
        xposedSessionManager = new KeepScreenOnXposedSessionManager(appContext);
    }

    public KeepScreenOnRouteResolution resolveCurrentState(long nowElapsedRealtime) {
        KeepScreenOnState xposedState = xposedSessionManager.reconcileExpiredState(nowElapsedRealtime);
        KeepScreenOnXposedRouteSnapshot snapshot = snapshotStore.read();
        boolean isModuleActive = YukiHookAPI.Status.INSTANCE.isModuleActive();
        if (selectionModel.shouldUseXposed(xposedState, snapshot, isModuleActive, nowElapsedRealtime)) {
            return new KeepScreenOnRouteResolution(
                    KeepScreenOnRoute.XPOSED,
                    xposedState
            );
        }
        return new KeepScreenOnRouteResolution(
                KeepScreenOnRoute.STANDARD,
                sessionManager.readState(nowElapsedRealtime)
        );
    }

    public KeepScreenOnRouteResolution resolveAfterStatusRequest(
            long requestedAtElapsedRealtime,
            long nowElapsedRealtime
    ) {
        KeepScreenOnState xposedState = xposedSessionManager.reconcileExpiredState(nowElapsedRealtime);
        KeepScreenOnXposedRouteSnapshot snapshot = snapshotStore.read();
        boolean isModuleActive = YukiHookAPI.Status.INSTANCE.isModuleActive();
        if (selectionModel.shouldUseXposedAfterStatusRequest(
                snapshot,
                xposedState,
                isModuleActive,
                requestedAtElapsedRealtime,
                nowElapsedRealtime
        )) {
            return new KeepScreenOnRouteResolution(
                    KeepScreenOnRoute.XPOSED,
                    xposedState
            );
        }
        return new KeepScreenOnRouteResolution(
                KeepScreenOnRoute.STANDARD,
                sessionManager.readState(nowElapsedRealtime)
        );
    }

    public void requestStatusRefresh() {
        moduleBridge.requestStatus(appContext);
    }
}
