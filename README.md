# ReflectTools

In Android we currently have two injection systems, namely Xposed Bridge and Cydia Substrate. While the first seams to be the most popular, the later does have it's own userbase. And with Xposed Bridge fading slowly away into multiple unreliable sub-projects, there is no telling what will turn op in the future. 

ReflectTools are two things. First it is a large Reflection Library that can be used for any type of reflection work. It does not depend on anything. Secondly it is an abstraction layer for working with multiple code injection mechanisms using the same library. Currenty ReflectTools work with both Xposed Framework and Cydia Substrate using the same code base. And should the future turn up new code injection mechanisms, ReflectTools can easily be adapted to include that as well, while keeping compatiblity with the current and without changing any existing projects using it. 

So why support only one when you can support it all? 
