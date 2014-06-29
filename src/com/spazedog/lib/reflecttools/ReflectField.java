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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.utils.ReflectException;
import com.spazedog.lib.reflecttools.utils.ReflectMember;

public class ReflectField extends ReflectMember<ReflectField> {
	private final static HashMap<String, Field> oFieldCache = new HashMap<String, Field>();
	
	private Field mField;
	private ReflectClass mReflectClass;
	
	public ReflectField(ReflectClass reflectClass, Field field) {
		mField = field;
		mReflectClass = reflectClass;
	}
	
	public ReflectField(ReflectClass reflectClass, String fieldName, Boolean deepSearch) {
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
	}
	
	public void setValue(Object value) {
		Object receiver = mReflectClass.getReceiver();
		Boolean isStatic = Modifier.isStatic(mField.getModifiers());
		
		if (!isStatic && receiver == null) {
			receiver = mReflectClass.triggerReceiverEvent(this);
			
			if (receiver == null) {
				receiver = mReflectClass.getReceiver();
			}
		}
		
		try {
			mField.set(isStatic ? null : resolveReceiverInternal(receiver), value);
			
		} catch (Throwable e) {
			mReflectClass.triggerErrorEvent(this);
			
			throw new ReflectException(e);
		}
	}
	
	public Object getValue() {
		Object receiver = mReflectClass.getReceiver();
		Boolean isStatic = Modifier.isStatic(mField.getModifiers());
		
		if (!isStatic && receiver == null) {
			receiver = mReflectClass.triggerReceiverEvent(this);
			
			if (receiver == null) {
				receiver = mReflectClass.getReceiver();
			}
		}
		
		try {
			return mField.get(isStatic ? null : resolveReceiverInternal(receiver));
			
		} catch (Throwable e) {
			mReflectClass.triggerErrorEvent(this);
			
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
				return new ReflectField(new ReflectClass(newReceiver), mField);
			}
		}
		
		return this;
	}
}
