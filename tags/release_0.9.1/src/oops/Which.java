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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Which works like the unix command "which," except it tells you the
 * location of a loaded Java class.  Use "-" as the only argument to force
 * reading the class list from standard input.
 */
public class Which {
    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            System.err.println("Expecting at least one Java class.");
            System.exit(1);
        }
        
        String[] classList = args;
        
        if (args[0].equals("-")) {
            //read list of classes from command line
            List<String> lines = new ArrayList<String>();
            String line = null;
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(System.in));
            while ((line = lnr.readLine()) != null) {
                lines.add(line);
            }
            classList = lines.toArray(new String[lines.size()]);
        }
        
        Map<String, String> results = locate(classList);
        
        int longest = 0;
        for (String clazz : results.keySet()) {
            if (clazz.length() > longest)
                longest = clazz.length();
        }
        
        for (Entry<String, String> result : results.entrySet()) {
            String location = result.getValue();
            if (location != null)
                System.out.printf("%-" + longest + "s: %s%n", result.getKey(), location);
            else 
                System.out.printf("%-" + longest + "s: not found in class path%n", result.getKey());
        }
    }
    
    /**
     * Search the class path and report the file used to source all the class
     * identifiers in the array/vararg parameter.
     * @param classes an array of class identifiers to process
     * @return a map of class identifiers to their location.  the value
     * will be null if the class could not be loaded
     */
    public static Map<String, String> locate(String... classes) {
        Map<String, String> result = new HashMap<String, String>(classes.length);
        for (String clazz : classes) {
            result.put(clazz, locate(clazz));
        }
        return result;
    }
    
    /**
     * Search the class path and report the file used to source the given
     * class identifier.
     * @param clazz the class to test
     * @return the file system location of the class file, or null if the
     * class could not be loaded
     */
    public static String locate(String clazz) {
        try {
            Class<?> test = Class.forName(clazz);
            URL location = test.getResource(test.getSimpleName() + ".class");
            //Strip out excess information that we don't need to see, such
            //as the jar:file:/ and file:/ protocol strings, and also remove
            //the class file from the end of jar file entries.
            String simpleLocation = location.toExternalForm();
            if (simpleLocation.contains("!"))
                simpleLocation = simpleLocation.substring(0, simpleLocation.indexOf('!'));
            simpleLocation = simpleLocation.substring(simpleLocation.indexOf("/") + 1);
            return simpleLocation;
        } catch (NoClassDefFoundError ncdfe) {
            return null;
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }
}