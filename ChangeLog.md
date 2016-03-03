# 0.9.1 #
  * Logging: you may now turn on java.util.Logging support for your scripted Analyzer by calling setLogging(true).  Optionally, you can pass in a Logger to use to override the default logger.  This may be convenient if you want to wrap other logging systems such as Log4J.
  * Some bugs fixed with class path expansion: the Analyzer no longer assumes that non-.class files are JARs.  This fix also removes the ugly IOException checked exception from Analyzer constructors.
  * A new usage option.  Try invoking the Analyzer from the command line with -h or --help!
  * It was available before, but now officially endorsed: split output.  Send successfully loaded classes to STDOUT and failures to STDERR, to be captured independently.

# 0.9 #
  * Scripting interface: use Oops! from Java, Beanshell, Groovy, etc. in addition to running it as a standalone program.
  * Better threading & interrupt handling (flag interrupts, stop processing, etc.)
  * DependencyVisitor interface for custom scripting
  * Quite a bit of clean up and refactoring.
  * Static convenience methods for scripting
  * Improvements to build script

# 0.8 #

Initial release.
  * Main program, which analyzes failed dependencies
  * Which utility, to find the source of currently loaded class files
  * Build script