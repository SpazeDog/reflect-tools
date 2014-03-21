/*
* This file is part of the ReflectTools Project: https://github.com/spazedog/reflect-tools
*
* Copyright (c) 2014 Daniel Bergløv
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

import java.lang.reflect.Member;
import java.util.HashMap;

import android.os.IBinder;

import com.spazedog.lib.reflecttools.utils.ReflectCallable;
import com.spazedog.lib.reflecttools.utils.ReflectException;
import com.spazedog.lib.reflecttools.utils.ReflectMember;
import com.spazedog.lib.reflecttools.utils.ReflectMember.Match;
import com.spazedog.lib.reflecttools.utils.ReflectMember.ReflectParameters;
import com.spazedog.lib.reflecttools.utils.ReflectMember.ReflectParameters.ReflectArgumentTypes;
import com.spazedog.lib.reflecttools.utils.ReflectMember.ReflectParameters.ReflectParameterTypes;

public class ReflectClass implements ReflectCallable<Class<?>> {
	protected final static HashMap<String, Class<?>> oClassCache = new HashMap<String, Class<?>>();
	protected static final ClassLoader oClassLoader = ClassLoader.getSystemClassLoader();
	
	protected final Class<?> mClass;
	protected Object mReceiver;
	
	protected OnReceiverListener mOnReceiverListener;
	protected OnErrorListener mOnErrorListener;
	
	final OnReflectEvent mHandler = new OnReflectEvent();
	
	public static class OnReflectEvent {
		private OnReflectEvent(){}
		
		public static enum Event { RECEIVER, ERROR, HANDLER }
		
		public Object onEvent(Event event, ReflectMember<?> member) {
			ReflectClass thisObject = member.getReflectClass();
			
			switch (event) {
				case RECEIVER:
					if (thisObject.mOnReceiverListener != null) {
						return thisObject.mOnReceiverListener.onReceiver(member);
					}
					break;
					
				case ERROR:
					if (thisObject.mOnErrorListener != null) {
						thisObject.mOnErrorListener.onError(member);
					}
					break;
					
				case HANDLER:
					return member.getReflectClass().mHandler;
			}
			
			return null;
		}
	}
	
	public static interface OnReceiverListener {
		public Object onReceiver(ReflectMember<?> member);
	}
	
	public static interface OnErrorListener {
		public void onError(ReflectMember<?> member);
	}
	
	public static ReflectClass forName(String className) {
		return new ReflectClass(className, null);
	}
	
	public static ReflectClass forName(String className, ClassLoader classLoader) {
		return new ReflectClass(className, classLoader);
	}
	
	public ReflectClass(String className) {
		this(className, null);
	}
	
	public ReflectClass(String className, ClassLoader classLoader) {
		if (!oClassCache.containsKey(className)) {
			Class<?> clazz = null;
			Throwable throwable = null;
			
			for (int i=0; i < 2; i++) {
				try {
					/*
					 * Some times it will fail if you provide a classloader, so on failure, we will try without. 
					 */
					clazz = i > 0 ? Class.forName(className) : Class.forName(className, false, classLoader == null ? oClassLoader : classLoader); break;
					
				} catch (ClassNotFoundException e) { throwable = e; }
			}
			
			if (clazz != null) {
				oClassCache.put(className, clazz);
				
			} else {
				throw new ReflectException("ClassNotFoundException: " + className, throwable);
			}
		}
		
		mClass = oClassCache.get(className);
	}
	
	public ReflectClass(Object clazz) {
		mClass = clazz instanceof Class ? (Class<?>) clazz : 
			clazz instanceof ReflectClass ? ((ReflectClass) clazz).getObject() : clazz.getClass();
			
		mReceiver = clazz instanceof Class ? null : 
			clazz instanceof ReflectClass ? ((ReflectClass) clazz).getReceiver() : clazz;
		
		if (!oClassCache.containsKey( mClass.getName() )) {
			oClassCache.put(mClass.getName(), mClass);
		}
	}

	@Override
	public Class<?> getObject() {
		return mClass;
	}
	
	public Object getReceiver() {
		return mReceiver;
	}
	
	public void setReceiver(Object receiver) {
		mReceiver = receiver;
	}
	
	public ReflectClass getParent() {
		try {
			if (mReceiver != null) {
				Object parent = null;
	
				try {
					parent = mReceiver.getClass().getDeclaredField("this$0").get(mReceiver);
					
				} catch (NoSuchFieldException e) {
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {}
				
				if (parent != null) {
					return new ReflectClass(parent);
				}
				
			} else {
				String clazzName = mClass.getName();
				Integer index = clazzName.lastIndexOf("$");
				
				if (index > 0) {
					return new ReflectClass(clazzName.substring(0, index), mClass.getClassLoader());
				}
			}
		
		} catch (ReflectException e) {}
		
		return null;
	}
	
	public Integer inject(String methodName, Object hook) {
		try {
			ReflectClass xposedBridge = ReflectClass.forName("de.robv.android.xposed.XposedBridge", mClass.getClassLoader());
			ReflectMethod hookMethod = xposedBridge.findMethod("hookMethod", Match.BEST, Member.class, "de.robv.android.xposed.XC_MethodHook");
			Member[] members = methodName != null ? mClass.getDeclaredMethods() : mClass.getDeclaredConstructors();
			Integer count = 0;
			
			for (Member member : members) {
				if (methodName == null || member.getName().equals(methodName)) {
					hookMethod.invoke(member, hook); count++;
				}
			}
			
			return count;
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public Integer inject(Object hook) {
		try {
			return inject(null, hook);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public Object newInstance(Object... args) {
		try {
			return findConstructor(Match.BEST, args.length == 0 ? null : new ReflectArgumentTypes(args)).invoke(args);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public Object newOriginalInstance(Object... args) {
		try {
			return findConstructor(Match.BEST, args.length == 0 ? null : new ReflectArgumentTypes(args)).invokeOriginal(args);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectClass bindInterface(String service) {
		try {
			IBinder binder = (IBinder) ReflectClass.forName("android.os.ServiceManager", mClass.getClassLoader()).findMethod("getService", Match.BEST, String.class).invoke(service);
			
			return bindInterface(binder);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectClass bindInterface(IBinder binder) {
		try {
			if (binder != null) {
				String className = mClass.getName();
				ReflectClass reflectClazz = ReflectClass.forName(className + "$Stub", mClass.getClassLoader());
				ReflectMethod reflectMethod = reflectClazz.findMethod("asInterface", Match.BEST, IBinder.class);
				
				mReceiver = reflectMethod.invoke(binder);
			}
			
			return this;
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectConstructor findConstructor() {
		try {
			return new ReflectConstructor(this, mHandler, Match.BEST, null);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectConstructor findConstructor(Match match, Object... paremeterTypes) {
		try {
			return new ReflectConstructor(this, mHandler, match, paremeterTypes.length > 0 ? new ReflectParameterTypes(paremeterTypes) : null);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectConstructor findConstructor(Match match, ReflectParameters parameters) {
		try {
			return new ReflectConstructor(this, mHandler, match, parameters);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectMethod findMethod(String methodName) {
		try {
			return new ReflectMethod(this, mHandler, methodName, Match.BEST, false, null);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectMethod findMethod(String methodName, Match match, Object... paremeterTypes) {
		try {
			return new ReflectMethod(this, mHandler, methodName, match, false, paremeterTypes.length > 0 ? new ReflectParameterTypes(paremeterTypes) : null);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectMethod findMethod(String methodName, Match match, ReflectParameters parameters) {
		try {
			return new ReflectMethod(this, mHandler, methodName, match, false, parameters);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectMethod findMethodDeep(String methodName) {
		try {
			return new ReflectMethod(this, mHandler, methodName, Match.BEST, true, null);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectMethod findMethodDeep(String methodName, Match match, Object... paremeterTypes) {
		try {
			return new ReflectMethod(this, mHandler, methodName, match, true, paremeterTypes.length > 0 ? new ReflectParameterTypes(paremeterTypes) : null);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectMethod findMethodDeep(String methodName, Match match, ReflectParameters parameters) {
		try {
			return new ReflectMethod(this, mHandler, methodName, match, true, parameters);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectField findField(String fieldName) {
		try {
			return new ReflectField(this, mHandler, fieldName, false);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public ReflectField findFieldDeep(String fieldName) {
		try {
			return new ReflectField(this, mHandler, fieldName, true);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public void setOnReceiverListener(OnReceiverListener listener) {
		mOnReceiverListener = listener;
	}
	
	public void setOnErrorListener(OnErrorListener listener) {
		mOnErrorListener = listener;
	}
}
