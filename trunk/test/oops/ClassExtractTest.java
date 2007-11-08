package oops;

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
            if (output != null) assert output.equals(result);
            else assert (result == null);
        }
    }
}
