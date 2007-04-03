package oops;

public interface DependencyVisitor {
    void success(String name);
    void fail(String name);
    void end();
}
