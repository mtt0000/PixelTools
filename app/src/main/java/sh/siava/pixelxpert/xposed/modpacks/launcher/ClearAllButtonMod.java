package sh.siava.pixelxpert.xposed.modpacks.launcher;

import static android.view.View.GONE;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.xposed.XPLauncher;
import sh.siava.pixelxpert.xposed.XposedModPack;
import sh.siava.pixelxpert.xposed.annotations.LauncherModPack;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.KSUConfigReader;

@SuppressWarnings("RedundantThrows")
@LauncherModPack
public class ClearAllButtonMod extends XposedModPack {
	private Object recentView;
	private static boolean RecentClearAllReposition = true;
	private android.widget.ImageButton clearAllButton;

	public ClearAllButtonMod(Context context) {
		super(context);
	}

	@Override
	public void onPreferenceUpdated(String... Key) {
		RecentClearAllReposition = KSUConfigReader.getBoolean("clear_all", true);
	}

	@Override
	public void onPackageLoaded(XposedModuleInterface.PackageReadyParam PRParam) throws Throwable {
		RecentClearAllReposition = KSUConfigReader.getBoolean("clear_all", true);

		ReflectedClass OverviewActionsViewClass = ReflectedClass.of("com.android.quickstep.views.OverviewActionsView");
		ReflectedClass RecentsViewClass = ReflectedClass.of("com.android.quickstep.views.RecentsView");
		Method dismissAllTasksMethod = findMethodBestMatch(RecentsViewClass.getClazz(), "dismissAllTasks", View.class);

		RecentsViewClass
				.afterConstruction()
				.run(param -> recentView = param.thisObject);

		RecentsViewClass
				.after("setVisibility")
				.run(param -> {
					if (clearAllButton == null) return;
					clearAllButton.setVisibility((Integer) param.args[0]);
				});

		OverviewActionsViewClass
				.after("onFinishInflate")
				.run(param -> {
					if (!RecentClearAllReposition) return;

					try {
						FrameLayout parent = (FrameLayout) param.thisObject;

						ViewGroup actionButtonsView = null;
						// Find the action buttons container dynamically (usually a LinearLayout containing Screenshot/Select)
						for (int i = 0; i < parent.getChildCount(); i++) {
							View child = parent.getChildAt(i);
							if (child instanceof android.widget.LinearLayout) {
								actionButtonsView = (ViewGroup) child;
								break;
							}
						}

						if (actionButtonsView == null) {
							// Fallback if not wrapped in LinearLayout
							actionButtonsView = parent;
						}

                        clearAllButton = new android.widget.ImageButton(mContext, null, android.R.attr.borderlessButtonStyle);

                        int clearAllResId = 0;
                        if (XPLauncher.moduleResources != null) {
                            clearAllResId = XPLauncher.moduleResources.getIdentifier("ic_clear_all", "drawable", sh.siava.pixelxpert.BuildConfig.APPLICATION_ID);
                        }

                        if (clearAllResId != 0 && XPLauncher.moduleResources != null) {
                            clearAllButton.setImageDrawable(XPLauncher.moduleResources.getDrawable(clearAllResId, null));
                        } else {
                            clearAllButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                        }

                        clearAllButton.setColorFilter(Color.WHITE);

                        // Set the background as a semi-transparent black pill
                        GradientDrawable background = new GradientDrawable();
                        background.setColor(0x80000000); // Semi-transparent black
                        background.setShape(GradientDrawable.OVAL);
                        clearAllButton.setBackground(background);

                        // Set padding
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, mContext.getResources().getDisplayMetrics());
                        clearAllButton.setPadding(padding, padding, padding, padding);

                        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics()),
                                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, mContext.getResources().getDisplayMetrics())
                        );

                        // Add margin right
                        params.rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());
                        clearAllButton.setLayoutParams(params);

                        clearAllButton.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics()));
                        clearAllButton.setClickable(true);
                        clearAllButton.setFocusable(true);

						clearAllButton.setOnClickListener(v -> {
							if (recentView != null) {
								try {
									dismissAllTasksMethod.invoke(recentView, v);
								} catch (Throwable ignored) {}
							}
						});

						actionButtonsView.addView(clearAllButton, 0);
						clearAllButton.setVisibility(GONE);
					} catch (Throwable ignored) {}
				});
	}
}
