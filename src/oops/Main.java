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

package oops;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oops.util.ConcurrentDependencyVisitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * The Oops! main program, which is used to verify that there are no
 * unresolved dependencies in the active class path.  Invocation arguments:
 * <ol>
 *   <li>Nothing: scan every class and jar in the current class path and verify.</li>
 *   <li>"-": read a list of one or more classes from standard input.</li>
 *   <li>A valid Java class identifier: verify the specified class.</li>
 * </ol>
 * Add "--verbose" or "-v" to any option in order to print both success
 * and failures.  To facilitate scripting, you may also use "--split" or "-s",
 * which implies verbose.  But in this case, when a class is processed its name is
 * printed to STDOUT, and if it fails, its name is printed to STDERR. By default,
 * Oops! only prints failed dependencies.  That means no output is a good thing!
 */
public class Main implements Runnable {
    private final BlockingQueue<String> discoveries = new LinkedBlockingQueue<String>();
    private final ConcurrentMap<String, Boolean> analysis = new ConcurrentHashMap<String, Boolean>();
    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    private final AtomicBoolean interruptFlag = new AtomicBoolean(false);
    
    private static final Pattern CLSID = Pattern.compile("\\[*?L(.*?(/.*?)*);");
    
    protected final ClassVisitor CLS_FINDER = new ClassReferenceFinder();
    protected final MethodVisitor MTD_FINDER = new MethodReferenceFinder();
    protected final FieldVisitor FLD_FINDER = new FieldReferenceFinder();
    protected final AnnotationVisitor ANT_FINDER = new AnnotationReferenceFinder();
    
    protected DependencyVisitor visitor = new DefaultDependencyVisitor(OutputStyle.STANDARD);
    
    protected static String extractClass(String desc) {
        Matcher m = CLSID.matcher(desc);
        if (m.matches()) {
            if (m.group(1).length() == 1) return null;
            return m.group(1);
        }
        return null;
    }
    
    /**
     * Use a specific DependencyVisitor instead of the default.  The default
     * dependency visitor prints failures to standard output.
     * @param visitor the visitor to use
     */
    public void setDependencyVisitor(DependencyVisitor visitor) {
        this.visitor = visitor;
    }
    
    /**
     * Construct an analyzer which reads the entire classpath.
     * @throws IOException - if error reading class path
     */
    public Main() throws IOException {
        addClasspath();
    }
    
    /**
     * Construct an analyzer for a single class.
     * @param clazz - the fully qualified class name
     */
    public Main(String clazz) {
        addClass(clazz);
    }
    
    /**
     * Construct an analyzer for a list of classes.
     * @param classes - the fully qualified class names
     */
    public Main(String... classes) {
        addClass(classes);
    }
    
    public static void main(String... args) throws IOException {
        Main m = new Main();
        
        //Process command arguments.
        String input = null;
        OutputStyle output = OutputStyle.STANDARD;
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.equals("-v") || arg.equals("--verbose")) {
                    output = OutputStyle.VERBOSE;
                } else if (arg.equals("-s") || arg.equals("--split")) {
                    output = OutputStyle.SPLIT;
                } else {
                    input = arg;
                }
            }
        }
        
        if (input == null) {
            m.addClasspath();
        } else if (input.equals("-")) {
            //read list of classes from command line
            List<String> lines = new ArrayList<String>();
            String line = null;
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(System.in));
            while ((line = lnr.readLine()) != null) {
                lines.add(line);
            }
            m.addClass(lines.toArray(new String[lines.size()]));
        } else {
            //put the target class onto the search list
            m.addClass(input);
        }
        
        m.visitor = new DefaultDependencyVisitor(output);
        m.run();
    }
    
    /**
     * Execute the analysis event loop.  This method is interruptible.  Once
     * an analysis has been run, the internal thread pool is shut down.
     */
    public void run() {
        //Enter the event loop.
        while(! interruptFlag.get()) {
            try {
                String next = discoveries.poll(1, TimeUnit.SECONDS);
                if (next == null) break; //no more elements
                if (next.trim().equals("")) continue;
                pool.execute(new ClassDiscoverer(next));
            } catch (InterruptedException ie) {
                break;
            }
        }
        
        //Terminate any remaining threads and signal shutdown
        visitor.end();
        interruptFlag.set(false);
        pool.shutdown();
    }
    
    /**
     * Analyze dependencies for the entire classpath with the given visitor.
     * @param visitor the DependencyVisitor to use.
     */
    public static void analyze(DependencyVisitor visitor) throws IOException {
        Main m = new Main();
        m.visitor = visitor;
        m.run();
    }
    
    protected void addClasspath() throws IOException {
        //map the classpath and test it
        String[] cpEntries = System.getProperty("java.class.path").split(File.pathSeparator);
        for (String entry : cpEntries) {
            processEntry(entry);
        }        
    }
    
    /**
     * Analyze dependencies for the specified classes.
     * @param visitor the DependencyVisitor to use
     * @param classes the array of classes to test
     */
    public static void analyze(DependencyVisitor visitor, String... classes) {
        Main m = new Main(classes);
        m.visitor = visitor;
        m.run();
    }
    
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
    
    protected void addClass(String... classes) {
        for (String clazz : classes) { discoveries.add(clazz); }
    }
    
    private void processEntry(String entry) throws IOException {
        File entryFile = new File(entry);
        if (!entryFile.exists() || !entryFile.canRead()) return;
        if (entryFile.isDirectory()) {
            processDirectory(entryFile, entryFile);
        } else if (entryFile.isFile()) {
            processJarFile(entryFile);
        }
    }
    
    private void processDirectory(File root, File dir) {
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                //recurse other directories
                processDirectory(root, f);
            } else {
                if (f.getName().endsWith(".class")) {
                    //subtract the local filesystem gunk from the entry
                    //and remove .class at the end
                    String target = f.getAbsolutePath()
                        .substring(root.getAbsolutePath().length())
                        .replace(".class", "");
                    
                    //in case the OS directory separator is still hanging on
                    //the front, get rid of it
                    if (target.startsWith(File.separator)) target = 
                        target.substring(File.separator.length());
                    
                    //switch the OS-specific separator to the one java expects
                    target = target.replace(File.separator, "/");
                    
                    //add to our list of target classes
                    discoveries.add(target);
                }
            }
        }
    }
    
    private void processJarFile(File jarFile) throws IOException {
        JarFile jf = new JarFile(jarFile);
        JarEntry je = null;
        Enumeration<JarEntry> jarEntries = jf.entries();
        while (jarEntries.hasMoreElements()) {
            je = jarEntries.nextElement();
            if (je.getName().endsWith(".class")) {
                discoveries.add(je.getName().replace(".class", ""));
            }
        }
    }
    
    protected void addDescription(String desc) {
        String type = Main.extractClass(desc);
        if (type != null) addType(type);
    }
    
    protected void addType(String type) {
        try {
            discoveries.put(type);
        } catch (InterruptedException ie) {
            interruptFlag.compareAndSet(false, true);
        }
    }
    
    /**
     * Find class references in Annotations.
     */
    class AnnotationReferenceFinder implements AnnotationVisitor {
        public void visit(String arg0, Object arg1) {
            //primitive values - don't need to search
        }

        public AnnotationVisitor visitAnnotation(String name, String desc) {
            addDescription(desc);
            return this;
        }

        public AnnotationVisitor visitArray(String arg0) {
            return this;
        }

        public void visitEnd() {}

        public void visitEnum(String name, String desc, String value) {
            addDescription(desc);
        }
    }
    
    /**
     * Find class references in Field declarations.
     */
    class FieldReferenceFinder implements FieldVisitor {
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            addDescription(desc);
            return ANT_FINDER;
        }

        public void visitAttribute(Attribute arg0) {}
        public void visitEnd() {}
    }
    
    /**
     * Find class references in Method implementations.
     */
    class MethodReferenceFinder implements MethodVisitor {

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            addDescription(desc);
            return ANT_FINDER;
        }

        public AnnotationVisitor visitAnnotationDefault() {
            return ANT_FINDER;
        }

        public void visitAttribute(Attribute arg0) {
            //TODO: needs implementation?
        }

        public void visitCode() {}
        public void visitEnd() {}

        public void visitFieldInsn(int op, String owner, String name, String desc) {
            addType(owner);
            addDescription(desc);
        }

        public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {}
        public void visitIincInsn(int arg0, int arg1) {}
        public void visitInsn(int arg0) {}
        public void visitIntInsn(int arg0, int arg1) {}
        public void visitJumpInsn(int arg0, Label arg1) {}
        public void visitLabel(Label arg0) {}

        public void visitLdcInsn(Object insn) {
            if (insn instanceof Type) {
                addDescription(insn.toString());
            }
        }

        public void visitLineNumber(int arg0, Label arg1) {}

        public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int index) {
            addDescription(desc);
        }

        public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {}
        public void visitMaxs(int arg0, int arg1) {}

        public void visitMethodInsn(int op, String owner, String name, String desc) {
            if (owner.startsWith("[") && owner.charAt(1) != 'L') return;
            if (owner.endsWith(";")) {
                addDescription(desc);
            } else {
                addType(owner);
            }
        }

        public void visitMultiANewArrayInsn(String type, int arg1) {
            addDescription(type);
        }

        public AnnotationVisitor visitParameterAnnotation(int param, String desc, boolean visible) {
            addDescription(desc);
            return ANT_FINDER;
        }

        public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label[] arg3) {}

        public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String type) {
            if (type != null) addType(type);
        }

        public void visitTypeInsn(int arg0, String type) {
            addDescription(type);
        }

        public void visitVarInsn(int arg0, int arg1) {}
    }
    
    /**
     * Find class references in Class descriptions.
     */
    class ClassReferenceFinder implements ClassVisitor {
        public void visit(int ver, int access, String name, String sig, String supr, String[] ifcs) {
            if (supr != null) addType(supr);
            for (String ifc : ifcs) {
                addType(ifc);
            }
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean arg1) {
            addDescription(desc);
            return ANT_FINDER;
        }

        public void visitAttribute(Attribute arg0) {
//          TODO: needs implementation?
        }

        public void visitEnd() {}

        public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
            addDescription(desc);
            return FLD_FINDER;
        }

        public void visitInnerClass(String name, String outer, String inner, int access) {
            if (name != null) addType(name);
            if (outer != null) addType(outer);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] expts) {
            if (expts != null) {
                for (String expt : expts) {
                    addType(expt);
                }
            }
            
            return MTD_FINDER;
        }

        public void visitOuterClass(String owner, String name, String desc) {
            if (owner != null) addType(owner);
        }

        public void visitSource(String arg0, String arg1) {}
    }
    
    /**
     * Basic "bootstrap" class for integration with asm.  Works as a task
     * unit in the thread pool.  Constructs an asm class reader, attempts
     * to read the class name for this task, and reports success or fail
     * to the visitor implementation.
     */
    class ClassDiscoverer implements Runnable {
        private final String next;

        ClassDiscoverer(String next) {
            //homogenize input formats to / format instead of .
            if (next.contains("."))
                this.next = next.replace('.', '/');
            else this.next = next;
        }
        
        public void run() {
            String outForm = next.replace('/', '.');
            try {
                if (analysis.containsKey(next)) return;
                analysis.putIfAbsent(next, false);
                ClassReader cr = new ClassReader(next);
                cr.accept(CLS_FINDER, 0);
                analysis.replace(next, false, true);
                visitor.success(outForm);
            } catch (IOException ioe) {
                //Mark class as processed
                analysis.putIfAbsent(next, false);
                visitor.fail(outForm);
            }
        }
    }
}

/**
 * Default dependency visitor, intended to work with stand-alone execution
 * of the Oops! main program.  Prints output to standard output and/or
 * standard err, depending on command line parameters to main. 
 */
class DefaultDependencyVisitor implements DependencyVisitor {
    private OutputStyle output = OutputStyle.STANDARD;
    public DefaultDependencyVisitor(OutputStyle output) {
        this.output = output;
    }
    public void end() {}

    public void fail(String name) {
        String stdErrMsg = "%s%n";
        String vbsErrMsg = "Fail: %s%n";
        switch (output) {
        case VERBOSE:
            System.out.printf(vbsErrMsg, name);
            break;
        case SPLIT:
            System.err.printf(stdErrMsg, name);
            break;
        default:
            System.out.printf(stdErrMsg, name);
        }
    }

    public void success(String name) {
        switch (output) {
        case VERBOSE:
            System.out.printf("Processing: %s%n", name);
            break;
        case SPLIT:
            System.out.println(name);
            break;
        default:
        }
    }
}

/**
 * Output style types for default dependency visitor.  Used only in
 * standalone execution of Oops! main analysis program.
 */
enum OutputStyle {
    STANDARD,
    VERBOSE,
    SPLIT;
}