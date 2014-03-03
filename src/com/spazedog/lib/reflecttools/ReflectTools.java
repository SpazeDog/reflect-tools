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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with ReflectTools. If not, see <http://www.gnu.org/licenses/>
 * 
 */

package com.spazedog.lib.reflecttools;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.apache.Common;

import android.util.Property;

public class ReflectTools {
	public static final String TAG = ReflectTools.class.getName();
	
	protected static final HashMap<String, ReflectClass> oClassCache = new HashMap<String, ReflectClass>();
	protected static final ClassLoader oClassLoader = ClassLoader.getSystemClassLoader();
	
	/**
	 * <p>Used by any {@link Member} locator methods to define the precision of the {@link Member} search</p>
	 * 
	 * <p>This {@link Property} will only cache {@link Member}'s by using their <code>Arguments</code> length in the cache name. 
	 * If a class has more than one Member of the same name with the same amount of <code>Arguments</code>, then this should not be used. 
	 * Otherwise this is recommended as it will load the cached {@link Member}'s much faster.</p>
	 * 
	 * @see ReflectTools#MEMBER_MATCH_EXACT
	 * @see ReflectTools#MEMBER_MATCH_BEST
	 * 
	 * @see ReflectTools#findMember(Class, String, Integer, Class...)
	 * @see ReflectTools#findMemberRecursive(Class, String, Integer, Class...)
	 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
	 */
	public static final int MEMBER_MATCH_FAST = -1;
	
	/**
	 * <p>Used by any {@link Member} locator methods to define the precision of the {@link Member} search</p>
	 * 
	 * <p>This {@link Property} will cache {@link Member}'s by using their <code>Arguments</code> <code>Parameter Type</code> in the cache name.
	 * Also this {@link Property} will only look for {@link Member}'s by their exact <code>Parameter Type</code>, hence <code>int</code> will not match {@link Integer}. 
	 * For that you should use {@link ReflectTools#MEMBER_MATCH_BEST} instead.</p>
	 * 
	 * @see ReflectTools#MEMBER_MATCH_FAST
	 * @see ReflectTools#MEMBER_MATCH_BEST
	 * 
	 * @see ReflectTools#findMember(Class, String, Integer, Class...)
	 * @see ReflectTools#findMemberRecursive(Class, String, Integer, Class...)
	 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
	 */
	public static final int MEMBER_MATCH_EXACT = 2;
	
	/**
	 * <p>Used by any {@link Member} locator methods to define the precision of the {@link Member} search</p>
	 * 
	 * <p>This {@link Property} will cache {@link Member}'s by using their <code>Arguments</code> <code>Parameter Type</code> in the cache name.
	 * Also this {@link Property} will only look for {@link Member}'s with the best <code>Parameter Type</code> match.</p>
	 * 
	 * @see ReflectTools#MEMBER_MATCH_FAST
	 * @see ReflectTools#MEMBER_MATCH_EXACT
	 * 
	 * @see ReflectTools#findMember(Class, String, Integer, Class...)
	 * @see ReflectTools#findMemberRecursive(Class, String, Integer, Class...)
	 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
	 */
	public static final int MEMBER_MATCH_BEST = 0;
	
	/**
	 * <p>Returns a new or cached {@link ReflectTools.ReflectClass} instance</p>
	 * 
	 * @param className
	 * 		Full name of the class to locate and return within the wrapper
	 * 
	 * @param classLoader
	 * 		Alternative {@link ClassLoader} or <code>NULL</code> to use the default
	 * 
	 * @see ReflectTools#getReflectClass(Object)
	 * @see ReflectTools.ReflectClass
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static ReflectClass getReflectClass(String className, ClassLoader classLoader) {
		if (!oClassCache.containsKey(className)) {
			try {
				oClassCache.put(className, new ReflectClass(findClass(className, classLoader)));
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		return oClassCache.get(className);
	}
	
	/**
	 * <p>Returns a new or cached {@link ReflectTools.ReflectClass} instance</p>
	 * 
	 * @param clazzObject
	 * 		A {@link Class} or {@link Object} to add to the wrapper.
	 * 		If {@link Object} is passed then {@link Object#getClass()} will be used
	 * 
	 * @see ReflectTools#getReflectClass(String, ClassLoader)
	 * @see ReflectTools.ReflectClass
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static ReflectClass getReflectClass(Object clazzObject) {
		if (clazzObject instanceof String) {
			return getReflectClass((String) clazzObject, null);
		}
		
		Class<?> clazz = clazzObject instanceof Class ? (Class<?>) clazzObject : clazzObject.getClass();
		String className = clazz.getName();
				
		if (!oClassCache.containsKey(className)) {
			oClassCache.put(className, new ReflectClass(clazz));
		}
		
		return oClassCache.get(className);
	}
	
	/**
	 * <p>Find a {@link Class}</p>
	 * 
	 * <p>This method is originally meant for internal usage only and therefore it has no caching. 
	 * You should use {@link ReflectTools#getReflectClass(String, ClassLoader)} instead.</p>
	 * 
	 * @param className
	 * 		Full name of the class to locate
	 * 
	 * @param classLoader
	 * 		Alternative {@link ClassLoader} or <code>NULL</code> to use the default
	 * 
	 * @see ReflectTools#getReflectClass(String, ClassLoader)
	 * @see ReflectTools#getReflectClass(Object)
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static Class<?> findClass(String className, ClassLoader classLoader) {
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
			return clazz;
			
		} else {
			throw new ReflectException("ClassNotFoundException: " + className, throwable);
		}
	}
	
	/**
	 * <p>Convert an {@link Object} array of <code>Arguments</code> into a {@link Class} array of <code>Parameter Types</code> based on their {@link Class} type</p>
	 * 
	 * @param args
	 * 		The argument to convert
	 * 
	 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
	 */
	public static Class<?>[] convertArgsToTypes(Object... args) {
		Class<?>[] types = new Class<?>[ args.length ];
		
		for (int i=0; i < args.length; i++) {
			types[i] = args[i] != null ? args[i].getClass() : null;
		}
		
		return types;
	}
	
	/**
	 * <p>Convert a mixed {@link Object} array of <code>Parameter Types</code> into a real {@link Class} array</p>
	 * 
	 * <p>Each {@link String} will be parsed through {@link ReflectTools#findClass(String, ClassLoader)}</p>
	 * 
	 * @param classLoader
	 * 		Alternative {@link ClassLoader} or <code>NULL</code> to use the default
	 * 
	 * @param parameterTypes
	 * 		Mixed {@link String} and {@link Class} objects
	 * 
	 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static Class<?>[] findParameterTypes(ClassLoader classLoader, Object... parameterTypes) {
		Class<?>[] types = new Class<?>[ parameterTypes.length ];
		
		try {
			for (int i=0; i < parameterTypes.length; i++) {
				types[i] = parameterTypes[i] instanceof String ? 
						getReflectClass((String) parameterTypes[i], classLoader).getObject() : 
							parameterTypes[i] instanceof Class ? (Class<?>) parameterTypes[i] : parameterTypes[i].getClass();
			}
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
		
		return types;
	}
	
	/**
	 * Internal class
	 */
	protected static String memberToString(String clazzName, String methodName, Class<?>... parameterTypes) {
		String output = "";
		
		for (int i=0; i < parameterTypes.length; i++) {
			if (i > 0) {
				output += ",";
			}

			output += parameterTypes[i] == null ? "NULL" : (parameterTypes[i].getCanonicalName() == null ? parameterTypes[i].getName() : parameterTypes[i].getCanonicalName());
		}
		
		return (clazzName != null ? clazzName : "") + 
					(clazzName != null ? "." : "") + 
						(methodName != null ? methodName : "Constructor") + 
							"[" + output + "]";
	}
	
	/**
	 * Internal class
	 */
	protected static String memberToString(String clazzName, String methodName, Integer parameterCount) {
		return (clazzName != null ? clazzName : "") + 
				(clazzName != null ? "." : "") + 
					(methodName != null ? methodName : "Constructor") + 
						"[" + parameterCount + "]";
	}
	
	/**
	 * <p>Get the parent/outer {@link Class} relative to @clazz</p>
	 * 
	 * @param clazz
	 * 		The child {@link Class}
	 * 
	 * @see ReflectTools.ReflectClass#getParent()
	 */
	public static Class<?> getParentClass(Class<?> clazz) {
		try {
			String clazzName = clazz.getName();
			Integer index = clazzName.lastIndexOf("$");
			
			return index > 0 ? findClass(clazzName.substring(0, index), clazz.getClassLoader()) : null;
			
		} catch (ReflectException e) {}
		
		return null;
	}
	
	/**
	 * <p>Get the parent/outer <code>Instance</code> relative to @instance</p>
	 * 
	 * @param instance
	 * 		The child <code>Instance</code>
	 * 
	 * @see ReflectTools#getParentInstance(Class, Object)
	 */
	public static Object getParentInstance(Object instance) {
		try {
			return findField(instance.getClass(), "this$0").get(instance);
			
		} 
		catch (IllegalArgumentException e) {} 
		catch (IllegalAccessException e) {} 
		catch (ReflectException e) {}
		
		return null;
	}
	
	/**
	 * <p>Get the parent/outer <code>Instance</code> relative to @instance that matches @clazz</p>
	 * 
	 * @param clazz
	 * 		The {@link Class} that it should match
	 * 
	 * @param instance
	 * 		The child <code>Instance</code>
	 * 
	 * @see ReflectTools#getParentInstance(Object)
	 */
	public static Object getParentInstance(Class<?> clazz, Object instance) {
		if (instance.getClass() != clazz) {
			Object in = instance;
			
			while ((in = getParentInstance(in)) != null) {
				if (in.getClass() == clazz) {
					return in;
				}
			}
		}
		
		return instance;
	}
	
	/**
	 * <p>Find a {@link Method} or {@link Constructor}</p>
	 * 
	 * <p>Depending on @match, this method will search both the {@link Class} itself along with it's <code>SuperClass</code>'s</p>
	 * 
	 * @param clazz
	 * 		The {@link Class} that contains the {@link Method} or {@link Constructor}
	 * 
	 * @param memberName
	 * 		Name of the {@link Method} or <code>NULL</code> to search for {@link Constructor}
	 * 
	 * @param match
	 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
	 * 
	 * @param parameterTypes
	 * 		An array of <code>Parameter Types</code>
	 * 
	 * @see ReflectTools#findMemberRecursive(Class, String, Integer, Class...)
	 * 
	 * @see ReflectTools.ReflectClass#getConstructor()
	 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethod(String)
	 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static Member findMember(Class<?> clazz, String memberName, Integer match, Class<?>... parameterTypes) {
		try {
			Throwable throwable = null;
			Member member = null;
			
			try {
				member = memberName != null ? clazz.getDeclaredMethod(memberName, parameterTypes) : clazz.getDeclaredConstructor(parameterTypes);
				
			} catch (NoSuchMethodException e) {
				Class<?> parentClazz = clazz;
				throwable = e;
				
				do {
					if (match == MEMBER_MATCH_EXACT && memberName != null && (parentClazz = parentClazz.getSuperclass()) != null) {
						try {
							member = parentClazz.getDeclaredMethod(memberName, parameterTypes);
							
						} catch (NoSuchMethodException ei) {}
						
					} else if (match != MEMBER_MATCH_EXACT) {
						for (int i=0; i < (memberName != null ? 2 : 1); i++) {
							Member[] members = memberName != null ? 
									((i == 0) ? parentClazz.getDeclaredMethods() : parentClazz.getMethods()) : 
										parentClazz.getDeclaredConstructors();
									
							for (int x=0; x < members.length; x++) {
								if ((memberName != null && members[x].getName().equals(memberName) && Common.ClassUtils.isAssignable(parameterTypes, ((Method) members[x]).getParameterTypes(), true)) || 
										(memberName == null && Common.ClassUtils.isAssignable(parameterTypes, ((Constructor<?>) members[i]).getParameterTypes(), true))) {
								
									if (member == null ||
											(memberName != null && Common.MemberUtils.compareParameterTypes(((Method) members[x]).getParameterTypes(), ((Method) member).getParameterTypes(), parameterTypes) < 0) ||
											(memberName == null && Common.MemberUtils.compareParameterTypes(((Constructor<?>) members[x]).getParameterTypes(), ((Constructor<?>) member).getParameterTypes(), parameterTypes) < 0)) {
										
										member = members[x];
									}
								}
							}
						}
					}
					
				} while (memberName != null && member == null && !parentClazz.equals(parentClazz.getSuperclass()) && (parentClazz = parentClazz.getSuperclass()) != null);
			}
			
			if (member != null) {
				((AccessibleObject) member).setAccessible(true);

				return member;
				
			} else {
				throw new ReflectException("NoSuchMethodException: " + memberToString(clazz.getName(), memberName, parameterTypes), throwable);
			}
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	/**
	 * <p>Find a {@link Method} or {@link Constructor}</p>
	 * 
	 * <p>Unlike {@link ReflectTools#findMember(Class, String, Integer, Class...)} this method will also search all parent/outer classes</p>
	 * 
	 * @param clazz
	 * 		The {@link Class} that contains the {@link Method} or {@link Constructor}
	 * 
	 * @param memberName
	 * 		Name of the {@link Method} or <code>NULL</code> to search for {@link Constructor}
	 * 
	 * @param match
	 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
	 * 
	 * @param parameterTypes
	 * 		An array of <code>Parameter Types</code>
	 * 
	 * @see ReflectTools#findMember(Class, String, Integer, Class...)
	 *
	 * @see ReflectTools.ReflectClass#locateMethod(String)
	 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static Member findMemberRecursive(Class<?> clazz, String memberName, Integer match, Class<?>... parameterTypes) {
		Class<?> outerClazz = clazz;
		Member member = null;
		Throwable throwable = null;
		
		do {
			try {
				member = findMember(outerClazz, memberName, match, parameterTypes);
				
			} catch (ReflectException e) { if (throwable == null) throwable = e; }
			
		} while (member == null && (outerClazz = getParentClass(outerClazz)) != null);
		
		if (member == null) {
			throw new ReflectException(throwable.getMessage(), throwable);
		}
		
		return member;
	}
	
	/**
	 * <p>Find a {@link Field}</p>
	 * 
	 * <p>This method will search both the {@link Class} itself along with it's <code>SuperClass</code>'s</p>
	 * 
	 * @param clazz
	 * 		The {@link Class} that contains the {@link Field}
	 * 
	 * @param fieldName
	 * 		The name of the {@link Field}
	 * 
	 * @see ReflectTools#findFieldRecursive(Class, String)
	 * @see ReflectTools.ReflectClass#getField(String)
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static Field findField(Class<?> clazz, String fieldName) {
		try {
			Class<?> parentClazz = clazz;
			Field field = null;
			Throwable throwable = null;
			
			do {
				try {
					field = parentClazz.getDeclaredField(fieldName);
					
				} catch (NoSuchFieldException e) { if (throwable == null) throwable = e; }
				
			} while (field == null && !parentClazz.equals(parentClazz.getSuperclass()) && (parentClazz = parentClazz.getSuperclass()) != null);
			
			if (field != null) {
				field.setAccessible(true);
				
				return field;
				
			} else {
				throw new ReflectException("NoSuchFieldException: " + clazz.getName() + "." + fieldName, throwable);
			}
			
		} catch (ReflectException e) {
			throw new ReflectException(e.getMessage(), e);
		}
	}
	
	/**
	 * <p>Find a {@link Field}</p>
	 * 
	 * <p>Unlike {@link ReflectTools#findField(Class, String)} this method will also search all parent/outer classes</p>
	 * 
	 * @param clazz
	 * 		The {@link Class} that contains the {@link Field}
	 * 
	 * @param fieldName
	 * 		The name of the {@link Field}
	 * 
	 * @see ReflectTools#findField(Class, String)
	 * @see ReflectTools.ReflectClass#locateField(String)
	 * 
	 * @throws ReflectTools.ReflectException
	 */
	public static Field findFieldRecursive(Class<?> clazz, String fieldName) {
		Class<?> outerClass = clazz;
		Field field = null;
		Throwable throwable = null;
		
		do {
			try {
				field = findField(outerClass, fieldName);
				
			} catch (ReflectException e) { if (throwable == null) throwable = e; }
			
		} while (field == null && (outerClass = getParentClass(outerClass)) != null);
		
		if (field == null) {
			throw new ReflectException(throwable.getMessage(), throwable);
		}
		
		return field;
	}

	public static class ReflectException extends Error {
		private static final long serialVersionUID = 744756325699271227L;
		
		protected ReflectException(Throwable cause) { super(cause); }
		protected ReflectException(String detailMessage, Throwable cause) { super(detailMessage, cause); }
	}
	
	/**
	 * <p>This is an abstract {@link Class} that is used to add shared functionality to {@link ReflectTools.ReflectConstructor} and {@link ReflectTools.ReflectMethod}</p>
	 * 
	 * @see ReflectTools.ReflectConstructor
	 * @see ReflectTools.ReflectMethod
	 */
	public static abstract class ReflectMember {
		/**
		 * <p>Invoke this {@link Member}</p>
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public abstract Object invoke();
		
		/**
		 * <p>Invoke this {@link Member}</p>
		 * 
		 * @param original
		 * 		Whether or not to invoke the original (If hooked by XposedBridge)
		 * 
		 * @param args
		 * 		The <code>Arguments</code> that will be parsed to the {@link Member}
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public abstract Object invoke(Boolean original, Object... args);
		
		/**
		 * <p>Get the real {@link Member} object</p>
		 */
		public abstract Member getObject();
		
		/**
		 * <p>Inject an <code>XC_MethodHook</code> into this {@link Member}</p>
		 * 
		 * @param hook
		 * 		The <code>XC_MethodHook</code> to be used as the hook
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public abstract void inject(Object hook);
	}
	
	/**
	 * <p>This is a {@link Class} wrapper that adds additional Reflection Tools to any {@link Method} object</p>
	 * 
	 * @see ReflectTools.ReflectClass#getMethod()
	 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
	 * 
	 * @see ReflectTools.ReflectClass#locateMethod(String)
	 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
	 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
	 */
	public static class ReflectMethod extends ReflectMember {
		protected final Member mMember;
		
		protected ReflectMethod(Member member) {
			mMember = member;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Member getObject() {
			return mMember;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object invoke() {
			try {
				return invoke(false);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object invoke(Boolean original, Object... args) {
			try {
				return invoke(null, original, args);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Invoke this {@link Member}</p>
		 * 
		 * @param instance
		 * 		The <code>Instance</code> to invoke
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Object invoke(Object instance) {
			try {
				return invoke(instance, false);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}

		/**
		 * <p>Invoke this {@link Member}</p>
		 * 
		 * @param instance
		 * 		The <code>Instance</code> to invoke
		 * 
		 * @param original
		 * 		Whether or not to invoke the original (If hooked by XposedBridge)
		 * 
		 * @param
		 * 		The <code>Arguments</code> that will be parsed to the {@link Member}
		 * 
		 * @param args
		 * 		The <code>Arguments</code> that will be parsed to the {@link Member}
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Object invoke(Object instance, Boolean original, Object... args) {
			try {
				if (original) {
					ReflectClass reflectClazz = getReflectClass("de.robv.android.xposed.XposedBridge", mMember.getDeclaringClass().getClassLoader());
					ReflectMethod reflectMethod = reflectClazz.getMethod("invokeOriginalMethod", MEMBER_MATCH_FAST, Member.class, Object.class, Object[].class);
					
					return ((Method) reflectMethod.getObject()).invoke(null, mMember, Modifier.isStatic(((Method) mMember).getModifiers()) ? null : getParentInstance(mMember.getDeclaringClass(), instance), args);
					
				} else {
					return ((Method) mMember).invoke(Modifier.isStatic(((Method) mMember).getModifiers()) ? null : getParentInstance(mMember.getDeclaringClass(), instance), args);
				}
				
			} catch (IllegalArgumentException e) {
				throw new ReflectException("IllegalArgumentException: " + memberToString(mMember.getDeclaringClass().getName(), mMember.getName(), ((Method) mMember).getParameterTypes()), e);
				
			} catch (IllegalAccessException e) {
				throw new ReflectException("IllegalAccessException: " + memberToString(mMember.getDeclaringClass().getName(), mMember.getName(), ((Method) mMember).getParameterTypes()), e);
				
			} catch (InvocationTargetException e) {
				throw new ReflectException("InvocationTargetException: " + memberToString(mMember.getDeclaringClass().getName(), mMember.getName(), ((Method) mMember).getParameterTypes()), e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void inject(Object hook) {
			try {
				ReflectClass reflectClazz = getReflectClass("de.robv.android.xposed.XposedBridge", mMember.getDeclaringClass().getClassLoader());
				ReflectMethod reflectMethod = reflectClazz.getMethod("hookMethod", MEMBER_MATCH_FAST, Member.class, "de.robv.android.xposed.XC_MethodHook");
				
				reflectMethod.invoke(false, mMember, hook);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * <p>This is a {@link Class} wrapper that adds additional Reflection Tools to any {@link Constructor} object</p>
	 * 
	 * @see ReflectTools.ReflectClass#getConstructor()
	 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
	 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
	 */
	public static class ReflectConstructor extends ReflectMember {
		protected final Member mMember;
		
		protected ReflectConstructor(Member member) {
			mMember = member;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Member getObject() {
			return mMember;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object invoke() {
			try {
				return invoke(false);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object invoke(Boolean original, Object... args) {
			try {
				if (original) {
					ReflectClass reflectClazz = getReflectClass("de.robv.android.xposed.XposedBridge", mMember.getDeclaringClass().getClassLoader());
					ReflectMethod reflectMethod = reflectClazz.getMethod("invokeOriginalMethod", MEMBER_MATCH_FAST, Member.class, Object.class, Object[].class);
					
					return ((Method) reflectMethod.getObject()).invoke(null, mMember, null, args);
					
				} else {
					return ((Constructor<?>) mMember).newInstance(args);
				}
				
			} catch (IllegalArgumentException e) {
				throw new ReflectException("IllegalArgumentException: " + memberToString(mMember.getDeclaringClass().getName(), null, ((Constructor<?>) mMember).getParameterTypes()), e);
				
			} catch (IllegalAccessException e) {
				throw new ReflectException("IllegalAccessException: " + memberToString(mMember.getDeclaringClass().getName(), null, ((Constructor<?>) mMember).getParameterTypes()), e);
				
			} catch (InvocationTargetException e) {
				throw new ReflectException("InvocationTargetException: " + memberToString(mMember.getDeclaringClass().getName(), null, ((Constructor<?>) mMember).getParameterTypes()), e);
				
			} catch (InstantiationException e) {
				throw new ReflectException("InstantiationException: " + memberToString(mMember.getDeclaringClass().getName(), null, ((Constructor<?>) mMember).getParameterTypes()), e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void inject(Object hook) {
			try {
				ReflectClass reflectClazz = getReflectClass("de.robv.android.xposed.XposedBridge", mMember.getDeclaringClass().getClassLoader());
				ReflectMethod reflectMethod = reflectClazz.getMethod("hookMethod", MEMBER_MATCH_FAST, Member.class, "de.robv.android.xposed.XC_MethodHook");
				
				reflectMethod.invoke(false, mMember, hook);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * <p>This is a {@link Class} wrapper that adds additional Reflection Tools to any {@link Field} object</p>
	 * 
	 * @see ReflectTools.ReflectClass#getField(String)
	 * @see ReflectTools.ReflectClass#locateField(String)
	 */
	public static class ReflectField {
		protected final Field mField;
		
		protected ReflectField(Field field) {
			mField = field;
		}
		
		/**
		 * <p>Get the real {@link Field} object</p>
		 */
		public Field getObject() {
			return mField;
		}
		
		/**
		 * <p>Change the value of a {@link Object} {@link Field}</p>
		 * 
		 * @param instance
		 * 		The <code>Instance</code> to access
		 * 
		 * @param value
		 * 		The new value for the {@link Field}
		 * 
		 * @see ReflectTools.ReflectField#set(Object)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Boolean set(Object instance, Object value) {
			try {
				mField.set(Modifier.isStatic(mField.getModifiers()) ? null : getParentInstance(mField.getDeclaringClass(), instance), value); return true;
				
			} catch (IllegalArgumentException e) {
				throw new ReflectException("IllegalArgumentException: " + mField.getDeclaringClass().getName() + "." + mField.getName(), e);
				
			} catch (IllegalAccessException e) {
				throw new ReflectException("IllegalAccessException: " + mField.getDeclaringClass().getName() + "." + mField.getName(), e);
			}
		}
		
		/**
		 * <p>Change the value of a <code>Static</code> {@link Field}</p>
		 * 
		 * @param value
		 * 		The new value for the {@link Field}
		 * 
		 * @see ReflectTools.ReflectField#set(Object, Object)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Boolean set(Object value) {
			try {
				return set(null, value);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Get the value of a {@link Object} {@link Field}</p>
		 * 
		 * @param instance
		 * 		The <code>Instance</code> to access
		 * 
		 * @see ReflectTools.ReflectField#get()
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Object get(Object instance) {
			try {
				return mField.get(Modifier.isStatic(mField.getModifiers()) ? null : getParentInstance(mField.getDeclaringClass(), instance));
				
			} catch (IllegalArgumentException e) {
				throw new ReflectException("IllegalArgumentException: " + mField.getDeclaringClass().getName() + "." + mField.getName(), e);
				
			} catch (IllegalAccessException e) {
				throw new ReflectException("IllegalAccessException: " + mField.getDeclaringClass().getName() + "." + mField.getName(), e);
			}
		}
		
		/**
		 * <p>Get the value of a <code>Static</code> {@link Field}</p>
		 * 
		 * @see ReflectTools.ReflectField#get(Object)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Object get() {
			try {
				return get(null);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * <p>This is a {@link Class} wrapper that adds additional Reflection Tools to any {@link Class} object</p>
	 * 
	 * <p>Also this wrapper will cache all {@link Method}'s and {@link Field}'s that is located within this class or it's parent's</p>
	 * 
	 * @see ReflectTools#getReflectClass(String, ClassLoader)
	 * @see ReflectTools#getReflectClass(Object)
	 */
	public static class ReflectClass {
		protected final Class<?> mClazz;
		protected final HashMap<String, ReflectMember> oMemberCache = new HashMap<String, ReflectMember>();
		protected final HashMap<String, ReflectField> oFieldCache = new HashMap<String, ReflectField>();
		
		protected ReflectClass(Class<?> clazz) {
			mClazz = clazz;
		}
		
		/**
		 * <p>Get a new {@link ReflectTools.ReflectClass} <code>Instance</code> of the <code>Parent Class</code></p>
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectClass getParent() {
			try {
				String clazzName = mClazz.getName();
				Integer index = clazzName.lastIndexOf("$");
				
				return index > 0 ? getReflectClass(clazzName.substring(0, index), mClazz.getClassLoader()) : null;
				
			} catch (ReflectException e) {}
			
			return null;
		}
		
		/**
		 * <p>Get a new {@link ReflectTools.ReflectClass} <code>Instance</code> of the <code>Super Class</code></p>
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectClass getSuper() {
			try {
				Class<?> superClass = mClazz.getSuperclass();
				
				if (superClass != null) {
					return getReflectClass(superClass.getName(), superClass.getClassLoader());
				}
				
			} catch (ReflectException e) {}
			
			return null;
		}
		
		/**
		 * <p>Get the real {@link Class} object</p>
		 */
		public Class<?> getObject() {
			return mClazz;
		}
		
		/**
		 * <p>Get a {@link Field} in the current {@link Class} or it's <code>Super Class</code>'s</p>
		 * 
		 * @param fieldName
		 * 		Name of the {@link Field}
		 * 
		 * @see ReflectTools.ReflectClass#locateField(String)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectField getField(String fieldName) {
			if (!oFieldCache.containsKey(fieldName)) {
				try {
					oFieldCache.put(fieldName, new ReflectField(findField(mClazz, fieldName)));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return oFieldCache.get(fieldName);
		}
		
		/**
		 * <p>Search for a {@link Field} in the current {@link Class} or it's <code>Super Class</code>'s and <code>Parent Class</code>'s</p>
		 * 
		 * @param fieldName
		 * 		Name of the {@link Field}
		 * 
		 * @see ReflectTools.ReflectClass#getField(String)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectField locateField(String fieldName) {
			String cacheName = fieldName + "#R";
					
			if (!oFieldCache.containsKey(cacheName)) {
				try {
					oFieldCache.put(cacheName, new ReflectField(findFieldRecursive(mClazz, fieldName)));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return oFieldCache.get(cacheName);
		}

		/**
		 * <p>Locate a {@link Constructor} based on <code>Arguments</code></p>
		 * 
		 * <p>This method will search both the current {@link Class} and it's <code>Super Class</code>'s</p>
		 * 
		 * @param match
		 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
		 * 
		 * @param args
		 * 		An array of <code>Arguments</code> that will be converted into <code>Parameter Types</code>
		 * 
		 * @see ReflectTools.ReflectClass#getConstructor()
		 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectConstructor getConstructorByArgs(Integer match, Object... args) {
			String cacheName = match < 0 ? 
					memberToString(mClazz.getName(), null, args.length) : 
						memberToString(mClazz.getName(), null, convertArgsToTypes(args));
			
			if (!oMemberCache.containsKey(cacheName)) {
				try {
					oMemberCache.put(cacheName, new ReflectConstructor(findMember(mClazz, null, match, convertArgsToTypes(args))));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return (ReflectConstructor) oMemberCache.get(cacheName);
		}
		
		/**
		 * <p>Locate a {@link Constructor} with no <code>Arguments</code></p>
		 * 
		 * <p>This method will search both the current {@link Class} and it's <code>Super Class</code>'s</p>
		 * 
		 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
		 * @see ReflectTools.ReflectClass#getConstructor(Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectConstructor getConstructor() {
			try {
				return getConstructor(MEMBER_MATCH_FAST);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Locate a {@link Constructor} based on <code>Parameter Types</code></p>
		 * 
		 * <p>This method will search both the current {@link Class} and it's <code>Super Class</code>'s</p>
		 * 
		 * @param match
		 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
		 * 
		 * @param args
		 * 		An array of <code>Parameter Types</code> as {@link String} or {@link Class}
		 * 
		 * @see ReflectTools.ReflectClass#getConstructor()
		 * @see ReflectTools.ReflectClass#getConstructorByArgs(Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectConstructor getConstructor(Integer match, Object... parameterTypes) {
			String cacheName = match < 0 ? 
					memberToString(mClazz.getName(), null, parameterTypes.length) : 
						memberToString(mClazz.getName(), null, parameterTypes instanceof Class<?>[] ? (Class<?>[]) parameterTypes : findParameterTypes(mClazz.getClassLoader(), parameterTypes));
			
			if (!oMemberCache.containsKey(cacheName)) {
				try {
					oMemberCache.put(cacheName, new ReflectConstructor(findMember(mClazz, null, match, findParameterTypes(mClazz.getClassLoader(), parameterTypes))));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return (ReflectConstructor) oMemberCache.get(cacheName);
		}
		
		/**
		 * <p>Locate a {@link Method} based on <code>Arguments</code></p>
		 * 
		 * <p>This method will search both the current {@link Class} and it's <code>Super Class</code>'s</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}
		 * 
		 * @param match
		 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
		 * 
		 * @param args
		 * 		An array of <code>Arguments</code> that will be converted into <code>Parameter Types</code>
		 * 
		 * @see ReflectTools.ReflectClass#getMethod(String)
		 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
		 * 
		 * @see ReflectTools.ReflectClass#locateMethod(String)
		 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectMethod getMethodByArgs(String methodName, Integer match, Object... args) {
			String cacheName = match < 0 ? 
					memberToString(mClazz.getName(), methodName, args.length) : 
						memberToString(mClazz.getName(), methodName, convertArgsToTypes(args));
			
			if (!oMemberCache.containsKey(cacheName)) {
				try {
					oMemberCache.put(cacheName, new ReflectMethod(findMember(mClazz, methodName, match, convertArgsToTypes(args))));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return (ReflectMethod) oMemberCache.get(cacheName);
		}
		
		/**
		 * <p>Locate a {@link Method} with no <code>Arguments</code></p>
		 * 
		 * <p>This method will search both the current {@link Class} and it's <code>Super Class</code>'s</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}
		 * 
		 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
		 * 
		 * @see ReflectTools.ReflectClass#locateMethod(String)
		 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectMethod getMethod(String methodName) {
			try {
				return getMethod(methodName, MEMBER_MATCH_FAST);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Locate a {@link Method} based on <code>Parameter Types</code></p>
		 * 
		 * <p>This method will search both the current {@link Class} and it's <code>Super Class</code>'s</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}
		 * 
		 * @param match
		 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
		 * 
		 * @param args
		 * 		An array of <code>Parameter Types</code> as {@link String} or {@link Class}
		 * 
		 * @see ReflectTools.ReflectClass#getMethod(String)
		 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
		 * 
		 * @see ReflectTools.ReflectClass#locateMethod(String)
		 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectMethod getMethod(String methodName, Integer match, Object... parameterTypes) {
			String cacheName = match < 0 ? 
					memberToString(mClazz.getName(), methodName, parameterTypes.length) : 
						memberToString(mClazz.getName(), methodName, parameterTypes instanceof Class<?>[] ? (Class<?>[]) parameterTypes : findParameterTypes(mClazz.getClassLoader(), parameterTypes));
			
			if (!oMemberCache.containsKey(cacheName)) {
				try {
					oMemberCache.put(cacheName, new ReflectMethod(findMember(mClazz, methodName, match, findParameterTypes(mClazz.getClassLoader(), parameterTypes))));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return (ReflectMethod) oMemberCache.get(cacheName);
		}

		/**
		 * <p>Locate a {@link Method} based on <code>Arguments</code></p>
		 * 
		 * <p>Unlike {@link ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)} this also searches in <Code>Parent Class</code>'s</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}
		 * 
		 * @param match
		 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
		 * 
		 * @param args
		 * 		An array of <code>Arguments</code> that will be converted into <code>Parameter Types</code>
		 * 
		 * @see ReflectTools.ReflectClass#locateMethod(String)
		 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
		 * 
		 * @see ReflectTools.ReflectClass#getMethod(String)
		 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectMethod locateMethodByArgs(String methodName, Integer match, Object... args) {
			String cacheName = match < 0 ? 
					memberToString(mClazz.getName(), methodName, args.length) + "#R" : 
						memberToString(mClazz.getName(), methodName, convertArgsToTypes(args)) + "#R";
			
			if (!oMemberCache.containsKey(cacheName)) {
				try {
					oMemberCache.put(cacheName, new ReflectMethod(findMemberRecursive(mClazz, methodName, match, convertArgsToTypes(args))));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return (ReflectMethod) oMemberCache.get(cacheName);
		}
		
		/**
		 * <p>Locate a {@link Method} with no <code>Arguments</code></p>
		 * 
		 * <p>Unlike {@link ReflectTools.ReflectClass#getMethodByArgs(String)} this also searches in <Code>Parent Class</code>'s</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}
		 * 
		 * @see ReflectTools.ReflectClass#locateMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
		 * 
		 * @see ReflectTools.ReflectClass#getMethod(String)
		 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectMethod locateMethod(String methodName) {
			try {
				return locateMethod(methodName, MEMBER_MATCH_FAST);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Locate a {@link Method} based on <code>Parameter Types</code></p>
		 * 
		 * <p>Unlike {@link ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object)} this also searches in <Code>Parent Class</code>'s</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}
		 * 
		 * @param match
		 * 		{@link ReflectTools#MEMBER_MATCH_BEST}, {@link ReflectTools#MEMBER_MATCH_EXACT} or {@link ReflectTools#MEMBER_MATCH_FAST}
		 * 
		 * @param args
		 * 		An array of <code>Parameter Types</code> as {@link String} or {@link Class}
		 * 
		 * @see ReflectTools.ReflectClass#locateMethod(String)
		 * @see ReflectTools.ReflectClass#locateMethodByArgs(String, Integer, Object...)
		 * 
		 * @see ReflectTools.ReflectClass#getMethod(String)
		 * @see ReflectTools.ReflectClass#getMethod(String, Integer, Object...)
		 * @see ReflectTools.ReflectClass#getMethodByArgs(String, Integer, Object...)
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public ReflectMethod locateMethod(String methodName, Integer match, Object... parameterTypes) {
			String cacheName = match < 0 ? 
					memberToString(mClazz.getName(), methodName, parameterTypes.length) + "#R" : 
						memberToString(mClazz.getName(), methodName, parameterTypes instanceof Class<?>[] ? (Class<?>[]) parameterTypes : findParameterTypes(mClazz.getClassLoader(), parameterTypes)) + "#R";
			
			if (!oMemberCache.containsKey(cacheName)) {
				try {
					oMemberCache.put(cacheName, new ReflectMethod(findMemberRecursive(mClazz, methodName, match, findParameterTypes(mClazz.getClassLoader(), parameterTypes))));
					
				} catch (ReflectException e) {
					throw new ReflectException(e.getMessage(), e);
				}
			}
			
			return (ReflectMethod) oMemberCache.get(cacheName);
		}
		
		/**
		 * <p>Inject an <code>XC_MethodHook</code> into all {@link Method}'s whose name matches @methodName</p>
		 * 
		 * @param methodName
		 * 		Name of the {@link Method}'s to inject
		 * 
		 * @param hook
		 * 		The <code>XC_MethodHook</code> to be used as the hook
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Integer inject(String methodName, Object hook) {
			try {
				ReflectClass reflectClazz = getReflectClass("de.robv.android.xposed.XposedBridge", mClazz.getClassLoader());
				ReflectMethod reflectMethod = reflectClazz.getMethod("hookMethod", MEMBER_MATCH_FAST, Member.class, "de.robv.android.xposed.XC_MethodHook");
				Member[] members = methodName != null ? mClazz.getDeclaredMethods() : mClazz.getDeclaredConstructors();
				Integer count = 0;
				
				for (Member member : members) {
					if (methodName == null || member.getName().equals(methodName)) {
						reflectMethod.invoke(false, member, hook); count++;
					}
				}
				
				return count;
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}

		/**
		 * <p>Inject an <code>XC_MethodHook</code> into all {@link Constructor}'s</p>
		 * 
		 * @param hook
		 * 		The <code>XC_MethodHook</code> to be used as the hook
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Integer inject(Object hook) {
			try {
				return inject(null, hook);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Invoke the {@link Constructor} with no <code>Arguments</code></p>
		 * 
		 * @throws ReflectTools.ReflectException
		 */
		public Object invoke() {
			try {
				return getConstructor(MEMBER_MATCH_BEST).invoke(false);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
		
		/**
		 * <p>Invoke a {@link Constructor} matching the <code>Arguments</code></p>
		 * 
		 * @param original
		 * 		Whether or not to invoke the original (If hooked by XposedBridge)
		 * 
		 * @param args
		 * 		The <code>Arguments</code> that will be parsed to the {@link Constructor}

		 * @throws ReflectTools.ReflectException
		 */
		public Object invoke(Boolean original, Object... args) {
			try {
				return getConstructorByArgs(MEMBER_MATCH_BEST, args).invoke(original, args);
				
			} catch (ReflectException e) {
				throw new ReflectException(e.getMessage(), e);
			}
		}
	}
}
