/*
* This file is part of the ReflectTools Project: https://github.com/spazedog/reflect-tools
*
* Copyright (c) 2015 Daniel Bergløv
*
* ReflectTools is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.

* ReflectTools is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.

* You should have received a copy of the GNU General Public License
* along with ReflectTools. If not, see <http://www.gnu.org/licenses/>
*
*/

package com.spazedog.lib.reflecttools.bridge;

import java.lang.reflect.Method;

import android.content.Context;
import android.os.Build;

import com.spazedog.lib.reflecttools.ReflectClass;
import com.spazedog.lib.reflecttools.ReflectException;
import com.spazedog.lib.reflecttools.ReflectUtils;
import com.spazedog.lib.reflecttools.ReflectUtils.LOG;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeType;

import de.robv.android.xposed.IXposedHookZygoteInit;

public abstract class InitBridge implements IXposedHookZygoteInit {
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	private final static MethodBridge mUtilsBridge = new MethodBridge() {
		@Override
		public void bridgeBegin(BridgeParams params) {
			params.setResult(true);
		}
	};

	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	@Override
	public final void initZygote(StartupParam startupParam) throws Throwable {
		LOG.Info(this, "Initiating Xposed Bridge");
		
		addUtilsBridge(BridgeType.XPOSED, null);
		internalZygoteInit();
	}
	
	/**
	 * Cydia Substrate uses a pure design where a static method is used to instantiate modules. 
	 * Since Java does not support static inheritance, we need this ugly trick in order to get 
	 * Cydia to work.<br /><br />
	 * 
	 * It is VERY important that any class extending this one, contains a static method called 
	 * initialize() which calls this method while parsing the class type. This library will handle everything else, 
	 * but there is no way in Java/Android to track down whatever classes might have extended this, not from a static call. 
	 * 
	 * @param clazz
	 * 		The {@link Class} type of whatever class extends this class and should be instantiated via Cydia
	 */
	public final static void initialize(Class<? extends InitBridge> clazz) throws Throwable {
		LOG.Info(InitBridge.class.getName(), "Initiating Cydia Substrate");

		InitBridge instance = (InitBridge) clazz.newInstance();
		instance.addUtilsBridge(BridgeType.CYDIA, null);
		instance.internalZygoteInit();
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	private final void internalZygoteInit() throws Throwable {
		LOG.Debug(this, "Bridge Status is currently at\n\t\t" + 
				"bridgeInitiated = " + (ReflectUtils.bridgeInitiated() ? "TRUE" : "FALSE") + "\n\t\t" + 
				"usesCydia = " + (ReflectUtils.usesCydia() ? "TRUE" : "FALSE") + "\n\t\t" + 
				"usesXposed = " + (ReflectUtils.usesXposed() ? "TRUE" : "FALSE"));
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			LOG.Info(this, "Adding bridge redirected via ActivityThread");
			
			try {
				ReflectClass.fromName("android.app.ActivityThread")
				.bridge("systemMain", new MethodBridge() {
					@Override
					public void bridgeEnd(BridgeParams params) {
						LOG.Info(this, "Adding bridge for ActivityManagerService");
						
						try {
							ReflectClass.fromName("com.android.server.am.ActivityManagerService")
							.bridge(new MethodBridge() {
								@Override
								public void bridgeEnd(BridgeParams params) {
									Context context = ReflectUtils.Bridge.correntContext();
									
									LOG.Debug(this, "Invoking onSystemInit\n\t\t" + (context != null ? "Parsing System Context" : "Could not locate System Context"));

									onSystemInit(context);
								}
							});
							
						} catch (ReflectException e) {
							LOG.Error(this, "Failed to add bridge", e);
						}
					}
				});
			
			} catch (ReflectException e) {
				LOG.Error(this, "Failed to add bridge", e);
			}
			
		} else {
			LOG.Info(this, "Adding bridge for ActivityManagerService");
			
			try {
				ReflectClass.fromName("com.android.server.am.ActivityManagerService")
				.bridge("main", new MethodBridge() {
					@Override
					public void bridgeEnd(BridgeParams params) {
						Context context = (Context) params.getResult();
						
						LOG.Debug(this, "Invoking onSystemInit\n\t\t" + (context != null ? "Parsing System Context" : "Could not locate System Context"));

						onSystemInit(context);
					}
				});
				
			} catch (ReflectException e) {
				LOG.Error(this, "Failed to add bridge", e);
			}
		}
		
		LOG.Info(this, "Adding bridge for LoadedApk");
		
		ReflectClass.fromName("android.app.ActivityThread")
		.bridge("handleBindApplication", new MethodBridge() {
			private boolean mIsLoaded = false;
			
			@Override
			public void bridgeEnd(BridgeParams params) {
				String processName = ReflectUtils.Bridge.currentProcessName();
				
				if (!"android".equals(processName)) {
					Context context = ReflectUtils.Bridge.correntContext();
					String packageName = ReflectUtils.Bridge.currentPackageName();
					String thisPackage = InitBridge.this.getClass().getName();
					
					LOG.Debug(this, "Invoking onPackageInit\n\t\t" + 
							(context != null ? "Parsing Application Context" : "Could not locate Application Context") + "\n\t\t" + 
							" Package Name: " + packageName + "\n\t\t" + 
							"Process Name: " + processName + "\n\t\t" + 
							(mIsLoaded ? "Skipping onProcessInit" : "Invoking onProcessInit"));
					
					/*
					 * TODO: 
					 * 			There must be a better way to determine the real package name of the module
					 */
					if (context != null && thisPackage != null && thisPackage.startsWith(packageName + ".")) {
						addUtilsBridge(params.bridgeType, context.getClassLoader());
					}
					
					if (!mIsLoaded) {
						mIsLoaded = true;
						onProcessInit(ReflectUtils.Bridge.correntContext(), packageName, processName);
					}
					
					onPackageInit(ReflectUtils.Bridge.correntContext(), packageName, processName);
				}	
			}
		});
		
		LOG.Debug(this, "Invoking onZygoteInit");
		
		onZygoteInit();
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	private final void addUtilsBridge(BridgeType type, ClassLoader loader) {
		try {
			Class<?> utilsClass = null;
			Method[] methods = new Method[2];
			
			if (loader != null) {
				utilsClass = Class.forName("com.spazedog.lib.reflecttools.ReflectUtils", false, loader);
				
			} else {
				utilsClass = ReflectUtils.class;
			}

			methods[0] = utilsClass.getDeclaredMethod("bridgeInitiated");

			if (!(Boolean) methods[0].invoke(null)) {
				LOG.Debug(this, "Changing Bridge status values");

				switch (type) {
					case CYDIA:
						methods[1] = utilsClass.getDeclaredMethod("usesCydia");

						for (Method method : methods) {
                            new MethodCydia(mUtilsBridge, method);
						}

						break;

					case XPOSED:
						methods[1] = utilsClass.getDeclaredMethod("usesXposed");

						for (Method method : methods) {
                            new MethodXposed(mUtilsBridge, method);
						}
				}
			}
			
		} catch (Throwable e) {
			LOG.Error(this, "Failed to change Bridge status values", e);
		}
	}
	
	/**
	 * This is invoked the moment a Bridge System, like Xposed or Cydia, loads the module. 
	 * Note that this is executed as Root and before Android has been started. You should avoid adding to many 
	 * module hooks from here.
	 */
	public void onZygoteInit() {}
	
	/**
	 * This is invoked once the system thread has been created and started along with things like system context and such. 
	 * This is executed before all system services is being started. It is a good place to add system hooks and 
	 * register custom system services and such. 
	 * 
	 * @param systemContext
	 * 		The main system {@link Context}
	 */
	public void onSystemInit(Context systemContext) {}
	
	/**
	 * This is invoked when an application is starting and creates it's process. 
	 * Any other applications that attaches themselves to this process will not call this. 
	 * This is a good place to add hooks for things shared within the application process. 
	 * 
	 * @param context
	 * 		The Application Context
	 * 
	 * @param packageName
	 * 		Name of the package
	 * 
	 * @param processName
	 * 		Name of the process
	 */
	public void onProcessInit(Context context, String packageName, String processName) {}
	
	/**
	 * This is called whenever a new application is starting. Unlike {@link #onProcessInit(Context, String, String)} 
	 * this is called on all applications, even those attaching themselves to already created processes. Avoid using this 
	 * where a shared hook can be used. Only use this to target app specific parts, like a specific {@link Activity} and such that is not used 
	 * within other applications in the shared process.
	 * 
	 * @param context
	 * 		The Application Context
	 * 
	 * @param packageName
	 * 		Name of the package
	 * 
	 * @param processName
	 * 		Name of the process
	 */
	public void onPackageInit(Context context, String packageName, String processName) {}
}
