# ReflectTools

XposedBridge is one of the largest things that have happened in Android in a long time. Even the SuperUser has started to become obsolete because you no longer have to 
hack the framework by executing shell commands and changing configuration files and database entries. However, as much as I love XposedBridge, 
I cannot get along with it's helper class `XposedHelpers`. The class itself is messy with to many different methods for the same tasks, only with 
minor differences which could have been handled by adding an addition parameter. The parameters also has no standards. Some requires a string representation of a class or Member, while 
others requires the actual class or member object to be parsed. 

And then there is the issue of not being able to locate all methods, depending on their access modifiers. Or the fact that you can't really use the class 
outside of hooked classes. If you try to use the class with your own application uid, you will get exceptions thrown.

So to put the fun back into working with XposedBridge (because it is fun), I created a new helper class that have addressed these issues. 
And it is also build on true OOP in mind and with the ability to be used even without XposedBridge installed (No hooked classes though). 

## Class Tools (ReflectClass)

Unlike java's own `Class<?>` object, the `ReflectClass` class stores the actual `Class Instances` (if parsed) within it's own instance. This makes it a lot easier to work with located classes, as you will never have to manually keep track of the `Class Instances` while invoking methods, setting field values etc.
    
```java
/*
 * This ReflectClass instance does not have a receiver. 
 * It can still be used to locate methods, fields and constructors. 
 * It can also invoke static members or create new instances. 
 */
ReflectClass clazz = new ReflectClass( "my.domain.MyClass" );

/*
 * This ReflectClass instance contains a receiver object to MyClass, which means that 
 * any members located through this instance, can also handle non-static member calls. 
 */
MyClass myClassInstance = new MyClass();
ReflectClass clazzWithReceiver = new ReflectClass( myClassInstance );
```

#### Binding IPC to the Receiver

ReflectClass also has build-in tools to easier bind IPC connection to an interface. Android has a lot of internal interfaces which is not available through the normal API's. 

```java
/*
 * This will provide an instance with a reference to the IStatusBarService interface
 * along with an IPC connection in the receiver to the actual service. 
*/
ReflectClass statusBarService = new ReflectClass("com.android.internal.statusbar.IStatusBarService").bindInterface("statusbar");
```

#### Receiver Listeners

If for some reason it is not possible to store the receiver, maybe because it has not been created at the time where you need to access the class, or if it is constantly changing, then you can instead add a listener which will be used whenever the receiver is needed but not available.

```java
ReflectClass statusBarService = new ReflectClass("com.android.internal.statusbar.IStatusBarService");

/*
 * Now whenever you call a method or retrieves a value from an object field 
 * within this class, the onReceiver() method will be called to get a receiver
 */
statusBarService.setOnReceiverListener(new OnReceiverListener(){
	@Override
	public Object onReceiver(ReflectMember<?> member) {
		Object receiver;
		
		// Populate receiver
		
		return receiver;
	}
});
```

## Method Tools (ReflectMethod & ReflectConstructor)

The `ReflectMethod` and `ReflectConstructor` will always contain a reference to the `ReflectClass` instance from which they was created. This means that any change to the `ReflectClass` instance, like the receiver, will affect any instances of `ReflectMethod` and `ReflectConstructor` that was initiated from that `ReflectClass` instance.

It also means that the creating `ReflectClass` instance can always be retrieved from any member instances. 

```java
ReflectClass clazzWithReceiver = new ReflectClass( new MyClass() );

/*
 * This call will search the main class along with all super classes/interfaces
 */
ReflectMethod methodWithReceiver = clazzWithReceiver.findMethod( "MethodName", Match.Best, Param1.class, Param1.class );

/*
 * This call will not only search the main class and all super classes/interfaces,
 * it will also search all parent classes in cases where the parsed class is nested. 
 */
ReflectMethod methodWithReceiver = clazzWithReceiver.findMethodDeep( "MethodName", Match.Best, Param1.class, Param1.class );

/*
 * Invoking the method will only require the arguments as the receiver is stored in the ReflectClass instance, 
 * which in turn in referenced in the ReflectMethod instance. 
 */
methodWithReceiver.invoke(arg1, arg2);
```

#### Define Parameter Types

When locating a method or constructor, there are several ways of defining the parameter types. You can pass the `Class<?>` object of the type or you can define it as a `String` which will be resolved by the method.

```java
/*
 * The last parameter type will be converted into String.class once parsed
 */
ReflectMethod methodWithReceiver = clazzWithReceiver.findMethod( "MethodName", Match.Best, String.class, "java.lang.String" );
```

The above example is actually just a redirect call to `findMethod(String, Match, ReflectParameters)`. The `ReflectParameters` is an abstract class that extends into `ReflectParameterTypes` (which is used by the above example) and `ReflectArgumentTypes`.

The `ReflectParameterTypes` is used to resolve parameter types based on string representations, like `'java.lang.String' ~> String.class`. The `ReflectArgumentTypes` is used to convert arguments into parameter types. 

So if you need to locate a method or constructor based on available arguments rather than pre-defined types, you can do this by parsing `ReflectArgumentTypes` to the `findMethod()` or any other member locators. 

```java
ReflectMethod methodWithReceiver = clazzWithReceiver.findMethod( "MethodName", Match.Best, new ReflectArgumentTypes(args1, arg2) );
```