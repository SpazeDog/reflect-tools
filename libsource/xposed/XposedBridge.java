package de.robv.android.xposed;

import java.lang.reflect.Member;
import java.util.Set;

public abstract class XposedBridge {
	public static Set<?> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) { return null; }
	public static Set<?> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) { return null; }
	public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) { return null; }
	public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) { return null; }
}
