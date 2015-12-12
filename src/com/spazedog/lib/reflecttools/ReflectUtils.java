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

package com.spazedog.lib.reflecttools;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.spazedog.lib.reflecttools.ReflectMember.Result;

public class ReflectUtils {

	/**
	 * Check whether or not the currently active injection system is Cydia Substrate
     *
     * Use Boolean instead of boolean in cases where simple methods are optimized
	 */
	public static Boolean usesCydia() {
		return false;
	}
	
	/**
	 * Check whether or not the currently active injection system is Xposed Framework
     *
     * Use Boolean instead of boolean in cases where simple methods are optimized
	 */
	public static Boolean usesXposed() {
		return false;
	}
	
	/**
	 * Check whether or not any type of injection system is currently active for the application that is including this library. 
	 * This does not only check if an injection system is available, but if this module is enabled
	 */
	public static boolean bridgeInitiated() {
		return false;
	}
	
	public static class LOG {
		public static final boolean DEBUG_LOGGING = false;
		
		public static final int DEBUG = Log.DEBUG;
		public static final int INFO = Log.INFO;
		public static final int ERROR = Log.ERROR;
		
		public static void Debug(Object caller, String msg, Throwable tr) {
			Print(DEBUG, caller.getClass().getName(), msg, tr);
		}
		
		public static void Debug(Object caller, String msg) {
			Print(DEBUG, caller.getClass().getName(), msg, null);
		}
		
		public static void Debug(String tag, String msg, Throwable tr) {
			Print(DEBUG, tag, msg, tr);
		}
		
		public static void Debug(String tag, String msg) {
			Print(DEBUG, tag, msg, null);
		}

		public static void Info(Object caller, String msg, Throwable tr) {
			Print(INFO, caller.getClass().getName(), msg, tr);
		}
		
		public static void Info(Object caller, String msg) {
			Print(INFO, caller.getClass().getName(), msg, null);
		}
		
		public static void Info(String tag, String msg, Throwable tr) {
			Print(INFO, tag, msg, tr);
		}
		
		public static void Info(String tag, String msg) {
			Print(INFO, tag, msg, null);
		}

		public static void Error(Object caller, String msg, Throwable tr) {
			Print(ERROR, caller.getClass().getName(), msg, tr);
		}
		
		public static void Error(Object caller, String msg) {
			Print(ERROR, caller.getClass().getName(), msg, null);
		}
		
		public static void Error(String tag, String msg, Throwable tr) {
			Print(ERROR, tag, msg, tr);
		}
		
		public static void Error(String tag, String msg) {
			Print(ERROR, tag, msg, null);
		}
		
		public static void Trace(int level, Object caller, String msg) {
			Trace(level, caller.getClass().getName(), msg);
		}
		
		public static void Trace(int level, String tag, String msg) {
			msg += "\n";
			
			for (StackTraceElement stack : Thread.currentThread().getStackTrace()) {
				msg += "\t\t";
				msg += stack.toString();
				msg += "\n";
			}
			
			Print(level, tag, msg.toString(), null);
		}

		public static void Print(int level, String tag, String msg, Throwable tr) {
			switch (level) {
				case DEBUG: 
					if (DEBUG_LOGGING) {
						Log.d(tag, msg, tr);
					}
					
					break;
					
				case INFO: Log.i(tag, msg, tr); break;
				case ERROR: Log.e(tag, msg, tr); break;
				default: Log.v(tag, msg, tr);
			}
		}
	}
	
	/**
	 * Useful helper tools for when working with module hooks. Although these should work from within a normal Application execution as well. 
	 */
	public static class Bridge {
		/**
		 * Returns the name of the application currently being worked within
		 */
		public static String currentPackageName() {
			try {
				String name = (String) ReflectClass.fromName("android.app.ActivityThread").invokeMethod("currentPackageName");
				
				if (name == null) {
					name = "android";
				}
				
				return name;
				
			} catch (ReflectException e) {
				return null;
			}
		}
		
		/**
		 * Returns the name of the process currently being worked within
		 */
		public static String currentProcessName() {
			try {
				String name = (String) ReflectClass.fromName("android.app.ActivityThread").invokeMethod("currentProcessName");
				
				if (name == null) {
					name = "android";
				}
				
				return name;
				
			} catch (ReflectException e) {
				return currentPackageName();
			}
		}
		
		/**
		 * Returns the {@link Context} associated with the current process. If currently within the <code>android</code> process, the System {@link Context} 
		 * is returned, otherwise the corresponding Application {@link Context}. This makes it possible to get a proper useful {@link Context} from anywhere. 
		 */
		public static Context correntContext() {
			try {
				if (!"android".equals(currentProcessName())) {
					Application applc = (Application) ReflectClass.fromName("android.app.ActivityThread").invokeMethod("currentApplication");
					
					if (applc != null) {
						return applc.getApplicationContext();
					}
					
				} else {
					ReflectClass activityThread = (ReflectClass) ReflectClass.fromName("android.app.ActivityThread").findMethod("currentActivityThread").invoke(Result.INSTANCE);
					
					if (activityThread != null) {
						return (Context) activityThread.invokeMethod("getSystemContext");
					}
				}
				
			} catch (ReflectException e) {}
			
			return null;
		}
	}
}
