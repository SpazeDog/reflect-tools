# ReflectTools

In Android we currently have two injection systems, namely Xposed Bridge and Cydia Substrate. While the first seams to be the most popular, the later does have it's own userbase. And with Xposed Bridge fading slowly away into multiple unreliable sub-projects, there is no telling what will turn op in the future. 

ReflectTools are two things. First it is a large Reflection Library that can be used for any type of reflection work. It does not depend on anything. Secondly it is an abstraction layer for working with multiple code injection mechanisms using the same library. Currenty ReflectTools work with both Xposed Framework and Cydia Substrate using the same code base. And should the future turn up new code injection mechanisms, ReflectTools can easily be adapted to include that as well, while keeping compatiblity with the current and without changing any existing projects using it. 

So why support only one when you can support it all? 

**Checkout the [Wiki page](https://github.com/SpazeDog/reflect-tools/wiki) to get started**

### Include Library
-----------

**Android Studio**

First download the [ReflectTools-release.aar](https://github.com/SpazeDog/reflect-tools/raw/3.x/projects/reflectTools-release.aar) file. 

Place the file in something like the `libs` folder in your module. 

Open your `build.gradle` file for your module _(not the main project version)_. 

```
dependencies {
    compile(name:'reflectTools-release', ext:'aar')
}

repositories {
    flatDir {
        dirs 'libs'
    }
}
```

**Eclipse/ADT**

Download the source and import it into eclipse. Then simply include the new library project to your main project.
