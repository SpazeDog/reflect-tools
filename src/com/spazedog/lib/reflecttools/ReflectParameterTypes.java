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

import java.util.ArrayList;
import java.util.List;

public class ReflectParameterTypes {
	
	public static class ReflectParameterException extends ReflectException {
		private static final long serialVersionUID = -8625768849575464987L;

		public ReflectParameterException(Throwable cause) {
			super(cause);
		}

		public ReflectParameterException(String detailMessage, Throwable cause) {
			super(detailMessage, cause);
		}
	}

	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected final List<Class<?>> mParameterTypes = new ArrayList<Class<?>>();
	
	/**
	 * @see #addType(String, ClassLoader)
	 */
	public ReflectParameterTypes addType(String clazzName) {
		return addType(clazzName, null);
	}
	
	/**
	 * Add a parameter type based on it's full class name. 
	 * The method will locate the class object associated with the name and 
	 * add it to the parameter list.
	 * 
	 * @param clazzName
	 * 		The name of the class type
	 * 
	 * @param loader
	 * 		{@link ClassLoader} to use for the search or NULL to use {@link ReflectClass} default
	 * 
	 * @throws ReflectParameterException
	 * 		This is thrown if {@link ReflectClass} was unable to resolve the class name
	 */
	public ReflectParameterTypes addType(String clazzName, ClassLoader loader) throws ReflectParameterException {
		if (clazzName != null) {
			try {
				mParameterTypes.add(ReflectClass.findClass(clazzName, loader));
				
			} catch (ReflectException e) {
				throw new ReflectParameterException("Could not locate the parameter type class " + clazzName, e);
			}
		}
		
		return this;
	}
	
	/**
	 * Add a {@link Class} object representing a parameter type
	 * 
	 * @param clazz
	 * 		The {@link Class} object that should be added to the parameter list
	 */
	public ReflectParameterTypes addType(Class<?> clazz) {
		if (clazz != null) {
			mParameterTypes.add(clazz);
		}
		
		return this;
	}
	
	/**
	 * Locates the {@link Class} object of an unknown argument. 
	 * Use this if you have an unknown argument/variable which should be converted into 
	 * a {@link Class} representing a parameter type
	 * 
	 * @param argument
	 * 		An unknown argument/variable
	 */
	public ReflectParameterTypes addResolveType(Object argument) {
		mParameterTypes.add(argument == null ? 
				null : argument.getClass());
		
		return this;
	}
	
	/**
	 * Returns the current parameter type array as a {@link String} representation
	 */
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
	
	/**
	 * Returns the current parameter type array
	 */
	public Class<?>[] toArray() {
		return mParameterTypes.toArray(new Class<?>[mParameterTypes.size()]);
	}
	
	/**
	 * Returns the current size of the parameter type array
	 */
	public Integer size() {
		return mParameterTypes.size();
	}
}
