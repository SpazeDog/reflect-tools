ReflectTools
============

XposedBridge is one of the largest things that have happened in Android in a long time. Even the SuperUser has started to become obsolete because you no longer have to 
hack the framework by executing shell commands and changing configuration files and database entries. However, as much as I love XposedBridge, 
I cannot get along with it's helper class `XposedHelpers`. The class itself is messy with to many different methods for the same tasks, only with 
minor differences which could have been handled by adding an addition parameter. The parameters also has no standards. Some requires a string representation of a class or Member, while 
others requires the actual class or member object to be parsed. 

And then there is the issue of not being able to locate all methods, depending on their access modifiers. Or the fact that you can't really use the class 
outside of hooked classes. If you try to use the class with your own application uid, you will get exceptions thrown.

So to put the fun back into working with XposedBridge (because it is fun), I created a new helper class that have addressed these issues. 
And it is also build on true OOP in mind and with the ability to be used even without XposedBridge installed (No hooked classes though). 

Usage
-----

Here is an example of the basics of adding hooks to a class

```java
/*
 * Get an instance of ReflectClass for the PhoneWindowManager
 */
ReflectClass pwm = ReflectTools.getReflectClass("com.android.internal.policy.impl.PhoneWindowManager");

/*
 * Add a hook to the constructor
 */
pwm.inject(new XC_MethodHook() {
	@Override
	protected final void afterHookedMethod(final MethodHookParam param) {
		
	}
});

/*
 * Add a hook to init
 */
pwm.inject("init", new XC_MethodHook() {
	@Override
	protected final void afterHookedMethod(final MethodHookParam param) {
		
	}
});
```

The above example will add a hook to all available constructors in that class and to all methods named init. 
If we wanted to add a hook to a specific constructor and init method, then we would have to get a `ReflectConstructor` and `ReflectMethod` instance of those two specific members. 

```java
/*
 * Get an instance of ReflectClass for the PhoneWindowManager
 * 
 * NOTE: getReflectClass excepts String, Class and Instances (Object). 
 */
ReflectClass pwm = ReflectTools.getReflectClass("com.android.internal.policy.impl.PhoneWindowManager");

/*
 * Get the specific constructor
 * 
 * NOTE: You can parse both Class object or String representations of a Class as Parameter Type
 */
ReflectConstrctor = pwm.getConstructor(ReflectTools.MEMBER_MATCH_BEST, "android.content.Context", Integer.TYPE);

/*
 * Get the specific method
 */
ReflectMethod = pwm.getMethod("init", ReflectTools.MEMBER_MATCH_BEST, String.class);

/*
 * Add a hook to a specific constructor
 */
ReflectConstrctor.inject(new XC_MethodHook() {
	@Override
	protected final void afterHookedMethod(final MethodHookParam param) {
		
	}
});

/*
 * Add a hook to a specific init
 */
ReflectMethod.inject(new XC_MethodHook() {
	@Override
	protected final void afterHookedMethod(final MethodHookParam param) {
		
	}
});
``` 

Locate Members
--------------

There are two tools to find a method with ReflectClass, those are `ReflectClass#getMethod` and `ReflectClass#locateMethod`. The first one will 
look for the method in the current class defined in your ReflectClass instance or it's Super Classes. The second will extend the search to all Parent Classes.

```java
public class Level_1 {
	private String level_1_method() {
		return "";
	}
	
	public class Level_2 {
		public String level_2_method() {
			return "";
		}
		
		public void example() {
			/*
			 * This will work as the method we call is in the same class
			 */
			String output = (String) ReflectTools.getReflectClass(this).getMethod("level_2_method").invoke(this);
			
			/*
			 * This will fail as the level_1_method method is not part of the Level_2 class
			 */
			String output = (String) ReflectTools.getReflectClass(this).getMethod("level_1_method").invoke(this);
			
			/*
			 * This will work because we used locateMethod() instead of getMethod()
			 */
			String output = (String) ReflectTools.getReflectClass(this).locateMethod("level_1_method").invoke(this);
		}
	}
}
```

The `invoke` methods has been made to automatically adapt any parsed instance to match the one required for the class holding the member. 
So even though we parse `this`, which is `Level_2` when invoking `Level_1#level_1_method` in the third example, it will invoke with success. 