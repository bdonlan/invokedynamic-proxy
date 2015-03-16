package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

public class ErrorCaseTests {
    @Test(expected = IllegalArgumentException.class)
    public void whenSuperclassIsAnInterface_throws() throws Exception {
        DynamicProxy.builder().withSuperclass(Runnable.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenInterfaceIsnt_throws() throws Exception {
        DynamicProxy.builder().withInterfaces(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSuperclassIsFinal_throws() throws Exception {
        DynamicProxy.builder().withSuperclass(String.class);
    }
}
