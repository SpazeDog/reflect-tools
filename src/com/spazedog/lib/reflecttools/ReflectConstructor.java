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

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.ReflectClass.ReflectClassException;
import com.spazedog.lib.reflecttools.apache.Common;
import com.spazedog.lib.reflecttools.bridge.MethodBridge;
import com.spazedog.lib.reflecttools.bridge.MethodCydia;
import com.spazedog.lib.reflecttools.bridge.MethodXposed;

public class ReflectConstructor extends ReflectMember<ReflectConstructor> {
	
	/**
	 * @hide
	 */
	protected final static HashMap<String, Constructor<?>> oConstructCache = new HashMap<String, Constructor<?>>();
	
	/**
	 * Search in a {@link ReflectClass} for a constructor. This method might also search super and parent classes, depending 
	 * on the {@link Match} value parsed.
	 * 
	 * @param match
	 * 		How deep the constructor should search
	 * 
	 * @param rclass
	 * 		The class to search in
	 * 
	 * @param parameterTypes
	 * 		Parameter types of the constructor that should be found
	 *
	 * @throws ReflectMemberException
	 * 		Thrown if the constructor could not be found
	 */
	public static Constructor<?> findConstructor(Match match, ReflectClass rclass, ReflectParameterTypes parameterTypes) throws ReflectMemberException {
		String className = rclass.getObject().getName();
		String cacheName = className + "(" + (parameterTypes == null ? "" : parameterTypes.toString()) + ")#" + match.name();
		Constructor<?> constructor = oConstructCache.get(cacheName);
		
		if (constructor == null) {
			ReflectClass currentRClass = rclass;
			Throwable throwable = null;
			Class<?>[] parameters = parameterTypes == null ? new Class<?>[0] : parameterTypes.toArray();
			
			/*
			 * Non-static nested classes needs the parent receiver to be instantiated. 
			 * If we have such class, we need to add a hidden parameter to find the correct constructor. 
			 */
			if (!rclass.isStatic() && rclass.isNested()) {
				Class<?>[] newParameters = new Class<?>[ parameters.length + 1 ];
				newParameters[0] = rclass.getParentClass();
				
				for (int i=0, x=1; i < parameters.length; i++, x++) {
					newParameters[x] = parameters[i];
				}
				
				parameters = newParameters;
			}
			
			do {
				Class<?> clazz = currentRClass.getObject();
				
				do {
					try {
						constructor = clazz.getDeclaredConstructor(parameters);
						
					} catch (NoSuchMethodException e) {
						if (throwable == null)
							throwable = e;
						
						if (match != Match.EXACT) {
							Constructor<?>[] constructorList = clazz.getDeclaredConstructors();
							
							for (int x=0; x < constructorList.length; x++) {
								if (Common.ClassUtils.isAssignable(parameters, constructorList[x].getParameterTypes(), true)) {
									if (constructor == null || Common.MemberUtils.compareParameterTypes(constructorList[x].getParameterTypes(), constructor.getParameterTypes(), parameters) < 0) {
										constructor = constructorList[x];
									}
								}
							}
						}
					}
					
				} while (constructor == null && (clazz = clazz.getSuperclass()) != null);
				
			} while (constructor == null && match == Match.DEEP && (currentRClass = currentRClass.getParent()) != null);
			
			if (constructor == null) {
				throw new ReflectMemberException("Could not locate the constructor " + cacheName, throwable);
				
			} else {
				constructor.setAccessible(true);
			}
			
			oConstructCache.put(cacheName, constructor);
		}
		
		return constructor;
	}
	
	/**
	 * @hide
	 */
	protected OnRequestReceiverListener mReceiverListener;
	
	/**
	 * @hide
	 */
	protected ReflectClass mReflectClass;
	
	/**
	 * @hide
	 */
	protected Constructor<?> mConstructor;
	
	/**
	 * @hide
	 */
	protected ReflectConstructor(ReflectClass rclass, Constructor<?> constructor) {
		mReflectClass = rclass;
		mConstructor = constructor;
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
	public Constructor<?> getObject() {
		return mConstructor;
	}
	
	/**
	 * Add a hook to this {@link Constructor}
	 * 
	 * @param callback
	 * 		A callback instance that will be called whenever someone calls one of the {@link Constructor}
	 * 
	 * @throws ReflectMemberException
	 * 		If it was not possible to add the hook due to missing injection systems, such as Xposed Framework and Cydia Substrate
	 */
	public void bridge(MethodBridge callback) throws ReflectMemberException {
		if (ReflectUtils.bridgeInitiated()) {
			try {
				if (ReflectUtils.usesCydia()) {
					MethodCydia.setupBridge(callback, mConstructor);
					
				} else {
					MethodXposed.setupBridge(callback, mConstructor);
				}
				
			} catch (Throwable e) {
				throw new ReflectMemberException("Error while injecting runtime code to the " + "constructor" + " for " + mReflectClass.getObject().getName(), e);
			}
			
		} else {
			throw new ReflectMemberException("Cannot inject runtime code while no bridge has been initiated, attempted on " + "constructor" + " for " + mReflectClass.getObject().getName());
		}
	}
	
	/**
	 * @see #invoke(Result, Object...)
	 */
	public Object invoke(Object... args) throws ReflectMemberException, ReflectClassException {
		return invokeInternal(Result.DATA, args, false);
	}
	
	/**
	 * Invoke this {@link Constructor} 
	 * 
	 * @param result
	 * 		Defines how to handle the result
	 * 
	 * @param args
	 * 		Arguments to be parsed to the {@link Constructor}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if it failed to invoke the {@link Constructor}
	 */
	public Object invoke(Result result, Object... args) throws ReflectMemberException, ReflectClassException {
		return invokeInternal(result, args, false);
	}
	
	/**
	 * @hide
	 */
	protected Object invokeInternal(Result result, Object[] args, boolean original) throws ReflectMemberException, ReflectClassException {
		Object[] params = null;
		
		if (!mReflectClass.isStatic() && mReflectClass.isNested()) {
			Object receiver = mReceiverListener != null ? mReceiverListener.onRequestReceiver(this) : null;
			
			if (receiver == null) {
				receiver = getReceiver();
				
				if (receiver != null && mReflectClass.getObject().isInstance(receiver)) {
					receiver = mReflectClass.getParentReceiver();
				}
				
				if (receiver != null) {
					params = new Object[ args.length + 1 ];
					params[0] = receiver;
					
					for (int i=0, x=1; i < args.length; i++, x++) {
						params[x] = args[i];
					}
					
				} else {
					throw new ReflectMemberException("Cannot instantiate a nested non-static class constructor without an accociated receiver");
				}
			}
			
		} else {
			params = args;
		}
		
		Object data = null;
		
		try {
			data = mConstructor.newInstance(params);
			
		} catch (Throwable e) {
			throw new ReflectMemberException("Unable to instantiate class constructor", e);
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
