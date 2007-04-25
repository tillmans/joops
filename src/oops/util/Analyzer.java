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

import java.io.IOException;
import java.util.Set;

import oops.Main;

public abstract class Analyzer {
    /**
     * Analyze the class path for failed dependencies.
     * @return a set of failed class names
     * @throws IOException if a class path entry cannot be read
     */
    public static Set<String> analyze() throws IOException, InterruptedException {
        Main analyzer = new Main();
        return getFailures(analyzer);
    }
    
    /**
     * Analyze a class or list of classes for failed dependencies.
     * @param classes the list of classes to check
     * @return a set of failed class names
     */
    public static Set<String> analyze(String... classes) throws InterruptedException {
        Main analyzer = new Main(classes);
        return getFailures(analyzer);
    }
    
    private static Set<String> getFailures(Main analyzer) throws InterruptedException {
        ConcurrentDependencyVisitor visitor = new ConcurrentDependencyVisitor();
        analyzer.setDependencyVisitor(visitor);
        
        Thread workerThread = new Thread(analyzer);
        workerThread.setDaemon(true);
        workerThread.start();
        
        try {
            return visitor.getFailures();
        } catch (InterruptedException ie) {
            //we are the only owner of the worker thread, so we need
            //to handle the interrupt for it as well.
            workerThread.interrupt();
            throw ie;
        }
    }
}
