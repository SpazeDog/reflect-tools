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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

public abstract class ReflectMember<RT extends ReflectMember<RT>> extends ReflectObject<Member> {

    protected OnMemberReceiverListener mListener;
    protected volatile boolean mListenerActive = false;
	
	public static class ReflectMemberException extends ReflectException {
		private static final long serialVersionUID = 8211193410992359199L;

		public ReflectMemberException(Throwable cause) {
			super(cause);
		}
		
		public ReflectMemberException(String detailMessage) {
			super(detailMessage, null);
		}

		public ReflectMemberException(String detailMessage, Throwable cause) {
			super(detailMessage, cause);
		}
	}
	
	/**
	 * Matching values that defines how to handle results from 
	 * the member collection
	 */
	public enum Result {
		/**
		 * The result should be used to create and return a new 
		 * {@link ReflectClass} instance. Depending on the data type, 
		 * this would search for a class if the data is a {@link String}, or simply use it as is if the 
		 * data is a {@link Class}, or be used as a receiver if the data is an instance of some sort. <br /><br />
		 * 
		 * The newly created {@link ReflectClass} instance will be returned to the caller instead of the data
		 */
		INSTANCE, 
		
		/**
		 * The result should be used as a receiver for the current associated {@link ReflectClass} instance. 
		 * The data itself will also be returned just like when using {@link #DATA}
		 */
		RECEIVER, 
		
		/**
		 * The result will not be used for anything. This will simply return the data to the caller
		 */
		DATA
	}
	
	/**
	 * Matching value for member searches. 
	 * These are used to tell member classes how to search for members. 
	 */
	public enum Match {
		/**
		 * This option will tell member classes to include Super and Parent classes in their search. 
		 * This can be dangerous in some cases. Especially during attempts to add hooks to a member, 
		 * which could result in adding hooks to a wrong class. But it can also help access members from nested classes. 
		 * Use this with care.
		 */
		DEEP,
		
		/**
		 * This option will tell member classes to include Super classes in their search. 
		 * The option will also test other methods in cases where an exact match cannot be found. 
		 * For example it would include <code>method(Object)</code> if <code>method(String)</code> does not exist. 
		 * This is the best option for most cases. 
		 */
		BEST, 
		
		/**
		 * This option will tell member classes that a match should be exact. This means that parameter types must 
		 * match 100% with no exceptions. This option will not be useful in most cases. 
		 */
		EXACT
	}
	
	/**
	 * There can be circumstances where you might need to customize a receiver before using it. 
	 * In these cases this listener can be used. It will be invoked each time a receiver is used to 
	 * invoke a member. 
	 */
	public static interface OnMemberReceiverListener {
		/**
		 * Invoked when a receiver is needed to invoke a member. 
		 * This will only be called on non-static classes where a receiver is needed. 
		 * 
		 * @param member
		 * 		The member being invoked
		 */
		public Object onRequestReceiver(ReflectMember<?> member);
	}

    /**
     * @deprecated
     *      Use {@link OnMemberReceiverListener instead}
     */
    public static interface OnRequestReceiverListener extends OnMemberReceiverListener {}
	
	/**
	 * Set the {@link OnMemberReceiverListener} listener for this member
	 * 
	 * @param listener
	 * 		The {@link OnMemberReceiverListener} to be invoked when a receiver is being requested
	 */
	public void setReceiverListener(OnMemberReceiverListener listener) {
        mListener = listener;
    }

    /**
     * @deprecated
     *      Use {@link #setReceiverListener(OnMemberReceiverListener)}
     *
     * @param listener
     */
    public void setOnRequestReceiverListener(OnMemberReceiverListener listener) {
        mListener = listener;
    }

	/**
	 * Returns the {@link ReflectClass} instance belonging to this member
	 */
	public abstract ReflectClass getReflectClass();
	
	/**
	 * Whether or not the member was declared static
	 */
	public boolean isStatic() {
		return Modifier.isStatic(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared private
	 */
	public boolean isPrivate() {
		return Modifier.isPrivate(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared protected
	 */
	public boolean isProtected() {
		return Modifier.isProtected(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared public
	 */
	public boolean isPublic() {
		return Modifier.isPublic(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared abstract
	 */
	public boolean isAbstract() {
		return Modifier.isAbstract(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member is part of an interface
	 */
	public boolean isInterface() {
		return Modifier.isInterface(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared final
	 */
	public boolean isFinal() {
		return Modifier.isFinal(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member is native
	 */
	public boolean isNative() {
		return Modifier.isNative(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared strict
	 */
	public boolean isStrict() {
		return Modifier.isStrict(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared synchronized
	 */
	public boolean isSynchronized() {
		return Modifier.isSynchronized(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member was declared volatile
	 */
	public boolean isVolatile() {
		return Modifier.isVolatile(getObject().getModifiers());
	}
	
	/**
	 * Whether or not the member is transient
	 */
	public boolean isTransient() {
		return Modifier.isTransient(getObject().getModifiers());
	}
	
	/**
	 * This method takes the receiver from the member's {@link ReflectClass}. 
	 * If the receiver does not match the class where this member was declared, it start searching 
	 * parent instances to find the correct receiver for this member. <br /><br />
	 * 
	 * This action will handle cases where a member was found based on a nested class. 
	 */
	public synchronized Object getReceiver() {
		Class<?> clazz = getObject().getDeclaringClass();
		Object receiver = null;

        if (mListener != null && !mListenerActive) {
            mListenerActive = true;
            receiver = mListener.onRequestReceiver(this);
            mListenerActive = false;
        }

        if (receiver == null) {
            receiver = getReflectClass().getReceiver();
        }
		
		if (receiver != null && !clazz.isInstance(receiver)) {
			Field field;
			
			try {
				Object parentReceiver = receiver;
				
				do {
					field = parentReceiver.getClass().getDeclaredField("this$0");
					field.setAccessible(true);
					
					parentReceiver = field.get(parentReceiver);
					
				} while (parentReceiver != null && !clazz.isInstance(parentReceiver));
				
				if (parentReceiver != null) {
					return parentReceiver;
				}
				
			} catch (Throwable ignorer) {}
		}
		
		return receiver;
	}
}
