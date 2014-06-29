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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.apache.Common;
import com.spazedog.lib.reflecttools.utils.ReflectException;
import com.spazedog.lib.reflecttools.utils.ReflectMember;

public class ReflectMethod extends ReflectMember<ReflectMethod> {
	private final static HashMap<String, Method> oMethodCache = new HashMap<String, Method>();
	private final static HashMap<Method, ArrayList<Object>> oMethodUnhookCache = new HashMap<Method, ArrayList<Object>>();
	
	private Method mMethod;
	private ReflectClass mReflectClass;
	
	public ReflectMethod(ReflectClass reflectClass, Method method) {
		mMethod = method;
		mReflectClass = reflectClass;
	}
	
	public ReflectMethod(ReflectClass reflectClass, String methodName, Match match, Boolean deepSearch, ReflectParameters parameterTypes) {
		String className = reflectClass.getObject().getName();
		String cacheName = className + "." + methodName + "[" + (parameterTypes == null ? "" : parameterTypes.toString()) + "]" + (match == Match.BEST ? "#B" : "#E");
				
		if (!oMethodCache.containsKey(cacheName)) {
			ReflectClass currentClass = reflectClass;
			Method method = null;
			Throwable throwable = null;
			Class<?>[] parameters = parameterTypes == null ? new Class<?>[0] : parameterTypes.get();
			
			do {
				Class<?> clazz = currentClass.getObject();
				
				do {
					try {
						method = clazz.getDeclaredMethod(methodName, parameters);
						
					} catch (NoSuchMethodException e) {
						if (throwable == null)
							throwable = e;
						
						if (match != Match.EXACT) {
							Method[] methods = clazz.getDeclaredMethods();
							
							for (int x=0; x < methods.length; x++) {
								if (methods[x].getName().equals(methodName) && Common.ClassUtils.isAssignable(parameters, methods[x].getParameterTypes(), true)) {
									if (method == null || Common.MemberUtils.compareParameterTypes(methods[x].getParameterTypes(), method.getParameterTypes(), parameters) < 0) {
										method = methods[x];
									}
								}
							}
						}
					}
					
				} while (method == null && (clazz = clazz.getSuperclass()) != null);
				
			} while (method == null && deepSearch && (currentClass = currentClass.getParent()) != null);
			
			if (method != null) {
				method.setAccessible(true);

				oMethodCache.put(cacheName, method);
				
			} else {
				throw new ReflectException("NoSuchMethodException: " + cacheName, throwable);
			}
		}
		
		mMethod = oMethodCache.get(cacheName);
		mReflectClass = reflectClass;
	}
	
	public Object invoke(Object... args) {
		Object receiver = mReflectClass.getReceiver();
		Boolean isStatic = Modifier.isStatic(mMethod.getModifiers());
		
		if (!isStatic && receiver == null) {
			receiver = mReflectClass.triggerReceiverEvent(this);
			
			if (receiver == null) {
				receiver = mReflectClass.getReceiver();
			}
		}
		
		try {
			return mMethod.invoke(isStatic ? null : resolveReceiverInternal(receiver), args);
			
		} catch (Throwable e) {
			mReflectClass.triggerErrorEvent(this);
			
			throw new ReflectException(e);
		}
	}
	
	public Object invokeOriginal(Object... args) {
		Object receiver = mReflectClass.getReceiver();
		Boolean isStatic = Modifier.isStatic(mMethod.getModifiers());
		
		if (!isStatic && receiver == null) {
			receiver = mReflectClass.triggerReceiverEvent(this);
			
			if (receiver == null) {
				receiver = mReflectClass.getReceiver();
			}
		}
		
		try {
			ReflectClass xposedBridge = ReflectClass.forName("de.robv.android.xposed.XposedBridge", mMethod.getDeclaringClass().getClassLoader());
			ReflectMethod invokeOriginalMethod = xposedBridge.findMethod("invokeOriginalMethod", Match.BEST, Member.class, Object.class, Object[].class);
			
			return invokeOriginalMethod.getObject().invoke(mMethod, isStatic ? null : resolveReceiverInternal(receiver), args);
			
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
			ReflectClass xposedBridge = ReflectClass.forName("de.robv.android.xposed.XposedBridge", mMethod.getDeclaringClass().getClassLoader());
			ReflectMethod hookMethod = xposedBridge.findMethod("hookMethod", Match.BEST, Member.class, "de.robv.android.xposed.XC_MethodHook");
			ArrayList<Object> unhooks = oMethodUnhookCache.get(mMethod);
			
			if (unhooks == null) {
				unhooks = new ArrayList<Object>();
			}
			
			unhooks.add(hookMethod.invoke(mMethod, hook));
			
			oMethodUnhookCache.put(mMethod, unhooks);
			
			mReflectClass.handleHookCache(mMethod, true);
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	public void removeInjection() {
		try {
			ArrayList<Object> unhooks = oMethodUnhookCache.get(mMethod);
			
			if (unhooks != null && unhooks.size() > 0) {
				ReflectClass xposedUnhook = ReflectClass.forName("de.robv.android.xposed.XC_MethodHook$Unhook", mMethod.getDeclaringClass().getClassLoader());
				ReflectMethod unhookMethod = xposedUnhook.findMethod("unhook");
				
				for (Object unhook : unhooks) {
					xposedUnhook.setReceiver(unhook);
					unhookMethod.invoke();
				}
				
				mReflectClass.handleHookCache(mMethod, false);
			}
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}

	@Override
	public Method getObject() {
		return mMethod;
	}

	@Override
	public ReflectClass getReflectClass() {
		return mReflectClass;
	}

	@Override
	public ReflectMethod resolveReceiver() {
		Object receiver = mReflectClass.getReceiver();
		Class<?> clazz = mMethod.getDeclaringClass();
		
		if (receiver != null && !clazz.isInstance(receiver)) {
			Object newReceiver = resolveReceiverInternal(receiver);
			
			if (newReceiver != receiver) {
				return new ReflectMethod(new ReflectClass(newReceiver), mMethod);
			}
		}
		
		return this;
	}
}
