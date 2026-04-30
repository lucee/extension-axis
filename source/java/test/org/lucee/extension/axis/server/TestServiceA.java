package org.lucee.extension.axis.server;

/**
 * Simple test class that Axis can introspect for service descriptor generation.
 */
public class TestServiceA {
    public String hello(String name) {
        return "Hello " + name;
    }

    public int add(int a, int b) {
        return a + b;
    }
}
