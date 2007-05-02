/*
Copyright (c) 2007 Greg Vanore

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package oops.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import oops.DependencyVisitor;

/**
 * The Concurrent Dependency Visitor is an implementation of the
 * DependencyVisitor that uses Java concurrency utilities to retrieve the list
 * of failures or successes.
 */
public class ConcurrentDependencyVisitor implements DependencyVisitor {
    private CountDownLatch endGate = new CountDownLatch(1);
    private Set<String> failures = new HashSet<String>();
    private Set<String> successes = new HashSet<String>();
       
    public void end() {
        endGate.countDown();
    }

    public void fail(String name) {
        if (endGate.getCount() == 0) return;
        
        synchronized(failures) {
            failures.add(name);
        }
    }

    public void success(String name) {
        if (endGate.getCount() == 0) return;
        
        synchronized(successes) {
            successes.add(name);
        }
    }
    
    /**
     * Return the Set of classes that could not be found and loaded for
     * bytecode inspection.  This method will block for the completion of 
     * analysis.
     * @return a Set of Strings of class names
     * @throws InterruptedException
     */
    public Set<String> getFailures() throws InterruptedException {
        endGate.await();
        return new HashSet<String>(failures);
    }

    /**
     * Return the Set of classes that could be found and loaded for bytecode
     * inspection.  This method will block for the completion of analysis.
     * @return a Set of Strings of class names
     * @throws InterruptedException
     */
    public Set<String> getSuccesses()  throws InterruptedException {
        endGate.await();
        return new HashSet<String>(successes);
    }
}
