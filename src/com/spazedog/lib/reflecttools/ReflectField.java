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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.ReflectClass.OnReflectEvent;
import com.spazedog.lib.reflecttools.ReflectClass.OnReflectEvent.Event;
import com.spazedog.lib.reflecttools.utils.ReflectException;
import com.spazedog.lib.reflecttools.utils.ReflectMember;

public class ReflectField extends ReflectMember<ReflectField> {
	protected final static HashMap<String, Field> oFieldCache = new HashMap<String, Field>();
	
	protected Field mField;
	protected ReflectClass mReflectClass;
	protected OnReflectEvent mEventHandler;
	
	public ReflectField(ReflectClass reflectClass, OnReflectEvent eventHandler, String fieldName, Boolean deepSearch) {
		String className = reflectClass.getObject().getName();
		String cacheName = className + "." + fieldName;
		
		if (!oFieldCache.containsKey(cacheName)) {
			ReflectClass currentClass = reflectClass;
			Field field = null;
			Throwable throwable = null;
			
			do {
				Class<?> clazz = currentClass.getObject();
				
				do {
					try {
						field = clazz.getDeclaredField(fieldName);
						
					} catch (NoSuchFieldException e) {
						if (throwable == null)
							throwable = e;
					}
					
				} while (field == null && (clazz = clazz.getSuperclass()) != null);
				
			} while (field == null && deepSearch && (currentClass = currentClass.getParent()) != null);
			
			if (field != null) {
				field.setAccessible(true);

				oFieldCache.put(cacheName, field);
				
			} else {
				throw new ReflectException("NoSuchFieldException: " + cacheName, throwable);
			}
		}
		
		mField = oFieldCache.get(cacheName);
		mReflectClass = reflectClass;
		mEventHandler = eventHandler;
	}
	
	private ReflectField() {}
	
	public void setValue(Object value) {
		Object receiver = mReflectClass.getReceiver();
		Boolean isStatic = Modifier.isStatic(mField.getModifiers());
		
		if (!isStatic && receiver == null) {
			receiver = mEventHandler.onEvent(Event.RECEIVER, this, null);
			
			if (receiver == null) {
				receiver = mReflectClass.getReceiver();
			}
		}
		
		try {
			mField.set(isStatic ? null : resolveReceiverInternal(receiver), value);
			
		} catch (Throwable e) {
			mEventHandler.onEvent(Event.ERROR, this, null);
			
			throw new ReflectException(e);
		}
	}
	
	public Object getValue() {
		Object receiver = mReflectClass.getReceiver();
		Boolean isStatic = Modifier.isStatic(mField.getModifiers());
		
		if (!isStatic && receiver == null) {
			receiver = mEventHandler.onEvent(Event.RECEIVER, this, null);
			
			if (receiver == null) {
				receiver = mReflectClass.getReceiver();
			}
		}
		
		try {
			return mField.get(isStatic ? null : resolveReceiverInternal(receiver));
			
		} catch (Throwable e) {
			mEventHandler.onEvent(Event.ERROR, this, null);
			
			throw new ReflectException(e);
		}
	}
	
	public ReflectClass getValueToInstance() {
		try {
			return new ReflectClass(getValue());
			
		} catch (Throwable e) {
			throw new ReflectException(e);
		}
	}
	
	public ReflectClass getValueForReceiver() {
		try {
			mReflectClass.setReceiver(getValue());
			
			return mReflectClass;
			
		} catch (Throwable e) {
			throw new ReflectException(e);
		}
	}
	
	@Override
	public Field getObject() {
		return mField;
	}

	@Override
	public ReflectClass getReflectClass() {
		return mReflectClass;
	}

	@Override
	public ReflectField resolveReceiver() {
		Object receiver = mReflectClass.getReceiver();
		Class<?> clazz = mField.getDeclaringClass();
		
		if (receiver != null && !clazz.isInstance(receiver)) {
			Object newReceiver = resolveReceiverInternal(receiver);
			
			if (newReceiver != receiver) {
				ReflectField newField = new ReflectField();
				
				newField.mReflectClass = new ReflectClass(newReceiver);
				newField.mField = mField;
				newField.mEventHandler = (OnReflectEvent) mEventHandler.onEvent(Event.HANDLER, newField, null);
				
				return newField;
			}
		}
		
		return this;
	}
}
