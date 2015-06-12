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

import java.lang.reflect.Method;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.apache.Common;
import com.spazedog.lib.reflecttools.bridge.MethodBridge;

public class ReflectMethod extends ReflectMember<ReflectMethod> {
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected final static HashMap<String, Method> oMethodCache = new HashMap<String, Method>();
	
	/**
	 * Search in a {@link ReflectClass} for a method. This method might also search super and parent classes, depending 
	 * on the {@link Match} value parsed. 
	 * 
	 * @param methodName
	 * 		The name of the method
	 * 
	 * @param match
	 * 		How deep the method should search
	 * 
	 * @param rclass
	 * 		The class to search in
	 * 
	 * @param parameterTypes
	 * 		Parameter types of the method that should be found
	 *
	 * @throws ReflectMemberException
	 * 		Thrown if the method could not be found
	 */
	public static Method findMethod(String methodName, Match match, ReflectClass rclass, ReflectParameterTypes parameterTypes) throws ReflectMemberException {
		String className = rclass.getObject().getName();
		String cacheName = className + "." + methodName + "(" + (parameterTypes == null ? "" : parameterTypes.toString()) + ")#" + match.name();
		Method method = oMethodCache.get(cacheName);
		
		if (method == null) {
			ReflectClass currentRClass = rclass;
			Throwable throwable = null;
			Class<?>[] parameters = parameterTypes == null ? new Class<?>[0] : parameterTypes.toArray();
			
			do {
				Class<?> clazz = currentRClass.getObject();
				
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
				
			} while (method == null && match == Match.DEEP && (currentRClass = currentRClass.getParent()) != null);
			
			if (method == null) {
				throw new ReflectMemberException("Could not locate the method " + cacheName, throwable);
				
			} else {
				method.setAccessible(true);
			}
			
			oMethodCache.put(cacheName, method);
		}
		
		return method;
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected OnRequestReceiverListener mReceiverListener;
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected ReflectClass mReflectClass;
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected Method mMethod;
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected ReflectMethod(ReflectClass rclass, Method method) {
		mReflectClass = rclass;
		mMethod = method;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setOnRequestReceiverListener(OnRequestReceiverListener listener) {
		mReceiverListener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReflectClass getReflectClass() {
		return mReflectClass;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Method getObject() {
		return mMethod;
	}
	
	/**
	 * Add a hook to this {@link Method}
	 * 
	 * @param callback
	 * 		A callback instance that will be called whenever someone calls one of the {@link Method}
	 * 
	 * @throws ReflectMemberException
	 * 		If it was not possible to add the hook due to missing injection systems, such as Xposed Framework and Cydia Substrate
	 */
	public void bridge(MethodBridge callback) throws ReflectMemberException {
		if (ReflectUtils.bridgeInitiated()) {
			callback.attachBridge(mMethod);
			
		} else {
			throw new ReflectMemberException("Cannot inject runtime code while no bridge has been initiated, attempted on " + "methods matching the name " + mMethod.getName() + " for " + mReflectClass.getObject().getName());
		}
	}
	
	/**
	 * @see #invoke(Result, Object...)
	 */
	public Object invoke(Object... args) throws ReflectMemberException {
		return invokeInternal(Result.DATA, args, false);
	}
	
	/**
	 * Invoke this {@link Method} 
	 * 
	 * @param result
	 * 		Defines how to handle the result
	 * 
	 * @param args
	 * 		Arguments to be parsed to the {@link Method}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if it failed to invoke the {@link Method}
	 */
	public Object invoke(Result result, Object... args) throws ReflectMemberException {
		return invokeInternal(result, args, false);
	}

	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected Object invokeInternal(Result result, Object[] args, boolean original) throws ReflectMemberException {
		Object receiver = null;
		
		if (!isStatic()) {
			receiver = mReceiverListener != null ? mReceiverListener.onRequestReceiver(this) : null;
			
			if (receiver == null) {
				receiver = getReceiver();
				
				if (receiver == null) {
					throw new ReflectMemberException("Cannot invoke a non-static method without an accociated receiver, Method = " + mReflectClass.getObject().getName() + "#" + mMethod.getName());
				}
			}
		}
		
		Object data = null;
		
		try {
			data = mMethod.invoke(receiver, args);
			
		} catch (Throwable e) {
			throw new ReflectMemberException("Unable to invoke method, Method = " + mReflectClass.getObject().getName() + "#" + mMethod.getName(), e);
		}
		
		switch (result) {
			case INSTANCE: 
				return ReflectClass.fromReceiver(data);
				
			case RECEIVER: 
				mReflectClass.setReceiver(data); 
				
			default:
				return data;
		}
	}
}
