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
import java.util.List;

/**
 * Which works like the unix command "which," except it tells you the
 * location of a loaded Java class.  Use "-" as the only argument to force
 * reading the class list from standard input.
 * @author gvanore
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
        
        int longest = 0;
        for (String clazz : classList) {
            try {
                Class.forName(clazz);
            } catch (ClassNotFoundException cnfe) {
                continue;
            } catch (NoClassDefFoundError ncdfe) {
                continue;
            }
            if (clazz.length() > longest)
                longest = clazz.length();
        }
        
        for (String clazz : classList) {
            try {
                Class test = Class.forName(clazz);
                URL location = test.getResource(test.getSimpleName() + ".class");
                
                //Strip out excess information that we don't need to see, such
                //as the jar:file:/ and file:/ protocol strings, and also remove
                //the class file from the end of jar file entries.
                String simpleLocation = location.toExternalForm();
                if (simpleLocation.contains("!"))
                    simpleLocation = simpleLocation.substring(0, simpleLocation.indexOf('!'));
                simpleLocation = simpleLocation.substring(simpleLocation.indexOf("/") + 1);
                
                System.out.printf("%-" + longest + "s: %s%n", clazz, simpleLocation);
            } catch (ClassNotFoundException cnfe) {
                System.err.printf("Class %s not found in class path.%n", clazz);
            } catch (NoClassDefFoundError ncdfe) {
                System.err.printf(
                    "Error loading %s; could not resolve dependency %s%n", 
                    clazz, ncdfe.getMessage().replace('/', '.'));
            }
        }
    }
}
