package oops;

import oops.annotations.TestAnnotation;

/**
 * The goal of this class is to have every possible permutation of
 * class reference location.  We can then run tests against this class.
 * @author gvanore
 */
@TestAnnotation
public class Permutations {
    FieldTypeReference fieldRef;

    public ReturnTypeReference someMethod() {
        //who cares?
        return new ReturnTypeReference();
    }
    
    public void anotherMethod(MethodParameterReference theRef) {
        //no op
    }
    
    public void doSomething() {
        MethodImplementationReference mir = new MethodImplementationReference();
        mir.activate();
    }
}
