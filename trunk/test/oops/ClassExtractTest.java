package oops;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class ClassExtractTest {
    @Test
    public void testExtractClass() {
        String[] testInputs = {
            "[Ljava.lang.String;",
            "Ljava.lang.Object;",
            "[[[Ljava.util.List;",
            "doNotMatch"
        };
        
        String[] outputs = {
            "java.lang.String",
            "java.lang.Object",
            "java.util.List",
            null
        };
        
        for (int i = 0; i < testInputs.length; ++i) {
            String output = outputs[i];
            String result = Analyzer.extractClass(testInputs[i]);
            if (output != null) Assert.assertTrue(output.equals(result));
            else Assert.assertTrue(result == null);
        }
    }
    
    @Test
    public void testExtractMethodClasses() {
        String[] testInputs = {
            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;",
            "()V",
            "(Ljava/lang/String;)Z",
            "()Ljava/lang/String;",
            "(IILjava/lang/Object;)V",
            "(Ljava/lang/Object;II)V"
        };
        
        String[][] outputs = {
            new String[] {"java/lang/Enum", "java/lang/Class", "java/lang/String"},
            new String[] {null},
            new String[] {null, "java/lang/String"},
            new String[] {"java/lang/String"},
            new String[] {null, "java/lang/Object"},
            new String[] {null, "java/lang/Object", null}
        };
        
        for (int i = 0; i < testInputs.length; ++i) {
            String[] output = outputs[i];
            String[] results = Analyzer.extractMethodClasses(testInputs[i]);
            System.out.println("Test " + (i + 1) + ":");
            System.out.println("\tRequired: " + Arrays.toString(output));
            System.out.println("\tReceived: " + Arrays.toString(results));
            System.out.println();
            Assert.assertTrue(results.length == output.length);
            for (int j = 0; j < results.length; ++j) {
                if (output[j] != null)
                    Assert.assertTrue(output[j].equals(results[j]));
                else
                    Assert.assertTrue(results[j] == null);
            }
        }
    }
}
