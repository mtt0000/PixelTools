package sh.siava.pixelxpert.xposed.utils;

import java.lang.reflect.Method;

public class KSUConfigReader {
    public static boolean getBoolean(String key, boolean defaultValue) {
        String propKey = "";
        if (key.equals("clear_all")) {
            propKey = "persist.sys.pxl.clear_all";
        } else if (key.equals("back_gesture")) {
            propKey = "persist.sys.pxl.back_gest";
        } else if (key.equals("kill_app")) {
            propKey = "persist.sys.pxl.kill_app";
        } else if (key.equals("hide_app")) {
            propKey = "persist.sys.pxl.hide_app";
        } else if (key.equals("screenshot_anywhere")) {
            propKey = "persist.sys.pxl.scr_any";
        } else {
            return defaultValue;
        }

        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getBoolean = systemProperties.getMethod("getBoolean", String.class, boolean.class);
            return (Boolean) getBoolean.invoke(null, propKey, defaultValue);
        } catch (Exception e) {
            // Fallback
        }
        return defaultValue;
    }
}
