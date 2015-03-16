package net.fushizen.invokedynamic.proxy;

import net.fushizen.invokedynamic.proxy.otherpackage.Helper;
import org.junit.Test;

public class PrivateAccessTests {
    protected interface ProtectedInterface {
    }

    @Test
    public void testProtectedInterface() throws Exception {
        DynamicProxy.builder().withInterfaces(ProtectedInterface.class).build().supplier().get();
    }

    protected static class Superclass {}

    @Test
    public void testProtectedSuperclass() throws Throwable {
        DynamicProxy.builder().withSuperclass(Superclass.class).build().supplier().get();
    }

    @Test
    public void testProtectedSuperclassOtherPackage() throws Exception {
        DynamicProxy.builder().withSuperclass(Helper.getSuperclass()).build().supplier().get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultiplePackageProtectedClasses() throws Exception {
        DynamicProxy.builder().withSuperclass(Helper.getSuperclass()).withInterfaces(ProtectedInterface.class);
    }
}
