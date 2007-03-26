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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class Main {
    private static final BlockingQueue<String> discoveries = new LinkedBlockingQueue<String>();
    private static final ConcurrentMap<String, Boolean> analysis = new ConcurrentHashMap<String, Boolean>();
    private static final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
    
    private static final Pattern CLSID = Pattern.compile("\\[*?L(.*?(/.*?)*);");
    
    protected static final ClassVisitor FINDER = new ClassReferenceFinder();
    
    private static boolean VERBOSE = false;
    
    public static String extractClass(String desc) {
        Matcher m = CLSID.matcher(desc);
        if (m.matches()) {
            if (m.group(1).length() == 1) return null;
            return m.group(1);
        }
        return null;
    }
    
    public static void main(String... args) throws IOException {
        //Process command arguments.
        processArgs(args);

        //Enter the event loop.
        while(true) {
            try {
                String next = discoveries.poll(1, TimeUnit.SECONDS);
                if (next == null) break; //no more elements
                if (next.trim().equals("")) continue;
                pool.execute(new ClassDiscoverer(next));
            } catch (InterruptedException ie) {
                break;
            }
        }
        
        //Terminate any remaining threads and exit the JVM.
        pool.shutdown();
    }
    
    private static void processArgs(String... args) throws IOException {
        String input = null;
        if (args.length > 0) {
            for (String arg : args) {
                if (arg.equals("-v") || arg.equals("--verbose")) {
                    VERBOSE = true;
                } else {
                    input = arg;
                }
            }
        }
        
        if (input == null) {
            //map the classpath and test it
            String[] cpEntries = System.getProperty("java.class.path").split(File.pathSeparator);
            for (String entry : cpEntries) {
                processEntry(entry);
            }
       } else if (input.equals("-")) {
            //read list of classes from command line
            List<String> lines = new ArrayList<String>();
            String line = null;
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(System.in));
            while ((line = lnr.readLine()) != null) {
                lines.add(line);
            }
            for (String t : lines) { discoveries.add(t); }
        } else {
            //put the target class onto the search list
            discoveries.add(input);
        }        
    }
    
    private static void processEntry(String entry) throws IOException {
        File entryFile = new File(entry);
        if (!entryFile.exists() || !entryFile.canRead()) return;
        if (entryFile.isDirectory()) {
            processDirectory(entryFile, entryFile);
        } else if (entryFile.isFile()) {
            processJarFile(entryFile);
        }
    }
    
    private static void processDirectory(File root, File dir) {
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
    
    private static void processJarFile(File jarFile) throws IOException {
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
    
    
    static class MethodReferenceFinder implements MethodVisitor {

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            String clazz = Main.extractClass(desc);
            try {
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
            return null;
        }

        public AnnotationVisitor visitAnnotationDefault() {
            return null;
        }

        public void visitAttribute(Attribute arg0) {
            System.out.printf("Method Attribute: %s%n", arg0.toString());
        }

        public void visitCode() {}
        public void visitEnd() {}

        public void visitFieldInsn(int op, String owner, String name, String desc) {
            //System.out.printf("Method Field Insn: %s, owner %s, desc %s%n", name, owner, desc);
            try {
                String clazz = Main.extractClass(desc);
                discoveries.put(owner);
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
        }

        public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
            // TODO Auto-generated method stub            
        }

        public void visitIincInsn(int arg0, int arg1) {}
        public void visitInsn(int arg0) {}
        public void visitIntInsn(int arg0, int arg1) {}
        public void visitJumpInsn(int arg0, Label arg1) {}
        public void visitLabel(Label arg0) {}

        public void visitLdcInsn(Object arg0) {
            if (arg0 instanceof Type) {
                String clazz = Main.extractClass(arg0.toString().trim());
                try {
                    if (clazz != null) discoveries.put(clazz);
                } catch (InterruptedException ie) {}
            }
        }

        public void visitLineNumber(int arg0, Label arg1) {}

        public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int index) {
            String clazz = Main.extractClass(desc);
            try {
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
        }

        public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {}
        public void visitMaxs(int arg0, int arg1) {}

        public void visitMethodInsn(int op, String owner, String name, String desc) {
            try {
                if (owner.startsWith("[") && owner.charAt(1) != 'L') return;
                String clazz = Main.extractClass(owner);
                discoveries.put((owner.endsWith(";")) ? clazz : owner);
            } catch (InterruptedException ie) {}
        }

        public void visitMultiANewArrayInsn(String type, int arg1) {
            String clazz = Main.extractClass(type);
            try {
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
        }

        public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1, boolean arg2) {
            System.out.printf("paramAnnotation: %s%n", arg1);
            return null;
        }

        public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label[] arg3) {}

        public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String type) {
            try {
                if (type != null) discoveries.put(type);
            } catch (InterruptedException ie) {}
        }

        public void visitTypeInsn(int arg0, String type) {
            try {
                String clazz = Main.extractClass(type);
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
        }

        public void visitVarInsn(int arg0, int arg1) {}
    }
    
    static class ClassReferenceFinder implements ClassVisitor {
        public void visit(int ver, int access, String name, String sig, String supr, String[] ifcs) {
            try {
                if (supr != null) discoveries.put(supr);
            } catch (InterruptedException ie) {}
            for (String ifc : ifcs) {
                try {
                    discoveries.put(ifc);
                } catch (InterruptedException ie) {}
            }
        }

        public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
            String clazz = Main.extractClass(arg0);
            try {
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
            return null;
        }

        public void visitAttribute(Attribute arg0) {
            System.out.println("Attribute: " + arg0);
        }

        public void visitEnd() {}

        public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
            String clazz = Main.extractClass(desc);            
            try {
                if (clazz != null) discoveries.put(clazz);
            } catch (InterruptedException ie) {}
           
            return null;
        }

        public void visitInnerClass(String name, String outer, String inner, int access) {
            try {
                if (name != null) discoveries.put(name);
                if (outer != null) discoveries.put(outer);
                //System.out.printf("%s : %s%n", name, outer);
            } catch (InterruptedException ie) {}
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] expts) {
            //System.out.printf("Method: %s, desc %s, sig %s%n", name, desc, sig);
            if (expts != null) {
                for (String expt : expts) {
                    try {
                        discoveries.put(expt);
                    } catch (InterruptedException ie) {}
                }
            }
            
            return new MethodReferenceFinder();
        }

        public void visitOuterClass(String owner, String name, String desc) {
            try {
                if (owner != null) discoveries.put(owner);
            } catch (InterruptedException ie) {}
        }

        public void visitSource(String arg0, String arg1) {}
    }
    
    static class ClassDiscoverer implements Runnable {
        private final String next;
        ClassDiscoverer(String next) {
            this.next = next;
        }
        public void run() {
            try {
                if (analysis.containsKey(next)) return;
                analysis.putIfAbsent(next, false);
                if (VERBOSE) System.out.printf("Processing: %s%n", next);
                ClassReader cr = new ClassReader(next);
                cr.accept(FINDER, 0);
                analysis.replace(next, false, true);                    
            } catch (IOException ioe) {
                //Set up output formatting
                String stdErrMsg = "%s%n";
                String vbsErrMsg = "Fail: %s%n";
                String errMsg = VERBOSE ? vbsErrMsg : stdErrMsg;
                
                System.out.printf(errMsg, next.replace('/', '.'));
                analysis.putIfAbsent(next, false);
            }
        }
    }
}



