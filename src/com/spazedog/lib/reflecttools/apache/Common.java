/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ------------------------------------------------------------------
 * This is a fragment collection from the Apache Common Library.
 * It only contains what is needed by the RelfetcTools library and nothing more. 
 */

package com.spazedog.lib.reflecttools.apache;

import java.util.HashMap;
import java.util.Map;

public class Common {
	public static class ClassUtils {
	    /**
	     * Maps primitive {@code Class}es to their corresponding wrapper {@code Class}.
	     */
	    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<Class<?>, Class<?>>();
	    static {
	         primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
	         primitiveWrapperMap.put(Byte.TYPE, Byte.class);
	         primitiveWrapperMap.put(Character.TYPE, Character.class);
	         primitiveWrapperMap.put(Short.TYPE, Short.class);
	         primitiveWrapperMap.put(Integer.TYPE, Integer.class);
	         primitiveWrapperMap.put(Long.TYPE, Long.class);
	         primitiveWrapperMap.put(Double.TYPE, Double.class);
	         primitiveWrapperMap.put(Float.TYPE, Float.class);
	         primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
	    }

	    /**
	     * Maps wrapper {@code Class}es to their corresponding primitive types.
	     */
	    private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<Class<?>, Class<?>>();
	    static {
	        for (Class<?> primitiveClass : primitiveWrapperMap.keySet()) {
	            Class<?> wrapperClass = primitiveWrapperMap.get(primitiveClass);
	            if (!primitiveClass.equals(wrapperClass)) {
	                wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
	            }
	        }
	    }
		
	    /**
	     * <p>Converts the specified wrapper class to its corresponding primitive
	     * class.</p>
	     *
	     * <p>This method is the counter part of {@code primitiveToWrapper()}.
	     * If the passed in class is a wrapper class for a primitive type, this
	     * primitive type will be returned (e.g. {@code Integer.TYPE} for
	     * {@code Integer.class}). For other classes, or if the parameter is
	     * <b>null</b>, the return value is <b>null</b>.</p>
	     *
	     * @param cls the class to convert, may be <b>null</b>
	     * @return the corresponding primitive type if {@code cls} is a
	     * wrapper class, <b>null</b> otherwise
	     * @see #primitiveToWrapper(Class)
	     * @since 2.4
	     */
	    public static Class<?> wrapperToPrimitive(Class<?> cls) {
	        return wrapperPrimitiveMap.get(cls);
	    }
	    
	    /**
	     * <p>Checks if an array of Classes can be assigned to another array of Classes.</p>
	     *
	     * <p>This method calls {@link #isAssignable(Class, Class) isAssignable} for each
	     * Class pair in the input arrays. It can be used to check if a set of arguments
	     * (the first parameter) are suitably compatible with a set of method parameter types
	     * (the second parameter).</p>
	     *
	     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method, this
	     * method takes into account widenings of primitive classes and
	     * {@code null}s.</p>
	     *
	     * <p>Primitive widenings allow an int to be assigned to a {@code long},
	     * {@code float} or {@code double}. This method returns the correct
	     * result for these cases.</p>
	     *
	     * <p>{@code Null} may be assigned to any reference type. This method will
	     * return {@code true} if {@code null} is passed in and the toClass is
	     * non-primitive.</p>
	     *
	     * <p>Specifically, this method tests whether the type represented by the
	     * specified {@code Class} parameter can be converted to the type
	     * represented by this {@code Class} object via an identity conversion
	     * widening primitive or widening reference conversion. See
	     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
	     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
	     *
	     * <p><strong>Since Lang 3.0,</strong> this method will default behavior for
	     * calculating assignability between primitive and wrapper types <em>corresponding
	     * to the running Java version</em>; i.e. autoboxing will be the default
	     * behavior in VMs running Java versions >= 1.5.</p>
	     *
	     * @param classArray  the array of Classes to check, may be {@code null}
	     * @param toClassArray  the array of Classes to try to assign into, may be {@code null}
	     * @return {@code true} if assignment possible
	     */
	    public static boolean isAssignable(Class<?>[] classArray, Class<?>... toClassArray) {
	        return isAssignable(classArray, toClassArray, true);
	    }

	    /**
	     * <p>Checks if an array of Classes can be assigned to another array of Classes.</p>
	     *
	     * <p>This method calls {@link #isAssignable(Class, Class) isAssignable} for each
	     * Class pair in the input arrays. It can be used to check if a set of arguments
	     * (the first parameter) are suitably compatible with a set of method parameter types
	     * (the second parameter).</p>
	     *
	     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method, this
	     * method takes into account widenings of primitive classes and
	     * {@code null}s.</p>
	     *
	     * <p>Primitive widenings allow an int to be assigned to a {@code long},
	     * {@code float} or {@code double}. This method returns the correct
	     * result for these cases.</p>
	     *
	     * <p>{@code Null} may be assigned to any reference type. This method will
	     * return {@code true} if {@code null} is passed in and the toClass is
	     * non-primitive.</p>
	     *
	     * <p>Specifically, this method tests whether the type represented by the
	     * specified {@code Class} parameter can be converted to the type
	     * represented by this {@code Class} object via an identity conversion
	     * widening primitive or widening reference conversion. See
	     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
	     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
	     *
	     * @param classArray  the array of Classes to check, may be {@code null}
	     * @param toClassArray  the array of Classes to try to assign into, may be {@code null}
	     * @param autoboxing  whether to use implicit autoboxing/unboxing between primitives and wrappers
	     * @return {@code true} if assignment possible
	     */
	    public static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray, boolean autoboxing) {
	        if (ArrayUtils.isSameLength(classArray, toClassArray) == false) {
	            return false;
	        }
	        if (classArray == null) {
	            classArray = ArrayUtils.EMPTY_CLASS_ARRAY;
	        }
	        if (toClassArray == null) {
	            toClassArray = ArrayUtils.EMPTY_CLASS_ARRAY;
	        }
	        for (int i = 0; i < classArray.length; i++) {
	            if (isAssignable(classArray[i], toClassArray[i], autoboxing) == false) {
	                return false;
	            }
	        }
	        return true;
	    }
	    
	    /**
	     * <p>Checks if one {@code Class} can be assigned to a variable of
	     * another {@code Class}.</p>
	     *
	     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
	     * this method takes into account widenings of primitive classes and
	     * {@code null}s.</p>
	     *
	     * <p>Primitive widenings allow an int to be assigned to a long, float or
	     * double. This method returns the correct result for these cases.</p>
	     *
	     * <p>{@code Null} may be assigned to any reference type. This method
	     * will return {@code true} if {@code null} is passed in and the
	     * toClass is non-primitive.</p>
	     *
	     * <p>Specifically, this method tests whether the type represented by the
	     * specified {@code Class} parameter can be converted to the type
	     * represented by this {@code Class} object via an identity conversion
	     * widening primitive or widening reference conversion. See
	     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
	     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
	     *
	     * <p><strong>Since Lang 3.0,</strong> this method will default behavior for
	     * calculating assignability between primitive and wrapper types <em>corresponding
	     * to the running Java version</em>; i.e. autoboxing will be the default
	     * behavior in VMs running Java versions >= 1.5.</p>
	     *
	     * @param cls  the Class to check, may be null
	     * @param toClass  the Class to try to assign into, returns false if null
	     * @return {@code true} if assignment possible
	     */
	    public static boolean isAssignable(Class<?> cls, Class<?> toClass) {
	        return isAssignable(cls, toClass, true);
	    }

	    /**
	     * <p>Checks if one {@code Class} can be assigned to a variable of
	     * another {@code Class}.</p>
	     *
	     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
	     * this method takes into account widenings of primitive classes and
	     * {@code null}s.</p>
	     *
	     * <p>Primitive widenings allow an int to be assigned to a long, float or
	     * double. This method returns the correct result for these cases.</p>
	     *
	     * <p>{@code Null} may be assigned to any reference type. This method
	     * will return {@code true} if {@code null} is passed in and the
	     * toClass is non-primitive.</p>
	     *
	     * <p>Specifically, this method tests whether the type represented by the
	     * specified {@code Class} parameter can be converted to the type
	     * represented by this {@code Class} object via an identity conversion
	     * widening primitive or widening reference conversion. See
	     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
	     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
	     *
	     * @param cls  the Class to check, may be null
	     * @param toClass  the Class to try to assign into, returns false if null
	     * @param autoboxing  whether to use implicit autoboxing/unboxing between primitives and wrappers
	     * @return {@code true} if assignment possible
	     */
	    public static boolean isAssignable(Class<?> cls, Class<?> toClass, boolean autoboxing) {
	        if (toClass == null) {
	            return false;
	        }
	        // have to check for null, as isAssignableFrom doesn't
	        if (cls == null) {
	            return !toClass.isPrimitive();
	        }
	        //autoboxing:
	        if (autoboxing) {
	            if (cls.isPrimitive() && !toClass.isPrimitive()) {
	                cls = primitiveToWrapper(cls);
	                if (cls == null) {
	                    return false;
	                }
	            }
	            if (toClass.isPrimitive() && !cls.isPrimitive()) {
	                cls = wrapperToPrimitive(cls);
	                if (cls == null) {
	                    return false;
	                }
	            }
	        }
	        if (cls.equals(toClass)) {
	            return true;
	        }
	        if (cls.isPrimitive()) {
	            if (toClass.isPrimitive() == false) {
	                return false;
	            }
	            if (Integer.TYPE.equals(cls)) {
	                return Long.TYPE.equals(toClass)
	                    || Float.TYPE.equals(toClass)
	                    || Double.TYPE.equals(toClass);
	            }
	            if (Long.TYPE.equals(cls)) {
	                return Float.TYPE.equals(toClass)
	                    || Double.TYPE.equals(toClass);
	            }
	            if (Boolean.TYPE.equals(cls)) {
	                return false;
	            }
	            if (Double.TYPE.equals(cls)) {
	                return false;
	            }
	            if (Float.TYPE.equals(cls)) {
	                return Double.TYPE.equals(toClass);
	            }
	            if (Character.TYPE.equals(cls)) {
	                return Integer.TYPE.equals(toClass)
	                    || Long.TYPE.equals(toClass)
	                    || Float.TYPE.equals(toClass)
	                    || Double.TYPE.equals(toClass);
	            }
	            if (Short.TYPE.equals(cls)) {
	                return Integer.TYPE.equals(toClass)
	                    || Long.TYPE.equals(toClass)
	                    || Float.TYPE.equals(toClass)
	                    || Double.TYPE.equals(toClass);
	            }
	            if (Byte.TYPE.equals(cls)) {
	                return Short.TYPE.equals(toClass)
	                    || Integer.TYPE.equals(toClass)
	                    || Long.TYPE.equals(toClass)
	                    || Float.TYPE.equals(toClass)
	                    || Double.TYPE.equals(toClass);
	            }
	            // should never get here
	            return false;
	        }
	        return toClass.isAssignableFrom(cls);
	    }
	    
	    /**
	     * <p>Converts the specified primitive Class object to its corresponding
	     * wrapper Class object.</p>
	     *
	     * <p>NOTE: From v2.2, this method handles {@code Void.TYPE},
	     * returning {@code Void.TYPE}.</p>
	     *
	     * @param cls  the class to convert, may be null
	     * @return the wrapper class for {@code cls} or {@code cls} if
	     * {@code cls} is not a primitive. {@code null} if null input.
	     * @since 2.1
	     */
	    public static Class<?> primitiveToWrapper(Class<?> cls) {
	        Class<?> convertedClass = cls;
	        if (cls != null && cls.isPrimitive()) {
	            convertedClass = primitiveWrapperMap.get(cls);
	        }
	        return convertedClass;
	    }
	}
	
	public static class ArrayUtils {
	    /**
	     * An empty immutable {@code Class} array.
	     */
	    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
	    
	    /**
	     * <p>Checks whether two arrays are the same length, treating
	     * {@code null} arrays as length {@code 0}.
	     *
	     * <p>Any multi-dimensional aspects of the arrays are ignored.</p>
	     *
	     * @param array1 the first array, may be {@code null}
	     * @param array2 the second array, may be {@code null}
	     * @return {@code true} if length of arrays matches, treating
	     *  {@code null} as an empty array
	     */
	    public static boolean isSameLength(Object[] array1, Object[] array2) {
	        if ((array1 == null && array2 != null && array2.length > 0) ||
	            (array2 == null && array1 != null && array1.length > 0) ||
	            (array1 != null && array2 != null && array1.length != array2.length)) {
	                return false;
	        }
	        return true;
	    }
	}
	
	public static abstract class MemberUtils {
	    /** Array of primitive number types ordered by "promotability" */
	    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = { Byte.TYPE, Short.TYPE,
	            Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE };
	    
	    /**
	     * Compares the relative fitness of two sets of parameter types in terms of
	     * matching a third set of runtime parameter types, such that a list ordered
	     * by the results of the comparison would return the best match first
	     * (least).
	     *
	     * @param left the "left" parameter set
	     * @param right the "right" parameter set
	     * @param actual the runtime parameter types to match against
	     * <code>left</code>/<code>right</code>
	     * @return int consistent with <code>compare</code> semantics
	     */
	    public static int compareParameterTypes(Class<?>[] left, Class<?>[] right, Class<?>[] actual) {
	        float leftCost = getTotalTransformationCost(actual, left);
	        float rightCost = getTotalTransformationCost(actual, right);
	        return leftCost < rightCost ? -1 : rightCost < leftCost ? 1 : 0;
	    }
	    
	    /**
	     * Returns the sum of the object transformation cost for each class in the
	     * source argument list.
	     * @param srcArgs The source arguments
	     * @param destArgs The destination arguments
	     * @return The total transformation cost
	     */
	    private static float getTotalTransformationCost(Class<?>[] srcArgs, Class<?>[] destArgs) {
	        float totalCost = 0.0f;
	        for (int i = 0; i < srcArgs.length; i++) {
	            Class<?> srcClass, destClass;
	            srcClass = srcArgs[i];
	            destClass = destArgs[i];
	            totalCost += getObjectTransformationCost(srcClass, destClass);
	        }
	        return totalCost;
	    }
	    
	    /**
	     * Gets the number of steps required needed to turn the source class into
	     * the destination class. This represents the number of steps in the object
	     * hierarchy graph.
	     * @param srcClass The source class
	     * @param destClass The destination class
	     * @return The cost of transforming an object
	     */
	    private static float getObjectTransformationCost(Class<?> srcClass, Class<?> destClass) {
	        if (destClass.isPrimitive()) {
	            return getPrimitivePromotionCost(srcClass, destClass);
	        }
	        float cost = 0.0f;
	        while (srcClass != null && !destClass.equals(srcClass)) {
	            if (destClass.isInterface() && ClassUtils.isAssignable(srcClass, destClass)) {
	                // slight penalty for interface match.
	                // we still want an exact match to override an interface match,
	                // but
	                // an interface match should override anything where we have to
	                // get a superclass.
	                cost += 0.25f;
	                break;
	            }
	            cost++;
	            srcClass = srcClass.getSuperclass();
	        }
	        /*
	         * If the destination class is null, we've travelled all the way up to
	         * an Object match. We'll penalize this by adding 1.5 to the cost.
	         */
	        if (srcClass == null) {
	            cost += 1.5f;
	        }
	        return cost;
	    }
	    
	    /**
	     * Gets the number of steps required to promote a primitive number to another
	     * type.
	     * @param srcClass the (primitive) source class
	     * @param destClass the (primitive) destination class
	     * @return The cost of promoting the primitive
	     */
	    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> destClass) {
	        float cost = 0.0f;
	        Class<?> cls = srcClass;
	        if (!cls.isPrimitive()) {
	            // slight unwrapping penalty
	            cost += 0.1f;
	            cls = ClassUtils.wrapperToPrimitive(cls);
	        }
	        for (int i = 0; cls != destClass && i < ORDERED_PRIMITIVE_TYPES.length; i++) {
	            if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
	                cost += 0.1f;
	                if (i < ORDERED_PRIMITIVE_TYPES.length - 1) {
	                    cls = ORDERED_PRIMITIVE_TYPES[i + 1];
	                }
	            }
	        }
	        return cost;
	    }
	}
}
