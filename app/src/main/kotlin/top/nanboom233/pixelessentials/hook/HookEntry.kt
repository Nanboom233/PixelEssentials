package top.nanboom233.pixelessentials.hook

import top.nanboom233.pixelessentials.BuildConfig
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    private const val TAG = "PixelEssentialsXposed"

    override fun onInit() = YukiHookAPI.configs {
        debugLog {
            tag = "PixelEssentialsXposed"
            isEnable = true
            isRecord = false
        }
        isDebug = BuildConfig.DEBUG
        YukiDiagnosticsLog.info(TAG, "HookEntry onInit completed. debug=${BuildConfig.DEBUG}")
    }

    override fun onHook() = YukiHookAPI.encase {
        YukiDiagnosticsLog.info(TAG, "HookEntry onHook installing SystemUI lifecycle hook")
        loadApp(name = "com.android.systemui") {
            onAppLifecycle(isOnFailureThrowToApp = false) {
                onCreate {
                    YukiDiagnosticsLog.info(TAG, "HookEntry observed SystemUI Application.onCreate")
                    SystemUiKeepScreenOnController.install(this)
                }
            }
        }
    }
}
