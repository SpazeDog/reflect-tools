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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.apache.Common;
import com.spazedog.lib.reflecttools.utils.ReflectException;
import com.spazedog.lib.reflecttools.utils.ReflectMember;

public class ReflectConstructor extends ReflectMember<ReflectConstructor> {
	private final static HashMap<String, Constructor<?>> oConstructorCache = new HashMap<String, Constructor<?>>();
	private final static HashMap<Constructor<?>, ArrayList<Object>> oConstructorUnhookCache = new HashMap<Constructor<?>, ArrayList<Object>>();
	
	private Constructor<?> mConstructor;
	private ReflectClass mReflectClass;
	
	public ReflectConstructor(ReflectClass reflectClass, Constructor<?> constructor) {
		mConstructor = constructor;
		mReflectClass = reflectClass;
	}
	
	public ReflectConstructor(ReflectClass reflectClass, Match match, ReflectParameters parameterTypes) {
		String className = reflectClass.getObject().getName();
		String cacheName = className + "[" + (parameterTypes == null ? "" : parameterTypes.toString()) + "]" + (match == Match.BEST ? "#B" : "#E");
		
		if (!oConstructorCache.containsKey(cacheName)) {
			Throwable throwable = null;
			Constructor<?> constructor = null;
			Class<?>[] parameters = parameterTypes == null ? new Class<?>[0] : parameterTypes.get();
			Class<?> clazz = reflectClass.getObject();
			
			try {
				constructor = clazz.getDeclaredConstructor(parameters);
				
			} catch (NoSuchMethodException e) {
				if (throwable == null)
					throwable = e;
				
				if (match != Match.EXACT) {
					Constructor<?>[] constructors = clazz.getDeclaredConstructors();
					
					for (int x=0; x < constructors.length; x++) {
						if (Common.ClassUtils.isAssignable(parameters, constructors[x].getParameterTypes(), true)) {
							if (constructor == null || Common.MemberUtils.compareParameterTypes(constructors[x].getParameterTypes(), constructor.getParameterTypes(), parameters) < 0) {
								constructor = constructors[x];
							}
						}
					}
				}
			}
			
			if (constructor != null) {
				constructor.setAccessible(true);

				oConstructorCache.put(cacheName, constructor);
				
			} else {
				throw new ReflectException("NoSuchMethodException: " + cacheName, throwable);
			}
		}
		
		mConstructor = oConstructorCache.get(cacheName);
		mReflectClass = reflectClass;
	}
	
	public Object invoke(Object... args) {
		try {
			return mConstructor.newInstance(args);
			
		} catch (Throwable e) {
			mReflectClass.triggerErrorEvent(this);
			
			throw new ReflectException(e);
		}
	}
	
	public Object invokeOriginal(Object... args) {
		try {
			ReflectClass xposedBridge = ReflectClass.forName("de.robv.android.xposed.XposedBridge", mConstructor.getDeclaringClass().getClassLoader());
			ReflectMethod invokeOriginalMethod = xposedBridge.findMethod("invokeOriginalMethod", Match.BEST, Member.class, Object.class, Object[].class);
			
			return invokeOriginalMethod.getObject().invoke(mConstructor, null, args);
			
		} catch (Throwable e) {
			mReflectClass.triggerErrorEvent(this);
			
			throw new ReflectException(e);
		}
	}
	
	public ReflectClass invokeToInstance(Object... args) {
		try {
			return new ReflectClass(invoke(args));
			
		} catch (Throwable e) {
			throw new ReflectException(e);
		}
	}
	
	public ReflectClass invokeOriginalToInstance(Object... args) {
		try {
			return new ReflectClass(invokeOriginal(args));
			
		} catch (Throwable e) {
			throw new ReflectException(e);
		}
	}
	
	public ReflectClass invokeForReceiver(Object... args) {
		try {
			mReflectClass.setReceiver(invoke(args));
			
			return mReflectClass;
			
		} catch (Throwable e) {
			throw new ReflectException(e);
		}
	}
	
	public ReflectClass invokeOriginalForReceiver(Object... args) {
		try {
			mReflectClass.setReceiver(invokeOriginal(args));
			
			return mReflectClass;
			
		} catch (Throwable e) {
			throw new ReflectException(e);
		}
	}
	
	public void inject(Object hook) {
		try {
			ReflectClass xposedBridge = ReflectClass.forName("de.robv.android.xposed.XposedBridge", mConstructor.getDeclaringClass().getClassLoader());
			ReflectMethod hookMethod = xposedBridge.findMethod("hookMethod", Match.BEST, Member.class, "de.robv.android.xposed.XC_MethodHook");
			ArrayList<Object> unhooks = oConstructorUnhookCache.get(mConstructor);
			
			if (unhooks == null) {
				unhooks = new ArrayList<Object>();
			}
			
			unhooks.add(hookMethod.invoke(mConstructor, hook));
			
			oConstructorUnhookCache.put(mConstructor, unhooks);
			
			mReflectClass.handleHookCache(mConstructor, true);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public void removeInjection() {
		try {
			ArrayList<Object> unhooks = oConstructorUnhookCache.get(mConstructor);
			
			if (unhooks != null && unhooks.size() > 0) {
				ReflectClass xposedUnhook = ReflectClass.forName("de.robv.android.xposed.XC_MethodHook$Unhook", mConstructor.getDeclaringClass().getClassLoader());
				ReflectMethod unhookMethod = xposedUnhook.findMethod("unhook");
				
				for (Object unhook : unhooks) {
					xposedUnhook.setReceiver(unhook);
					unhookMethod.invoke();
				}
				
				mReflectClass.handleHookCache(mConstructor, false);
			}
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}

	@Override
	public Constructor<?> getObject() {
		return mConstructor;
	}

	@Override
	public ReflectClass getReflectClass() {
		return mReflectClass;
	}

	@Override
	public ReflectConstructor resolveReceiver() {
		Object receiver = mReflectClass.getReceiver();
		Class<?> clazz = mConstructor.getDeclaringClass();
		
		if (receiver != null && !clazz.isInstance(receiver)) {
			Object newReceiver = resolveReceiverInternal(receiver);
			
			if (newReceiver != receiver) {
				return new ReflectConstructor(new ReflectClass(newReceiver), mConstructor);
			}
		}
		
		return this;
	}
}
