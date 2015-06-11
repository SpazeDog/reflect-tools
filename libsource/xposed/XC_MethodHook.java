package de.robv.android.xposed;

import java.lang.reflect.Member;

public abstract class XC_MethodHook {
	
	public static abstract class MethodHookParam {
		public Member method;
		public Object thisObject;
		public Object[] args;
		
		public abstract Object getResult();
		public abstract void setResult(Object result);
	}
	
	public static abstract class Unhook {
		
	}
	
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
}
