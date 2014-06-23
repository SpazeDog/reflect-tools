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

package com.spazedog.lib.reflecttools.utils;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

import com.spazedog.lib.reflecttools.ReflectClass;
import com.spazedog.lib.reflecttools.utils.ReflectException;

public abstract class ReflectMember<T> implements ReflectCallable<Member> {
	public static enum Match { BEST, EXACT }
	
	public abstract ReflectClass getReflectClass();
	public abstract T resolveReceiver();
	
	protected Object resolveReceiverInternal(Object receiver) {
		Class<?> clazz = ((ReflectCallable<Member>) this).getObject().getDeclaringClass();
		
		if (receiver != null && !clazz.isInstance(receiver)) {
			try {
				Object newReceiver = receiver;
				
				while ((newReceiver = newReceiver.getClass().getDeclaredField("this$0").get(newReceiver)) != null) {
					if (clazz.isInstance(newReceiver)) {
						return newReceiver;
					}
				}
				
			} catch (NoSuchFieldException e) {
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {}
		}
		
		return receiver;
	}
	
	public static abstract class ReflectParameters {
		protected final List<Class<?>> mParameterTypes = new ArrayList<Class<?>>();
		
		public Class<?>[] get() {
			return mParameterTypes.toArray(new Class<?>[mParameterTypes.size()]);
		}
		
		public Integer size() {
			return mParameterTypes.size();
		}
		
		public String toString() {
			String output = "";
			
			for (Class<?> type : mParameterTypes) {
				if (output.length() > 0) {
					output += ",";
				}

				output += type == null ? "NULL" : (type.getCanonicalName() == null ? type.getName() : type.getCanonicalName());
			}
			
			return output;
		}
		
		public static class ReflectParameterTypes extends ReflectParameters {
			public ReflectParameterTypes(Object... parameterTypes) {
				for (Object type : parameterTypes) {
					if (type == null) {
						mParameterTypes.add(null);
						
					} else if (type instanceof String) {
						try {
							mParameterTypes.add(ReflectClass.forName((String) type).getObject());
							
						} catch (ReflectException e) {
							throw new ReflectException(e.getMessage(), e);
						}
						
					} else if (type instanceof Class) {
						mParameterTypes.add((Class<?>) type);
						
					} else {
						mParameterTypes.add(type.getClass());
					}
				}
			}
		}
		
		public static class ReflectArgumentTypes extends ReflectParameters {
			public ReflectArgumentTypes(Object... args) {
				for (Object type : args) {
					mParameterTypes.add(type == null ? null : type.getClass());
				}
			}
		}
	}
}
