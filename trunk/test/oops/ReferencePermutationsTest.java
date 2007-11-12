package oops;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import oops.util.ConcurrentDependencyVisitor;

/**
 * Test that all permutations of ways to stuff object references can be discovered
 * by the oops Analyzer.
 */
public class ReferencePermutationsTest {
    public static void main(String... args) throws Exception {
        new ReferencePermutationsTest().testPermutations();
    }
    
    @Test
    public void testPermutations() throws Exception {
        ConcurrentDependencyVisitor cdv = new ConcurrentDependencyVisitor();
        Analyzer.analyze(cdv, "oops.Permutations");
        Set<String> success = cdv.getSuccesses();
        List<String> expectedResults = Arrays.asList(new String[] {
            "oops.annotations.EnumInAnnotation",
            "oops.annotations.TestAnnotationAnnotation",
            "oops.annotations.TestMemberAnnotation",
            "oops.annotations.TestMethodAnnotation",
            "oops.annotations.TestTypeAnnotation",
            "oops.ClassArrayInAnnotation",
            "oops.FieldTypeReference",
            "oops.MethodImplementationReference",
            "oops.MethodParameterReference",
            "oops.ReturnTypeReference",
            "oops.TypeInEnumeration",
            "oops.Enumeration",
            "oops.annotations.TestAnnotationAnnotation$NestedAnnotation",
            "oops.Permutations",
            "oops.Permutations$InnerClass"
        });
        
        for (String result : expectedResults) {
            System.out.print("Testing " + result + "... ");
            boolean found = success.contains(result);
            System.out.println(found);
            Assert.assertTrue(found);
        }
    }
}
