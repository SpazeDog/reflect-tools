package de.robv.android.xposed;

/**
 * For Internal Use
 * 
 * @hide
 */
public interface IXposedHookZygoteInit {
	
	/*
	 * XposedBridge includes a main library on devices that installs Xposed. This means that modules can only 
	 * add the API to their build paths, but not actually include it into the compiled APK. If they do, Xposed will crash when including 
	 * the module as it will find to versions of the API. This however is only the case for the hook classes and not the case for interfaces. 
	 * So in order to load Cydia and Xposed via the same class, we need to have the interface packed into this library. Otherwise 
	 * it will crash on devices that does not have Xposed installed as the interface will be missing. But in order to include this 
	 * interface, we cannot have the official API added to the build path as it will then not build the APK, because if will find two 
	 * versions. So we use a custom made API jar file without this interface and include the interface into the library. 
	 */

	public static interface StartupParam {
		
	}
	
	public void initZygote(StartupParam startupParam) throws Throwable;
}
