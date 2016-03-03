## Oops! 0.9.1 released on Nov 5, 2007! ##

# Oops! #
Oops! is a simple program to find missing dependencies.  You can specify a single class to search, a list of classes, or Oops! can even map the entire class path for any and all missing pieces.  Oops! will descend into every referenced class, recursively searching for references and verifying them against the specified class path.

## Abstract ##
If you depend on 3rd-party code directly, your code will not compile unless you include it directly in your class path.  But you may run into problems when using a binary distribution of your 3rd-party utility, since its dependencies will not be immediately resolved in your development environment.  That is, if my project **Foo** depends on library **Baz**, that is sufficient for me to compile and distribute **Foo** with **Baz**.  The issue at hand is that if **Baz** requires **Thud** in a rare code path, **Foo** could blow up during use with a NoClassDefFound error.  I didn't include **Thud** with my release, and because my testing wasn't thorough, I didn't even know that it was needed!  Oops! is a tool to mitigate these issues.  It verifies that every referenced type is available in the class path, and can scan specific classes or the entire class path specification itself.

## History ##
What does Oops! mean?  Unfortunately, it's all rather silly.  Originally, Oops! was to be called 'Depends.'  This is not a very interesting name for a dependency checker.  Those using it started calling it 'Oops I crapped my pants' instead, an homage from the Saturday Night Live spoof advertisement.  The name stuck, and here we are.

## Usage ##
Add the `oops.jar` to your class path and execute the class oops.Main.  Even though it would be convenient to build an executable jar, using one unfortunately overrides the class path. There are three possible ways to use Oops!:
  1. Find all unresolved dependencies for a given class.
  1. Find all unresolved dependencies for a list of classes.
  1. Find all unresolved dependencies for the entire class path.
Typically, Oops! will only produce output when there is a problem.  Like many UNIX commands, no output should be celebrated, as it is the success condition.  However, if you enjoy watching your console scroll, you may specify `-v` or `--verbose` to make Oops! print its successes as well as its failures.

### Find all unresolved dependencies for a given class ###
Specify this class as the argument for Oops!
```
java -cp %MY_CLASSPATH%;oops.jar oops.Analyzer some.class.file.to.Verify
```

### Find all unresolved dependencies for a list of classes ###
Specify `-` as the argument for Oops! to read a list from standard input. Directly:
```
java -cp %MY_CLASSPATH%;oops.jar oops.Analyzer -
some.Clazz
another.package.Type
^Z
```
Or via a list of classes:
```
java -cp %MY_CLASSPATH%;oops.jar oops.Analyzer - < check_these_classes.txt
```
This mode is handy when verifying that a previous failure has been resolved.

### Find all unresolved dependencies for the entire class path ###
Specify no arguments at all to Oops! and it will map out the entire class path, making sure that there are no unspecified references.
```
java -cp %MY_CLASSPATH%;oops.jar oops.Analyzer
```

## Integrating with Eclipse ##
Oops! isn't an Eclipse plug-in, but it still plays nice with this (and other) development environments.  When you run a Java class as an application in Eclipse, it inherits the project class path by default.  If you would like to easily verify your Eclipse project class path, add `oops.jar` to your libraries.  Expand it to find the Main type, and then right click this and Run As -> Java Application.  Any output you see in the console will be unresolved dependencies.  Hopefully there are none!

## Bonus Utilities ##
Oops! at this time comes with one bonus utility, Which.

### Which ###
Which is similar to the UNIX utility of the same name.  It finds the location of a loaded class and prints it to the screen.  You can list any number of class identifiers as arguments.
```
$ java -cp oops.jar oops.Which java.lang.String
java.lang.String: /usr/java/jre1.5.0_11/lib/rt.jar
```
When the sole argument is a hyphen, Which reads the class list from standard input.
```
$ java -cp bin;lib\asm-3.0.jar oops.Which -
org.objectweb.asm.Type
oops.Analyzer
^Z
org.objectweb.asm.Type: ~/workspace/Oops/lib/asm-3.0.jar
oops.Analyzer         : ~/workspace/Oops/bin/oops/Analyzer.class
```