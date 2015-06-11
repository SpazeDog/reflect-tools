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
	 * @hide
	 */
	private BridgeLogic mLogic;
	
	/**
	 * This method is invoked before the Original. If you want to skip the Original method, 
	 * just set a custom result using {@link BridgeParams#setResult(Object)}
	 * 
	 * @param params
	 * 		Contains information about the original member and the arguments parsed to it
	 */
	protected void bridgeBegin(BridgeParams params) {}
	
	/**
	 * This method is invoked after the Original, or after {@link #bridgeBegin(BridgeParams)} 
	 * if the Original has been skipped. 
	 * 
	 * @param params
	 * 		Contains information about the original member and the arguments parsed to it
	 */
	protected void bridgeEnd(BridgeParams params) {}
	
	/**
	 * This method will invoke the original member and return 
	 * it's return value
	 * 
	 * @param args
	 * 		Arguments to parse to the original member
	 */
	protected Object invoke(Object... args) {
		if (mLogic != null) {
			return mLogic.invoke(args);
		}
		
		return null;
	}
	
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
	}
	
	/**
	 * @hide
	 */
	protected static interface BridgeLogic {
		public Object invoke(Object... args);
	}
}
