package com.spazedog.lib.reflecttools.utils;

public class ReflectConstants {
	public static enum Match {
		DEFAULT (1, false),
		SUPPRESS (1, true),
		BEST (1, false),
		EXACT (0, false),
		BEST_SUPPRESS (1, true),
		EXACT_SUPPRESS (0, true);
		
		private final Integer mMatchLevel;
		private final Boolean mSuppress;
		
		private Match(Integer matchLevel, Boolean suppressException) {
			mMatchLevel = matchLevel;
			mSuppress = suppressException;
		}
		
		public Integer getMatch() {
			return mMatchLevel;
		}
		
		public Boolean suppress() {
			return mSuppress;
		}
	}
}
