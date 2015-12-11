package com.spazedog.lib.reflecttools;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.HashMap;

import com.spazedog.lib.reflecttools.ReflectClass.ReflectClassException;

public class ReflectField extends ReflectMember<ReflectField> {
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected final static HashMap<String, Field> oFieldCache = new HashMap<String, Field>();
	
	/**
	 * Search in a {@link ReflectClass} for a {@link Field}. This method might also search super and parent classes, depending 
	 * on the {@link Match} value parsed. 
	 * 
	 * @param fieldName
	 * 		The name of the {@link Field}
	 * 
	 * @param match
	 * 		How deep the method should search
	 * 
	 * @param rclass
	 * 		The class to search in
	 *
	 * @throws ReflectMemberException
	 * 		Thrown if the field could not be found
	 */
	public static Field findField(String fieldName, Match match, ReflectClass rclass) throws ReflectMemberException {
		String className = rclass.getObject().getName();
		String cacheName = className + "." + fieldName;
		Field field = oFieldCache.get(cacheName);
		
		if (field == null) {
			ReflectClass currentRClass = rclass;
			Throwable throwable = null;
			
			do {
				Class<?> clazz = currentRClass.getObject();
				
				do {
					try {
						field = clazz.getDeclaredField(fieldName);
						
					} catch (NoSuchFieldException e) {
						if (throwable == null)
							throwable = e;
					}
					
				} while (field == null && (clazz = clazz.getSuperclass()) != null);
				
			} while (field == null && match == Match.DEEP && (currentRClass = currentRClass.getParent()) != null);
			
			if (field == null) {
				throw new ReflectMemberException("Could not locate the field " + cacheName, throwable);
				
			} else {
				field.setAccessible(true);
			}
			
			oFieldCache.put(cacheName, field);
		}
		
		return field;
	}
	
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
	protected Field mField;
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected ReflectField(ReflectClass rclass, Field field) {
		mReflectClass = rclass;
		mField = field;
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
	public Member getObject() {
		return mField;
	}
	
	/**
	 * @see #getValue()
	 */
	public Object getValue() throws ReflectMemberException, ReflectClassException {
		return valueInternal(Result.DATA, null, false);
	}
	
	/**
	 * Get the value from this field
	 * 
	 * @param result
	 * 		Defines how to handle the field data
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if it was not possible to get the field data
	 * 
	 * @throws ReflectClassException
	 * 		Thrown if you select {@link Result#RECEIVER} and {@link ReflectClass} fails to create the instance
	 */
	public Object getValue(Result result) throws ReflectMemberException, ReflectClassException {
		return valueInternal(result, null, false);
	}
	
	/**
	 * Change the value in this field
	 * 
	 * @param value
	 * 		The new value
	 * 
	 * @throws ReflectMemberException
	 * 		Thrown if it was not possible to change the field value
	 */
	public void setValue(Object value) throws ReflectMemberException {
		valueInternal(null, value, true);
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected Object valueInternal(Result result, Object value, boolean setValue) throws ReflectMemberException, ReflectClassException {
		Object receiver = null;
		
		if (!isStatic()) {
			receiver = getReceiver();

			if (receiver == null) {
				throw new ReflectMemberException("Cannot invoke a non-static field without an accociated receiver, Field = " + mReflectClass.getObject().getName() + "#" + mField.getName());
			}
		}
		
		Object data = null;
		
		try {
			if (setValue) {
				mField.set(receiver, value);
				
			} else {
				data = mField.get(receiver);
			}
			
		} catch (Throwable e) {
			throw new ReflectMemberException("Unable to invoke field, Field = " + mReflectClass.getObject().getName() + "#" + mField.getName(), e);
		}
		
		if (!setValue) {
			switch (result) {
				case INSTANCE: 
					return ReflectClass.fromReceiver(data);
					
				case RECEIVER: 
					mReflectClass.setReceiver(data); 
					
				default:
					return data;
			}
		}
		
		return null;
	}
}
