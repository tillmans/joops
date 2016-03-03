## Testing, testing, testing. ##
_November 7, 2007_

For the next phase of Oops! I would like to focus on testing.  I think I should have started with clearly defined test cases and worked from there, but I had a need for a utility such as this and that drove the early development.  I won't let this project go 1.0 until I feel that I have made a comprehensive set of tests.  Of course, I value user feedback so if you are among the people who have downloaded and tried out oops and have something to say, don't be shy.  Once I "finish" this project in terms of my own needs, the community will have to drive future development.

If you browse the source you will see beginnings of tests in the repository already.  I believe I have most of the ways type references can be embedded already taken care of.  There are some special challenges in testing this tool, since the class path itself must change in order for some differential tests to be falsifiable.

## Oops! 0.9.1 Released ##
_November 5, 2007_

Oops! 0.9.1 has been released with some awesome fixes.  I haven't been working a lot on this project, but I'm still happy with it.  I had the opportunity to discuss it with some friends at a software convention recently, and it revived my interest in it.  I noticed that there are some bugs, and they are fixed!  There is a new feature or two for you to explore as well.  Please see the ChangeLog for more information.

## Oops! 0.9 Released ##
_May 17, 2007_

Oops! 0.9 has now been released.  This new version introduces a scripting interface for integration with your application, build, or testing process.  See more information in the ChangeLog.

## Getting ready for 0.9 ##
_April 25, 2007_

I have been working on updates for 0.9.  Planned features:
  * Scripting interface: use Oops! from Java, Beanshell, Groovy, etc. in addition to running it as a standalone program.
  * Better threading: interrupt handling (flag interrupts, stop processing, etc.)
  * DependencyVisitor interface for custom scripting
  * Utility class for launching an analysis (may refactor Main class name and merge this)

## News page ##
_April 25, 2007_

I have created a news page to post status updates, heartbeats, or other things about the project as I see fit.



