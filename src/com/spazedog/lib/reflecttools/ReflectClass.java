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

import android.os.Build;
import android.os.IBinder;

import com.spazedog.lib.reflecttools.ReflectMember.Match;
import com.spazedog.lib.reflecttools.ReflectMember.ReflectMemberException;
import com.spazedog.lib.reflecttools.ReflectMember.Result;
import com.spazedog.lib.reflecttools.ReflectParameterTypes.ReflectParameterException;
import com.spazedog.lib.reflecttools.bridge.MethodBridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class ReflectClass extends ReflectObject<Class<?>> {

    protected OnClassReceiverListener mListener;
    protected volatile boolean mListenerActive = false;

	public static interface OnClassReceiverListener {
        public Object onRequestReceiver(ReflectClass reflectClass);
    }
	
	public static class ReflectClassException extends ReflectException {
		private static final long serialVersionUID = -8476128751589149058L;

		public ReflectClassException(Throwable cause) {
			super(cause);
		}

		public ReflectClassException(String detailMessage, Throwable cause) {
			super(detailMessage, cause);
		}
		
		public ReflectClassException(String detailMessage) {
			super(detailMessage, null);
		}
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected final static HashMap<String, Class<?>> oClassCache = new HashMap<String, Class<?>>();
	
	/**
	 * Finds the best suited {@link ClassLoader}. 
	 * This method also handles special cases in Android >= Lollipop where the Boot {@link ClassLoader} 
	 * cannot access specific internal packages. 
	 */
	public static ClassLoader findClassLoader() {
		/*
		 * In Lollipop and above we need to bypass some restrictions
		 */
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Thread current = Thread.currentThread();
			
			if (current != null) {
				return current.getContextClassLoader();
			}
		}
		
		return ClassLoader.getSystemClassLoader();
	}
	
	/**
	 * @see #findClass(String, ClassLoader)
	 */
	public static Class<?> findClass(String className) throws ReflectClassException {
		return findClass(className, findClassLoader());
	}
	
	/**
	 * This is the same as using {@link Class#forName(String, boolean, ClassLoader)} only that 
	 * this method handles a few issues that some times occurs when parsing a {@link ClassLoader}. 
	 * This method also stores an internal cache that speed things up if the same class is searched multiple times.  
	 * 
	 * @param className
	 * 		Full name of the class to find
	 * 
	 * @param loader
	 * 		A {@link ClassLoader} object. If this is <code>NULL</code> then {@link #findClassLoader()} is used instead
	 * 
	 * @throws ReflectClassException
	 *  	This is thrown if the class could not be located
	 */
	public static Class<?> findClass(String className, ClassLoader loader) throws ReflectClassException {
		Class<?> clazz = oClassCache.get(className);
		
		if (clazz == null) {
			ClassLoader[] loaderArray = loader != null ? new ClassLoader[]{loader, null} : new ClassLoader[]{null};
			Throwable throwable = null;
			
			for (ClassLoader forLoader : loaderArray) {
				try {
					/*
					 * Some times it will fail if you provide a classloader, so on failure, we will try without. 
					 */
					clazz = forLoader != null ? Class.forName(className, false, forLoader) : Class.forName(className);
					
				} catch (ClassNotFoundException e) {
					throwable = e;
					
				} finally {
					if (clazz != null) {
						oClassCache.put(className, clazz); break;
					}
				}
			}
			
			if (clazz == null) {
				throw new ReflectClassException("Could not locate the class " + className, throwable);
			}
		}
		
		return clazz;
	}
	
	/**
	 * @see #instantiateName(String, ClassLoader, Object...)
	 */
	public static ReflectClass instantiateName(String className, Object... arguments) throws ReflectClassException, ReflectMemberException {
		return instantiateName(className, findClassLoader(), arguments);
	}
	
	/**
	 * This is the same as {@link #fromName(String, ClassLoader)}, only this method will also instantiate the class 
	 * using the constructor matching the parsed arguments. It will search for a constructor using {@link Match#BEST} based on the 
	 * {@link Class} types of the arguments. It will then invoke the constructor and add the receiver to this class's receiver. 
	 * For more precise constructor search, manually invoke one using {@link #findConstructor(Match, ReflectParameterTypes)}
	 * 
	 * @param className
	 * 		Full name of the class to find
	 * 
	 * @param loader
	 * 		A {@link ClassLoader} object. If this is <code>NULL</code> then {@link #findClassLoader()} is used instead
	 * 
	 * @param arguments
	 * 		Arguments that should be parsed to the constructor. The constructor will be found using the types of these arguments using a best match.
	 *
	 * @throws ReflectClassException
	 * 		Thrown if the class could not be found
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the constructor could not be found or failed when being invoked
	 */
	public static ReflectClass instantiateName(String className, ClassLoader loader, Object... arguments) throws ReflectClassException, ReflectMemberException {
		ReflectClass rclass = fromName(className, loader);
		ReflectParameterTypes reflectTypes = null;
		Object receiver = null;
		
		if (arguments != null && arguments.length > 0) {
			reflectTypes = new ReflectParameterTypes();
			
			for (Object arg : arguments) {
				reflectTypes.addResolveType(arg);
			}
			
			receiver = rclass.findConstructor(Match.BEST, reflectTypes).invoke(Result.DATA, arguments);
			
		} else {
			receiver = rclass.findConstructor(Match.BEST, reflectTypes).invoke(Result.DATA);
		}
		
		rclass.setReceiver(receiver);
		
		return rclass;
	}
	
	/**
	 * @see #instantiateName(String, ClassLoader, Object...)
	 */
	public static ReflectClass instantiateClass(Class<?> clazz, Object... arguments) throws ReflectClassException, ReflectMemberException {
		return instantiateName(clazz.getName(), clazz.getClassLoader(), arguments);
	}
	
	/**
	 * @see #fromName(String, ClassLoader)
	 */
	public static ReflectClass fromName(String className) throws ReflectClassException {
		return fromName(className, findClassLoader());
	}
	
	/**
	 * Find a {@link Class} based on class name and return a new {@link ReflectClass} instance with the located 
	 * {@link Class} attached to it
	 * 
	 * @param className
	 * 		Full name of the class to find
	 * 
	 * @param loader
	 * 		A {@link ClassLoader} object. If this is <code>NULL</code> then {@link #findClassLoader()} is used instead
	 * 
	 * @throws ReflectClassException
	 *  	This is thrown if the class could not be located
	 */
	public static ReflectClass fromName(String className, ClassLoader loader) throws ReflectClassException {
		if (className != null) {
			return new ReflectClass( findClass(className, loader) );
		}
		
		throw new ReflectClassException("Cannot locate a class from a NULL class name");
	}
	
	/**
	 * Attach a {@link Class} to a new instance of {@link ReflectClass}
	 * 
	 * @param clazz
	 * 		The {@link Class} to attach to this instance
	 */
	public static ReflectClass fromClass(Class<?> clazz) throws ReflectClassException {
		if (clazz != null) {
			return new ReflectClass(clazz);
		}
		
		throw new ReflectClassException("Cannot create a ReflectClass instance from a NULL Class<?>");
	}
	
	/**
	 * Attach the {@link Class} of a receiver to a new instance of {@link ReflectClass} and 
	 * add the receiver itself to be used with this instance whenever invoking members from it 
	 * 
	 * @param receiver
	 * 		A class instance
	 * 
	 * @see #setReceiver(Object)
	 */
	public static ReflectClass fromReceiver(Object receiver) throws ReflectClassException {
		if (receiver != null) {
            ReflectClass rclazz = new ReflectClass(receiver.getClass());
			rclazz.setReceiver(receiver);
			
			return rclazz;
		}
		
		throw new ReflectClassException("Cannot create a ReflectClass instance from a NULL Receiver");
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected Class<?> mClass;
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected Object mReceiver;
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected ReflectClass(Class<?> clazz) {
		mClass = clazz;
	}

    public void setReceiverListener(OnClassReceiverListener listener) {
        mListener = listener;
    }
	
	/**
	 * Set the receiver used when invoking members belonging to it
	 * 
	 * @param receiver
	 * 		A class instance of this class object
	 */
	public void setReceiver(Object receiver) {
		mReceiver = receiver;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Object getReceiver() {
        Object receiver = null;

        if (mListener != null && !mListenerActive) {
            mListenerActive = true;
            receiver = mListener.onRequestReceiver(this);
            mListenerActive = false;
        }

        if (receiver == null) {
            receiver = mReceiver;
        }

		return receiver;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getObject() {
		return mClass;
	}
	
	/**
	 * Whether or not the class was declared static
	 */
	public boolean isStatic() {
		return Modifier.isStatic(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class was declared private
	 */
	public boolean isPrivate() {
		return Modifier.isPrivate(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class was declared protected
	 */
	public boolean isProtected() {
		return Modifier.isProtected(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class was declared public
	 */
	public boolean isPublic() {
		return Modifier.isPublic(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class was declared abstract
	 */
	public boolean isAbstract() {
		return Modifier.isAbstract(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class is an interface
	 */
	public boolean isInterface() {
		return Modifier.isInterface(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class was declared final
	 */
	public boolean isFinal() {
		return Modifier.isFinal(mClass.getModifiers());
	}
	
	/**
	 * Whether or not the class is a nested class
	 */
	public boolean isNested() {
		return mClass.getName().contains("$");
	}

	/**
	 * This will return a new {@link ReflectClass} of the parent class. 
	 * If this instance has a receiver then the parent receiver will be added as well. <br /><br />
	 * 
	 * <Code>NULL</code> is returned if this is not a nested class. 
	 */
	public ReflectClass getParent() {
		try {
			if (mReceiver != null) {
				return ReflectClass.fromReceiver( getParentReceiver() );
				
			} else {
				return ReflectClass.fromClass( getParentClass() );
			}
			
		} catch (ReflectClassException e) {}
		
		return null;
	}
	
	/**
	 * Returns a new {@link ReflectClass} instance pointing at a nested class within the current class. 
	 * If the nested class is not declared as static, it will get a reference of the current receiver which will 
	 * allow you to instantiate it. <br /><br />
	 * 
	 * <Code>NULL</code> is returned if no nested class was found
	 * 
	 * @param simpleName
	 * 		The name of the nested class. This should be the simple name
	 */
	public ReflectClass getNested(String simpleName) {
		Class<?> clazz = getNestedClass(simpleName);
		
		if (clazz != null) {
			ReflectClass rclass = ReflectClass.fromClass(clazz);
			
			/*
			 * If this is not static, we need the parent receiver to create new instances
			 */
			if (!rclass.isStatic()) {
				rclass.setReceiver( getReceiver() );
			}
			
			return rclass;
		}
		
		return null;
	}
	
	/**
	 * This is the same as {@link #getNested(String)} only this one will also instantiate the nested class. 
	 * It will search for a constructor based on the parsed arguments using a best match. If the nested class 
	 * is not static, it will instantiate the class based on it's parent receiver. 
	 * 
	 * @param simpleName
	 * 		The name of the nested class. This should be the simple name
	 * 
	 * @param arguments
	 * 		Arguments to be parsed to the constructor
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if a matching constructor could not be found or if it failed being invoked
	 */
	public ReflectClass getNestedInstantiated(String simpleName, Object... arguments) throws ReflectMemberException {
		ReflectClass rclass = getNested(simpleName);
		ReflectParameterTypes reflectTypes = null;
		Object receiver = null;
		
		if (rclass != null) {
			if (arguments != null && arguments.length > 0) {
				reflectTypes = new ReflectParameterTypes();
				
				for (Object arg : arguments) {
					reflectTypes.addResolveType(arg);
				}
				
				receiver = rclass.findConstructor(Match.BEST, reflectTypes).invoke(Result.DATA, arguments);
				
			} else {
				receiver = rclass.findConstructor(Match.BEST, reflectTypes).invoke(Result.DATA);
			}
			
			rclass.setReceiver(receiver);
		}
		
		return rclass;
	}
	
	/**
	 * Returns the receiver of the parent class or <code>NULL</code> if the current class does not have 
	 * a receiver or if the current class is not a nested instance. 
	 */
	public Object getParentReceiver() {
		if (mReceiver != null) {
			try {
				Field field = mReceiver.getClass().getDeclaredField("this$0");
				field.setAccessible(true);
				
				return field.get(mReceiver);
				
			} catch (NoSuchFieldException e) {
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {}
		}
		
		return null;
	}
	
	/**
	 * Returns the parent {@link Class} or <code>NULL</code> if the current class 
	 * is not a nested class
	 */
	public Class<?> getParentClass() {
		try {
			String clazzName = mClass.getName();
			int index = clazzName.lastIndexOf("$");
			
			if (index > 0) {
				return ReflectClass.findClass(clazzName.substring(0, index), mClass.getClassLoader());
			}
			
		} catch (ReflectClassException e) {}
		
		return null;
	}
	
	/**
	 * Locates a nested class based on it's simple name. Returns <code>NULL</code> 
	 * if no nested class was found. <br /><br />
	 * 
	 * This method will also search super classes if the current class has no nested class matching the name. 
	 */
	public Class<?> getNestedClass(String simpleName) {
		Class<?> clazz = mClass;
		
		do {
			try {
				return ReflectClass.findClass(clazz.getName() + "$" + simpleName, clazz.getClassLoader());
				
			} catch (ReflectClassException e) {}
			
		} while ((clazz = clazz.getSuperclass()) != null);
		
		return null;
	}
	
	/**
	 * @see #invokeConstructor(Match, Object...)
	 */
	public ReflectClass invokeConstructor(Object... arguments) throws ReflectMemberException, ReflectParameterException {
		return invokeConstructor(Match.BEST, arguments);
	}
	
	/**
	 * This is a shortcut that will both search for and invoke a {@link Constructor} based on 
	 * the arguments parsed. 
	 * 
	 * @see #findConstructor(Match, Object...)
	 * @see ReflectConstructor#invoke(Result, Object...)
	 * 
	 * @param match
	 * 		How deep to search for the {@link Constructor}
	 * 
	 * @param arguments
	 * 		Arguments that should be parsed to the {@link Constructor}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Constructor} could not be found or invoked
	 * 
	 * @throws ReflectParameterException
	 * 		Thrown if an error occurs while getting the types of the arguments
	 */
	public ReflectClass invokeConstructor(Match match, Object... arguments) throws ReflectMemberException, ReflectParameterException {
		ReflectParameterTypes reflectTypes = null;
		
		if (arguments != null && arguments.length > 0) {
			reflectTypes = new ReflectParameterTypes();
			
			for (Object arg : arguments) {
				reflectTypes.addResolveType(arg);
			}
			
			return (ReflectClass) findConstructor(match, reflectTypes).invoke(Result.INSTANCE, arguments);
		}
		
		return (ReflectClass) findConstructor(match, reflectTypes).invoke(Result.INSTANCE);
	}
	
	/**
	 * @see #findConstructor(Match, Object...)
	 */
	public ReflectConstructor findConstructor(Object... parameterTypes) throws ReflectMemberException, ReflectParameterException {
		return findConstructor(Match.BEST, parameterTypes);
	}
	
	/**
	 * Search for a {@link Constructor} within this class. 
	 * 
	 * @see #findConstructor(Match, ReflectParameterTypes)
	 * 
	 * @param match
	 * 		How deep to search for the {@link Constructor}
	 * 
	 * @param parameterTypes
	 * 		The {@link Class} types of the arguments that should match the {@link Constructor}. 
	 * 		This can also be {@link String} representing the whole {@link Class} path which will be converted into a {@link Class} type
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Constructor} could not be found
	 * 
	 * @throws ReflectParameterException
	 * 		Thrown if an error occurs while converting {@link String} types into {@link Class} types
	 */
	public ReflectConstructor findConstructor(Match match, Object... parameterTypes) throws ReflectMemberException, ReflectParameterException {
		ReflectParameterTypes reflectTypes = null;
		
		if (parameterTypes != null && parameterTypes.length > 0) {
			reflectTypes = new ReflectParameterTypes();
			
			for (Object type : parameterTypes) {
				if (type instanceof Class) {
					reflectTypes.addType((Class<?>) type);
					
				} else if (type instanceof String) {
					reflectTypes.addType((String) type, mClass.getClassLoader());
				}
			}
		}
		
		return new ReflectConstructor(this, 
				ReflectConstructor.findConstructor(match, this, reflectTypes)
		);
	}
	
	/**
	 * Search for a {@link Constructor} within this class. 
	 * 
	 * @see #findConstructor(Match, Object...)
	 * 
	 * @param match
	 * 		How deep to search for the {@link Constructor}
	 * 
	 * @param parameterTypes
	 * 		A {@link ReflectParameterTypes} instance containing the parameter types that should match the {@link Constructor}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Constructor} could not be found
	 */
	public ReflectConstructor findConstructor(Match match, ReflectParameterTypes parameterTypes) throws ReflectMemberException {
		return new ReflectConstructor(this, 
				ReflectConstructor.findConstructor(match, this, parameterTypes)
		);
	}

	/**
	 * @see #invokeMethod(String, Match, Object...)
	 */
	public Object invokeMethod(String methodName, Object... arguments) throws ReflectMemberException, ReflectParameterException {
		return invokeMethod(methodName, Match.DEEP, arguments);
	}
	
	/**
	 * This is a shortcut that will both search for and invoke a {@link Method} based on 
	 * the arguments parsed. 
	 * 
	 * @see #findMethod(String, Match, Object...)
	 * @see ReflectMethod#invoke(Result, Object...)
	 * 
	 * @param methodName
	 * 		The name of the {@link Method}
	 * 
	 * @param match
	 * 		How deep to search for the {@link Method}
	 * 
	 * @param arguments
	 * 		Arguments that should be parsed to the {@link Method}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Method} could not be found or invoked
	 * 
	 * @throws ReflectParameterException
	 * 		Thrown if an error occurs while getting the types of the arguments
	 */
	public Object invokeMethod(String methodName, Match match, Object... arguments) throws ReflectMemberException, ReflectParameterException {
		ReflectParameterTypes reflectTypes = null;
		
		if (arguments != null && arguments.length > 0) {
			reflectTypes = new ReflectParameterTypes();
			
			for (Object arg : arguments) {
				reflectTypes.addResolveType(arg);
			}
			
			return findMethod(methodName, match, reflectTypes).invoke(Result.DATA, arguments);
		}
		
		return findMethod(methodName, match, reflectTypes).invoke(Result.DATA);
	}
	
	/**
	 * @see #findMethod(String, Match, Object...)
	 */
	public ReflectMethod findMethod(String methodName, Object... parameterTypes) throws ReflectMemberException, ReflectParameterException {
		return findMethod(methodName, Match.DEEP, parameterTypes);
	}
	
	/**
	 * Search for a {@link Method} within this class. 
	 * 
	 * @see #findMethod(String, Match, ReflectParameterTypes)
	 * 
	 * @param methodName
	 * 		The name of the {@link Method}
	 * 
	 * @param match
	 * 		How deep to search for the {@link Method}
	 * 
	 * @param parameterTypes
	 * 		The {@link Class} types of the arguments that should match the {@link Method}. 
	 * 		This can also be {@link String} representing the whole {@link Class} path which will be converted into a {@link Class} type
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Method} could not be found
	 * 
	 * @throws ReflectParameterException
	 * 		Thrown if an error occurs while converting {@link String} types into {@link Class} types
	 */
	public ReflectMethod findMethod(String methodName, Match match, Object... parameterTypes) throws ReflectMemberException, ReflectParameterException {
		ReflectParameterTypes reflectTypes = null;
		
		if (parameterTypes != null && parameterTypes.length > 0) {
			reflectTypes = new ReflectParameterTypes();
			
			for (Object type : parameterTypes) {
				if (type instanceof Class) {
					reflectTypes.addType((Class<?>) type);
					
				} else if (type instanceof String) {
					reflectTypes.addType((String) type, mClass.getClassLoader());
				}
			}
		}
		
		return new ReflectMethod(this, 
				ReflectMethod.findMethod(methodName, match, this, reflectTypes)
		);
	}
	
	/**
	 * Search for a {@link Method} within this class. 
	 * 
	 * @see #findMethod(String, Match, Object...)
	 * 
	 * @param methodName
	 * 		The name of the {@link Method}
	 * 
	 * @param match
	 * 		How deep to search for the {@link Method}
	 * 
	 * @param parameterTypes
	 * 		A {@link ReflectParameterTypes} instance containing the parameter types that should match the {@link Method}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Method} could not be found
	 */
	public ReflectMethod findMethod(String methodName, Match match, ReflectParameterTypes parameterTypes) throws ReflectMemberException {
		return new ReflectMethod(this, 
				ReflectMethod.findMethod(methodName, match, this, parameterTypes)
		);
	}

	/**
	 * @see #getFieldValue(String, Match)
	 */
	public Object getFieldValue(String fieldName) throws ReflectMemberException {
		return getFieldValue(fieldName, Match.DEEP);
	}
	
	/**
	 * This is a shortcut that will both search for and get the value from a {@link Field}
	 * 
	 * @see #findField(String, Match)
	 * @see ReflectField#getValue(Result)
	 * 
	 * @param fieldName
	 * 		Name of the {@link Field}
	 * 
	 * @param match
	 * 		How deep to search for the {@link Field}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Field} could not be found or invoked
	 */
	public Object getFieldValue(String fieldName, Match match) throws ReflectMemberException {
		return findField(fieldName, match).getValue(Result.DATA);
	}
	
	/**
	 * @see #setFieldValue(String, Match, Object)
	 */
	public void setFieldValue(String fieldName, Object value) throws ReflectMemberException {
		setFieldValue(fieldName, Match.DEEP, value);
	}
	
	/**
	 * This is a shortcut that will both search for and change the value of a {@link Field}
	 * 
	 * @see #findField(String, Match)
	 * @see ReflectField#setValue(Object)
	 * 
	 * @param fieldName
	 * 		Name of the {@link Field}
	 * 
	 * @param match
	 * 		How deep to search for the {@link Field}
	 * 
	 * @param value
	 * 		The value to add to the fieldName
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Field} could not be found or changed
	 */
	public void setFieldValue(String fieldName, Match match, Object value) throws ReflectMemberException {
		findField(fieldName, match).setValue(value);
	}
	
	/**
	 * @see #findField(String, Match)
	 */
	public ReflectField findField(String fieldName) throws ReflectMemberException {
		return findField(fieldName, Match.DEEP);
	}
	
	/**
	 * Search for a {@link Field} within this class.
	 * 
	 * @param fieldName
	 * 		The name of the {@link Field}
	 * 
	 * @param match
	 * 		How deep to search for the {@link Field}
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if the {@link Field} could not be found
	 */
	public ReflectField findField(String fieldName, Match match) throws ReflectMemberException {
		return new ReflectField(this, 
				ReflectField.findField(fieldName, match, this)
		);
	}

	/**
	 * Add a hook to all {@link Constructor}'s of this class
	 * 
	 * @param callback
	 * 		A callback instance that will be called whenever someone calls one of the {@link Constructor}'s
	 * 
	 * @throws ReflectClassException
	 * 		Thrown if no {@link Constructor}'s could be found or if it was not possible to add the hook due to missing 
	 * 		injection systems, such as Xposed Framework and Cydia Substrate
	 * 
	 * @return
	 * 		The number of {@link Constructor}'s that was affected
	 */
	public int bridge(MethodBridge callback) throws ReflectClassException {
		return bridge(null, callback);
	}
	
	/**
	 * Add a hook to all {@link Method}'s of this class with a specific name
	 * 
	 * @param methodName
	 * 		The name of the {@link Method}'s
	 * 
	 * @param callback
	 * 		A callback instance that will be called whenever someone calls one of the {@link Method}'s
	 * 
	 * @throws ReflectClassException
	 * 		Thrown if no {@link Method}'s could be found matching the name or if it was not possible to add the hook due to missing 
	 * 		injection systems, such as Xposed Framework and Cydia Substrate
	 * 
	 * @return
	 * 		The number of {@link Method}'s that was affected
	 */
	public int bridge(String methodName, MethodBridge callback) throws ReflectClassException {
		if (ReflectUtils.bridgeInitiated()) {
			try {
				Member[] members = methodName != null ? mClass.getDeclaredMethods() : mClass.getDeclaredConstructors();
				Integer count = 0;
				
				for (Member member : members) {
					if (methodName == null || member.getName().equals(methodName)) {
						callback.attachBridge(member); count += 1;
					}
				}
				
				if (count == 0) {
					throw new ReflectClassException("Could not find any " + (methodName == null ? "constructors" : "methods matching the name " + methodName) + " for the class " + mClass.getName());
				}
				
				return count;
				
			} catch (Throwable e) {
				throw new ReflectClassException("Error while injecting runtime code to the " + (methodName == null ? "constructors" : "methods matching the name " + methodName) + " for " + mClass.getName(), e);
			}
			
		} else {
			throw new ReflectClassException("Cannot inject runtime code while no bridge has been initiated, attempted on " + (methodName == null ? "constructors" : "methods matching the name " + methodName) + " for " + mClass.getName());
		}
	}
	
	/**
	 * Bind this {@link ReflectClass} instance as an interface to a system service {@link IBinder}. Note that the {@link Class} attached to this instance 
	 * needs to be a valid interface for that specific service {@link IBinder}. <br /><br />
	 * 
	 * This will give you easy access to services and service methods not normally available through the regular Service Manager. 
	 * 
	 * @param service
	 * 		The name of the service to bind to
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if using an invalid interface
	 * 
	 * @throws ReflectParameterException
	 * 		Thrown if an {@link IBinder} could not be found
	 * 
	 * @return Itself with an {@link IBinder} Receiver
	 */
    private static boolean oServiceManagerReady = false;
	public ReflectClass bindInterface(String service) throws ReflectMemberException, ReflectParameterException {
        if (!oServiceManagerReady) {
            IBinder managerBinder = (IBinder) fromName("com.android.internal.os.BinderInternal").invokeMethod("getContextObject");

            if (managerBinder != null) {
                oServiceManagerReady = true;
            }
        }

        if (oServiceManagerReady) {
            IBinder binder = (IBinder) fromName("android.os.ServiceManager").invokeMethod("getService", service);

            if (binder != null) {
                return bindInterface(binder);

            } else {
                throw new ReflectClassException("Cannot bind to a service using a NULL IBinder\n\t\t" +
                        "Service Name = " + service + "\n\t\t" +
                        "Interface = " + mClass.getName());
            }

        } else {
            throw new ReflectClassException("The native service manager is not yet ready\n\t\t" +
                    "Service Name = " + service + "\n\t\t" +
                    "Interface = " + mClass.getName());
        }
	}
	
	/**
	 * @see #bindInterface(String)
	 * 
	 * @param binder
	 * 		The {@link IBinder} to bind to the interface
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if using an invalid interface
	 * 
	 * @throws ReflectParameterException
	 * 		Thrown if an {@link IBinder} is <code>NULL</code>
	 * 
	 * @return Itself with the {@link IBinder} Receiver
	 */
	public ReflectClass bindInterface(IBinder binder) throws ReflectMemberException, ReflectParameterException {
		if (binder != null) {
            ReflectClass callee = this;

			if (!mClass.getName().endsWith("$Stub")) {
                try {
				    callee = getNested("Stub");

                } catch (ReflectException e) {
                    /*
                     * Some Android internal services does not use 'Stub'
                     */
                    callee = this;
                }
			}
			
			mReceiver = callee.invokeMethod("asInterface", binder);
            mClass = mReceiver.getClass();
			
			return this;
			
		} else {
			throw new ReflectClassException("Cannot bind to a service using a NULL IBinder\n\t\t" + 
					"Interface = " + mClass.getName());
		}
	}
}
