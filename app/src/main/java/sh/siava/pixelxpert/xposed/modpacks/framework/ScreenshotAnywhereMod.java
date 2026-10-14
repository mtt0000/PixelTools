package sh.siava.pixelxpert.xposed.modpacks.framework;

import android.content.Context;
import android.view.WindowManager;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.FrameworkModPack;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

@FrameworkModPack
public class ScreenshotAnywhereMod extends XposedModPack {
    private boolean enableScreenshotAnywhere = false;

    public ScreenshotAnywhereMod(Context context) {
        super(context);
    }

    @Override
    public void onPreferenceUpdated(String... key) {
        enableScreenshotAnywhere = KSUConfigReader.getBoolean("screenshot_anywhere", false) ||
            sh.siava.pixelxpert.xposed.XPrefs.Xprefs.getBoolean("ScreenshotChordInsecure", false);
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
        onPreferenceUpdated();

        try {
            ReflectedClass windowStateClass = ReflectedClass.of("com.android.server.wm.WindowState");
            windowStateClass.after("isSecureLocked").run(param -> {
                if (enableScreenshotAnywhere) {
                    param.setResult(false);
                }
            });
        } catch (Throwable t) {
            Logger.log("ScreenshotAnywhereMod error hooking WindowState.isSecureLocked: " + t.getMessage());
        }
    }
}
