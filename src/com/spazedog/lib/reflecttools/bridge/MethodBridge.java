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

import com.spazedog.lib.reflecttools.ReflectUtils;

import java.lang.reflect.Member;

public abstract class MethodBridge {
	
	/**
	 * Used by {@link BridgeParams#bridgeType} to identify the 
	 * system used to invoke a hook
	 */
	public enum BridgeType {
		/**
		 * The hook was invoked by Xposed Bridge
		 */
		XPOSED,
		
		/**
		 * The hook was invoked by Cydia Substrate
		 */
		CYDIA
	}
	
	/**
	 * Create a bridge between this callback and the parsed {@link Member}
	 * 
	 * @param member
	 * 		The {@link Member} to bridge
	 */
	public final BridgeOriginal attachBridge(Member member) {
		if (ReflectUtils.bridgeInitiated()) {
            BridgeLogic bridge = null;
            BridgeOriginal original = null;

			if (ReflectUtils.usesCydia()) {
                bridge = new MethodCydia(this, member);
					
			} else {
                bridge = new MethodXposed(this, member);
			}

            original = bridge.getOriginal();

            bridgeAttached(member, original);

            return original;
		}

        return null;
	}
	
	/**
	 * This method is invoked before the Original. If you want to skip the Original method, 
	 * just set a custom result using {@link BridgeParams#setResult(Object)}
	 * 
	 * @param params
	 * 		Contains information about the original member and the arguments parsed to it
	 */
	public void bridgeBegin(BridgeParams params) {}
	
	/**
	 * This method is invoked after the Original, or after {@link #bridgeBegin(BridgeParams)} 
	 * if the Original has been skipped. 
	 * 
	 * @param params
	 * 		Contains information about the original member and the arguments parsed to it
	 */
	public void bridgeEnd(BridgeParams params) {}

    /**
     * Called once the bridge has been attached
     *
     * @param member
     *      The member where the bridge was attached to
     *
     * @param original
     *      An {@link BridgeOriginal} instance that can be used to call the original member.
     *      This is meant to be used outside of the hooked method. Use <code>invokeOriginal</code>
     *      from {@link BridgeParams} if you are executing from within the hook.
     */
    public void bridgeAttached(Member member, BridgeOriginal original) {}
	
	/**
	 * Container used by {@link #bridgeBegin(BridgeParams)} and {@link #bridgeEnd(BridgeParams)}
	 */
	public abstract static class BridgeParams {
		/**
		 * The original {@link Member} being invoked
		 */
		public Member method;
		
		/**
		 * The instance of the class being invoked or <code>NULL</code> if it is a static call
		 */
		public Object receiver;
		
		/**
		 * Arguments that was parsed to the original {@link Member}
		 */
		public Object[] args;
		
		/**
		 * The type of system used to invoke the hook, like Xposed Bridge or Cydia Substrate
		 */
		public BridgeType bridgeType;
		
		/**
		 * Change the result that should be parsed back to the caller of the original {@link Member}
		 * 
		 * @param result
		 */
		public abstract void setResult(Object result);
		
		/**
		 * Return the current result that is to be parsed back to the caller of the original {@link Member}
		 */
		public abstract Object getResult();

		/*
		 * Invoke the original method, bypassing the hooks.
		 */
		public abstract Object invokeOriginal(Object... args);
	}
	
	/**
	 * For Internal Use
	 * 
	 * @hide
	 */
	protected static interface BridgeLogic {
		public BridgeOriginal getOriginal();
	}

    public static interface BridgeOriginal {
        public Object invoke(Object receiver, Object... args);
    }
}
