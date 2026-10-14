package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static de.robv.android.xposed.XposedHelpers.callMethod;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XPLauncher;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

@LauncherModPack
public class HideAppMod extends XposedModPack {
    private boolean enableHideApp = true;
    private android.content.SharedPreferences prefs;

    public HideAppMod(Context context) {
        super(context);
        prefs = mContext.getSharedPreferences("hidden_apps_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public void onPreferenceUpdated(String... Key) {
        enableHideApp = KSUConfigReader.getBoolean("hide_app", true);
    }

    private List<String> getHiddenApps() {
        String hiddenStr = prefs.getString("hidden_apps", "");
        if (hiddenStr == null || hiddenStr.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(hiddenStr.split(",")));
    }

    private void addHiddenApp(String pkg) {
        List<String> hidden = getHiddenApps();
        if (!hidden.contains(pkg)) {
            hidden.add(pkg);
            prefs.edit().putString("hidden_apps", String.join(",", hidden)).apply();
        }
    }

    private void removeHiddenApp(String pkg) {
        List<String> hidden = getHiddenApps();
        if (hidden.contains(pkg)) {
            hidden.remove(pkg);
            prefs.edit().putString("hidden_apps", String.join(",", hidden)).apply();
        }
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
        enableHideApp = KSUConfigReader.getBoolean("hide_app", true);

        ReflectedClass TaskViewClass = ReflectedClass.of("com.android.quickstep.views.TaskView");
        ReflectedClass RecentsViewClass = ReflectedClass.of("com.android.quickstep.views.RecentsView");
        ReflectedClass OverviewActionsViewClass = ReflectedClass.of("com.android.quickstep.views.OverviewActionsView");

        // 1. Hide Button on each TaskView
        TaskViewClass.after("onFinishInflate").run(param -> {
            if (!enableHideApp) return;
            try {
                FrameLayout taskView = (FrameLayout) param.thisObject;

                ImageButton hideButton = new ImageButton(mContext, null, android.R.attr.borderlessButtonStyle);

                int hideIconId = 0;
                if (XPLauncher.moduleResources != null) {
                    hideIconId = XPLauncher.moduleResources.getIdentifier("ic_eye_off", "drawable", sh.siava.pixelxpert.BuildConfig.APPLICATION_ID);
                }

                if (hideIconId != 0 && XPLauncher.moduleResources != null) {
                    hideButton.setImageDrawable(XPLauncher.moduleResources.getDrawable(hideIconId, null));
                } else {
                    hideButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Fallback
                }

                hideButton.setColorFilter(Color.WHITE);

                // Add proper padding and size
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
                hideButton.setPadding(padding, padding, padding, padding);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x80000000);
                bg.setShape(GradientDrawable.OVAL);
                hideButton.setBackground(bg);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, mContext.getResources().getDisplayMetrics())
                );
                // Place top left so it doesn't conflict with kill app on top right
                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());
                lp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, mContext.getResources().getDisplayMetrics()); // Positioned to the left of the close button
                hideButton.setLayoutParams(lp);

                // Elevate above task view to ensure clicks are caught
                hideButton.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics()));
                hideButton.setClickable(true);
                hideButton.setFocusable(true);

                hideButton.setOnClickListener(v -> {
                    try {
                        Object task = callMethod(taskView, "getTask");
                        if (task != null) {
                            Object taskKey = task.getClass().getField("key").get(task);
                            Object componentName = callMethod(taskKey, "getComponent");
                            String packageName = (String) callMethod(componentName, "getPackageName");
                            addHiddenApp(packageName);

                            // Remove task from RecentsView visually
                            Object recentsView = callMethod(taskView, "getRecentsView");
                            if (recentsView != null) {
                                callMethod(recentsView, "removeView", taskView);
                            }
                        }
                    } catch (Throwable t) {
                        Logger.log("HideAppMod error hiding app: " + t.getMessage());
                    }
                });


                hideButton.setOnTouchListener((v, event) -> {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return true;
                });

                taskView.addView(hideButton);
            } catch (Throwable t) {
                Logger.log("HideAppMod error in TaskView: " + t.getMessage());
            }
        });

        // 2. Hide tasks in RecentsView that are in the hidden list
        TaskViewClass.after("bind").run(param -> {
            if (!enableHideApp) return;
            try {
                Object task = param.args[0];
                if (task != null) {
                    Object taskKey = task.getClass().getField("key").get(task);
                    Object componentName = callMethod(taskKey, "getComponent");
                    String packageName = (String) callMethod(componentName, "getPackageName");

                    FrameLayout taskView = (FrameLayout) param.thisObject;
                    if (getHiddenApps().contains(packageName)) {
                        taskView.setVisibility(GONE);
                        ViewGroup.LayoutParams lp = taskView.getLayoutParams();
                        lp.width = 0;
                        lp.height = 0;
                        taskView.setLayoutParams(lp);
                    } else {
                        taskView.setVisibility(VISIBLE);
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }
        });

        // 3. Hidden Apps Manager Button in OverviewActionsView (as an icon button)
        OverviewActionsViewClass.after("onFinishInflate").run(param -> {
            if (!enableHideApp) return;
            try {
                FrameLayout parent = (FrameLayout) param.thisObject;

                ViewGroup actionButtonsView = null;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View child = parent.getChildAt(i);
                    if (child instanceof android.widget.LinearLayout) {
                        actionButtonsView = (ViewGroup) child;
                        break;
                    }
                }

                if (actionButtonsView == null) {
                    actionButtonsView = parent;
                }

                ImageButton manageHiddenBtn = new ImageButton(mContext, null, android.R.attr.borderlessButtonStyle);

                int historyIconId = 0;
                if (XPLauncher.moduleResources != null) {
                    historyIconId = XPLauncher.moduleResources.getIdentifier("ic_history", "drawable", sh.siava.pixelxpert.BuildConfig.APPLICATION_ID);
                }

                if (historyIconId != 0 && XPLauncher.moduleResources != null) {
                    manageHiddenBtn.setImageDrawable(XPLauncher.moduleResources.getDrawable(historyIconId, null));
                } else {
                    manageHiddenBtn.setImageResource(android.R.drawable.ic_menu_info_details);
                }

                manageHiddenBtn.setColorFilter(Color.WHITE);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x80000000);
                bg.setShape(GradientDrawable.OVAL);
                manageHiddenBtn.setBackground(bg);

                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
                manageHiddenBtn.setPadding(padding, padding, padding, padding);

                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics())
                );
                lp.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
                manageHiddenBtn.setLayoutParams(lp);

                manageHiddenBtn.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics()));
                manageHiddenBtn.setClickable(true);
                manageHiddenBtn.setFocusable(true);

                manageHiddenBtn.setOnClickListener(v -> {
                    showHiddenAppsDialog(parent.getContext());
                });

                actionButtonsView.addView(manageHiddenBtn, 0);
            } catch (Throwable t) {
                Logger.log("HideAppMod error in OverviewActionsView: " + t.getMessage());
            }
        });
    }

    private void showHiddenAppsDialog(Context context) {
        List<String> hiddenApps = getHiddenApps();
        if (hiddenApps.isEmpty()) {
            new AlertDialog.Builder(context)
                .setTitle("App Nascoste")
                .setMessage("Nessuna app nascosta.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, hiddenApps);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("App Nascoste (Tocca per ripristinare)")
            .setAdapter(adapter, null)
            .setPositiveButton("Chiudi", null)
            .create();

        dialog.getListView().setOnItemClickListener((parent, view, position, id) -> {
            String pkg = hiddenApps.get(position);
            removeHiddenApp(pkg);
            hiddenApps.remove(position);
            adapter.notifyDataSetChanged();
            if (hiddenApps.isEmpty()) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
