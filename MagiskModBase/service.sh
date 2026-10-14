#!/system/bin/sh
MODDIR=${0%/*} 

# Wait for boot to finish
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

# Apply gesture settings based on user preference
BACK_GEST=$(getprop persist.sys.pxl.back_gest)
if [ "$BACK_GEST" = "true" ]; then
    settings put secure back_gesture_inset_scale_left 0.0
    settings put secure back_gesture_inset_scale_right 0.0
else
    settings delete secure back_gesture_inset_scale_left
    settings delete secure back_gesture_inset_scale_right
fi

exit 0
