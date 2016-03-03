# Introduction #

New for Oops! version 0.9 is a scripting interface.  In 0.8, the application could only be run as a stand-alone tool via the inbuilt main() method.  Now, however, there is an API for directing your own analysis.

# Scripting Oops! #

Scripting Oops! is fairly straightforward.  There are essentially three ways to script, in increasing complexity:
  * Call the static analyze(...) methods that return a Set of failed class names. (These methods block.)
  * Call the static analyze(DependencyVisitor visitor, ...) methods that use your custom event handler. (These methods also block.)
  * Instantiate an Analyzer object, set the DependencyVisitor, and call the run() method. (You may choose to block or create a Thread to run this asynchronously.)
Each method of scripting has three alternative invocations:
  * No class name identifiers, which causes Oops! to scan and test the entire class path.
  * A single class name, which will be the only class tested.
  * An array of class names, which will all be tested.

## The DependencyVisitor Interface ##

Before proceeding, we should explain the DependencyVisitor interface.  It is a simple interface that opens the analysis to your application.  It has three methods:
  * ` void success(String name) `
  * ` void fail(String name) `
  * ` void end() `
These are very straightforward.  When a class is successfully analyzed, it is passed to the `success` method.  Likewise, when a class could not be found, it is passed to the `fail` method.  And finally, when the task queue has been emptied, the `end` method is invoked.

## The analyze(...) methods ##

These are the simplest set and likely all you will need.  They use the ConcurrentDependencyVisitor utility class, which you may wish to look at for a sample implementation of a DependencyVisitor.  These methods return a Set of the failed classes, which is typically all you will need.

Sample use in Beanshell 2.0b4:
```
bsh % import oops.*;
bsh % show();
bsh % Analyzer.analyze("java.lang.String");
<[]>
bsh % Analyzer.analyze("test.FailTestA");
<["test.b.ClassB"]>
```

## The analyze(DependencyVisitor visitor, ...) methods ##

The second most useful way to interact with Oops! are these static utility methods that take a DependencyVisitor as the first parameter.  They allow you to define custom handlers for specific issues, or whatever you may need.  The first step, of course, is defining your own DependencyVisitor, instantiating it, and passing it into the method.  Here is another example in Beanshell:
```
bsh % import oops*;
bsh % v = new DependencyVisitor() {
void success(name) {}
void fail(name) {
  if ( name.startsWith("javax") ) {
    print("Java extension missing: " + name);
  }
}
void end() { print("done"); }
};
bsh % Analyzer.analyze(v, "com.mycompany.JmsClient");
Java extension missing: javax.jms.QueueConnectionFactory
Java extension missing: javax.jndi.Context
done
```

## Instantiating an Analyzer Object ##

Finally, instantiating an Analyzer object allows you to control precisely how you wish your Analyzer to execute.  The above two methods block, so the most common time you will use this is when you wish to asynchronously execute the Analyzer and wait for it to complete, for example in a GUI thread. (_nota bene_: you can probably use your GUI library's AsyncExec functionality rather than doing this, but I present the example anyway.)

First, implement your DependencyVisitor so that it notifies another thread when it is complete:
```
public class MyDependencyVisitor implements DependencyVisitor {
   ...
   public void end() {
     analysisCompleteCountDownLatch.countDown();
   }
}
```

Next, set up your event handling:
```
  //inside some event handler
  analysisCountDownLatch.await();
  ...
```

Finally, begin your analysis:
```
  ...
  Analyzer a = new Analyzer(targetClasses);
  a.setDependencyVisitor(theVisitor);
  Thread t = new Thread(a);
  t.setDaemon(true);
  t.start();
```

Implementing the Thread communication without `java.util.concurrent` is left as an exercise to the reader. :)