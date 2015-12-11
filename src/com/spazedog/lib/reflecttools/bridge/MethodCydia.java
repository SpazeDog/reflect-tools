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

package com.spazedog.lib.reflecttools.bridge;

import com.saurik.substrate.MS;
import com.spazedog.lib.reflecttools.ReflectUtils.LOG;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeLogic;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeParams;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * For Internal Use
 * 
 * @hide
 */
@SuppressWarnings("rawtypes")
class MethodCydia extends MS.MethodAlteration implements BridgeLogic {
	
	private MethodBridge mBridge;
	private Member mMember;
	
	@SuppressWarnings("unchecked")
	public static void setupBridge(MethodBridge bridge, Member member) {
		LOG.Debug(MethodCydia.class.getName(), "Setting up new bridge\n\t\t" + 
				"Class = " + member.getDeclaringClass().getName() + "\n\t\t" + 
				"Member = " + (member instanceof Method ? member.getName() : "Constructor"));
		
		if (member instanceof Constructor) {
			MS.hookMethod(member.getDeclaringClass(), (Constructor) member, new MethodCydia(bridge, member));
			
		} else {
			MS.hookMethod(member.getDeclaringClass(), (Method) member, new MethodCydia(bridge, member));
		}
	}
	
	public MethodCydia(MethodBridge bridge, Member member) {
		super();
		
		mBridge = bridge;
		mMember = member;
		
		try {
			Field field = mBridge.getClass().getDeclaredField("mLogic");
			field.setAccessible(true);
			field.set(mBridge, this);
			
		} catch (Throwable e) {}
	}

	@SuppressWarnings("unchecked")
	@Override
	public final Object invoked(Object receiver, Object... args) throws Throwable {
		CydiaParams bridgeParams = CydiaParams.getInstance(this, mMember, receiver, args);
		mBridge.bridgeBegin(bridgeParams);
		
		if (!bridgeParams.mQuit) {
			bridgeParams.setResult( invoke(receiver, args) );
		}

		mBridge.bridgeEnd(bridgeParams);
		
		Object result = bridgeParams.mResult;
		bridgeParams.recycle();
		
		return result;
	}
	
	protected static class CydiaParams extends BridgeParams {
		private static final Object oLock = new Object();
		private static CydiaParams oInstance;
		private CydiaParams mLastInstance = null;
		
		private boolean mQuit = false;
		private Object mResult;
        private MS.MethodAlteration mHook;
		
		private static CydiaParams getInstance(MS.MethodAlteration hook, Member member, Object receiver, Object[] args) {
			synchronized (oLock) {
				if (oInstance == null) {
					oInstance = new CydiaParams();
				}
				
				CydiaParams instance = oInstance;
				oInstance = instance.mLastInstance;
				
				instance.mLastInstance = null;
				instance.method = member;
				instance.receiver = receiver;
				instance.args = args;
				instance.bridgeType = BridgeType.CYDIA;

                instance.mHook = hook;
				
				return instance;
			}
		}
		
		private void recycle() {
			synchronized (oLock) {
				method = null;
				receiver = null;
				args = null;
				mResult = null;
				mQuit = false;
                mHook = null;
				mLastInstance = oInstance;
				
				oInstance = this;
			}
		}
		
		@Override
		public void setResult(Object result) {
			mResult = result;
			mQuit = true;
		}
		
		@Override
		public Object getResult() {
			return mResult;
		}

		@Override
		public Object invokeOriginal(Object... args) {
            try {
                return mHook.invoke(receiver, args);

            } catch (Throwable e) {
                return null;
            }
		}
	}

	@SuppressWarnings("unchecked")
	@Override
    public Object invokeOriginal(Object receiver, Object... args) {
        try {
            return invoke(receiver, args);

        } catch (Throwable e) {
        }
		
		return null;
	}
}
