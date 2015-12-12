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

import com.spazedog.lib.reflecttools.ReflectUtils.LOG;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeLogic;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeOriginal;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeParams;
import com.spazedog.lib.reflecttools.bridge.MethodBridge.BridgeType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * For Internal Use
 * 
 * @hide
 */
class MethodXposed extends XC_MethodHook implements BridgeLogic {
	
	private MethodBridge mBridge;
	private Member mMember;

	protected MethodXposed(MethodBridge bridge, Member member) {
		super();

        mBridge = bridge;
        mMember = member;

        LOG.Debug(MethodXposed.class.getName(), "Setting up new bridge\n\t\t" +
                "Class = " + member.getDeclaringClass().getName() + "\n\t\t" +
                "Member = " + (member instanceof Method ? member.getName() : "Constructor"));

        XposedBridge.hookMethod(mMember, this);
	}
	
	@Override
	protected void beforeHookedMethod(MethodHookParam params) throws Throwable {
		XposedParams bridgeParams = XposedParams.getInstance(params);
		mBridge.bridgeBegin(bridgeParams);
		bridgeParams.recycle();
	}
	
	@Override
	protected void afterHookedMethod(MethodHookParam params) throws Throwable {
		XposedParams bridgeParams = XposedParams.getInstance(params);
		mBridge.bridgeEnd(bridgeParams);
		bridgeParams.recycle();
	}

    @Override
    public BridgeOriginal getOriginal() {
        return new BridgeOriginal() {
            @Override
            public Object invoke(Object receiver, Object... args) {
                try {
                    return XposedBridge.invokeOriginalMethod(mMember, receiver, args);

                } catch (Throwable e) {
                    LOG.Error(MethodXposed.class.getName(), e.getMessage(), e);
                }

                return null;
            }
        };
    }

    protected static class XposedParams extends BridgeParams {
		private static final Object oLock = new Object();
		private static XposedParams oInstance;
		private XposedParams mLastInstance = null;
		
		private MethodHookParam mParams;
		
		private static XposedParams getInstance(MethodHookParam params) {
			synchronized (oLock) {
				if (oInstance == null) {
					oInstance = new XposedParams();
				}
				
				XposedParams instance = oInstance;
				oInstance = instance.mLastInstance;
				
				instance.mLastInstance = null;
				instance.mParams = params;
				instance.method = params.method;
				instance.receiver = params.thisObject;
				instance.args = params.args;
				instance.bridgeType = BridgeType.XPOSED;
				
				return instance;
			}
		}
		
		private void recycle() {
			synchronized (oLock) {
				mParams = null;
				method = null;
				receiver = null;
				args = null;
				mLastInstance = oInstance;
				
				oInstance = this;
			}
		}
		
		@Override
		public void setResult(Object result) {
			mParams.setResult(result);
		}
		
		@Override
		public Object getResult() {
			return mParams.getResult();
		}

		@Override
		public Object invokeOriginal(Object... args) {
			return XposedBridge.invokeOriginalMethod(method, receiver, args);
		}
	}
}
