package oops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import oops.util.ConcurrentDependencyVisitor;

/**
 * Test that all permutations of ways to stuff object references can be discovered
 * by the oops Analyzer.
 */
public class ReferencePermutationsTest {
    /*
     * For now we'll just make this a main class rather than depend on Junit.
     */
    public static void main(String... args) throws Exception {
        ConcurrentDependencyVisitor cdv = new ConcurrentDependencyVisitor();
        Analyzer.analyze(cdv, "oops.Permutations");
        Set<String> success = cdv.getSuccesses();
        List<String> expectedResults = Arrays.asList(new String[] {
            "oops.annotations.EnumInAnnotation",
            "oops.annotations.TestAnnotation",
            "oops.FieldTypeReference",
            "oops.MethodImplementationReference",
            "oops.MethodParameterReference",
            "oops.ReturnTypeReference",
            "oops.TypeInEnumeration",
            "oops.Enumeration"
        });
        
        for (String result : expectedResults) {
            if (!success.contains(result)) {
                System.out.println("[FAIL] " + result + " not found in result set!");
            }
        }
    }
}
