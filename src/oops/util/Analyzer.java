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
