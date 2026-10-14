package sh.siava.pixelxpert.xposed;

import static android.content.Context.CONTEXT_IGNORE_SECURITY;
import static sh.siava.pixelxpert.BuildConfig.APPLICATION_ID;
import static sh.siava.pixelxpert.xposed.XPrefs.Xprefs;
import static sh.siava.pixelxpert.xposed.utils.BootLoopProtector.isBootLooped;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import sh.siava.pixelxpert.BuildConfig;
import sh.siava.pixelxpert.Constants;
import sh.siava.pixelxpert.R;
import sh.siava.pixelxpert.xposed.utils.SystemUtils;
import sh.siava.pixelxpert.xposed.utils.reflection.ReflectedClass;
import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

public class XPLauncher extends XposedModule {
	public static String processName = "";
	public static boolean isSystemServer = false;

	public static ArrayList<XposedModPack> runningMods = new ArrayList<>();
	public Context mContext = null;
	@SuppressLint("StaticFieldLeak")
	static XPLauncher instance;

	private static boolean TELECOM_SERVER_LOADED = false;
	public static Resources moduleResources;

	public XPLauncher()
	{
		instance = this;
		Logger.setXposedInterface(this);
	}

	@Override
	public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
		super.onModuleLoaded(param);

		processName = param.getProcessName();
		isSystemServer = param.isSystemServer();
	}

	@Override
	public void onSystemServerStarting(@NonNull XposedModuleInterface.SystemServerStartingParam SSSP)
	{
		ReflectedClass.setFrameworkClassloader(SSSP.getClassLoader());
	}

	@Override
	public void onPackageReady(@NonNull PackageReadyParam PRParam){
		ReflectedClass.setDefaultXposedInterface(this);

		if (isSystemServer && !PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE)) {
			ReflectedClass PhoneWindowManagerClass = ReflectedClass.of("com.android.server.policy.PhoneWindowManager");

			PhoneWindowManagerClass
					.before("init")
					.run(instance,param -> {
						try {
							if (mContext == null) {
								mContext = (Context) param.args[0];

								moduleResources = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
										.getResources();

								XPrefs.init(mContext);

								CompletableFuture.runAsync(() -> waitForXprefsLoad(PRParam));
							}
						} catch (Throwable t) {
							Logger.log(t);
						}
					});
		}

		if(!isSystemServer || PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE)) {
			ReflectedClass.of(Instrumentation.class)
					.after("newApplication")
					.run(this, param -> {
				try {
					if (mContext == null || (PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE) && !TELECOM_SERVER_LOADED)) {
						if (PRParam.getPackageName().equals(Constants.TELECOM_SERVER_PACKAGE))
							TELECOM_SERVER_LOADED = true;

						mContext = (Context) param.args[param.args.length - 1];

						moduleResources = mContext.createPackageContext(APPLICATION_ID, CONTEXT_IGNORE_SECURITY)
								                  .getResources();

						XPrefs.init(mContext);

						waitForXprefsLoad(PRParam);
					}
				} catch (Throwable t) {
					Logger.log(t);
				}
			});
		}
	}

	private void waitForXprefsLoad(PackageReadyParam PRParam) {
		while (true) {
			try {
				Xprefs.getBoolean("LoadTestBooleanValue", false);
				break;
			} catch (Throwable ignored) {
				SystemUtils.threadSleep(1000);
			}
		}

		Logger.log(String.format("Loading PixelXpert version: %s on %s", BuildConfig.VERSION_NAME, PRParam.getPackageName()));
		try {
			Logger.log("PixelXpert Records: " + Xprefs.getAll().size());
		} catch (Throwable ignored) {
		}

		onXPrefsReady(PRParam);
	}

	private void onXPrefsReady(PackageReadyParam PRParam) {
		if (isBootLooped(PRParam.getPackageName())) {
			Logger.log(String.format("PixelXpert: Possible bootloop in %s. Will not load for now", PRParam.getPackageName()));
			return;
		}

		new SystemUtils(mContext);
		XPrefs.setPackagePrefs(PRParam.getPackageName());

		loadModPacks(PRParam);

		XPrefs.onContentProviderLoaded();
	}

	private void loadModPacks(PackageReadyParam PRParam) {
		ReflectedClass.setDefaultClassloader(PRParam.getClassLoader());

		ModPacks.getModPacks()
				.forEach(modPackData -> {
					String partOfProcessName = modPackData.targetsMainProcess ? "" : modPackData.childProcessName;

					if((modPackData.targetPackage.equals(PRParam.getPackageName()) || modPackData.targetPackage.isEmpty() /*common mod packs*/ || (modPackData.targetPackage.equals(Constants.SYSTEM_FRAMEWORK_PACKAGE) && isSystemServer))
							   && processName.contains(partOfProcessName))
					{
						//noinspection unchecked
						loadModPack((Class<? extends XposedModPack>) modPackData.clazz, PRParam);
					}
				});
	}

	private void loadModPack(Class<? extends XposedModPack> thisClass, PackageReadyParam PRParam) {
		try {
			XposedModPack instance = thisClass.getConstructor(Context.class).newInstance(mContext);
			try {
				instance.onPreferenceUpdated();
			} catch (Throwable ignored) {
			}

			instance.onPackageLoaded(PRParam);
			runningMods.add(instance);
		} catch (Throwable T) {
			Logger.log("Start Error Dump - Occurred in " + thisClass.getName());
			Logger.log(T);
		}
	}
}
