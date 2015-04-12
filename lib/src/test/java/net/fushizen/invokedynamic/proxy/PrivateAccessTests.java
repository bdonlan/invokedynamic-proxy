package net.fushizen.invokedynamic.proxy;

import net.fushizen.invokedynamic.proxy.otherpackage.Helper;
import org.junit.Test;

interface PackagePrivateInterface {}

class PackagePrivateSuperclass {}

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

    @Test
    public void testPackagePrivateSupers() throws Exception {
        DynamicProxy.builder().withSuperclass(PackagePrivateSuperclass.class).build();
        DynamicProxy.builder().withInterfaces(PackagePrivateInterface.class).build();
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongPackagePrivateSupers_wrongCustomPackageAfter() throws Exception {
        DynamicProxy.builder().withSuperclass(PackagePrivateSuperclass.class).withPackageName("a.b.c.d");
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongPackagePrivateSupers_wrongCustomPackageBefore() throws Exception {
        DynamicProxy.builder().withPackageName("a.b.c.d").withSuperclass(PackagePrivateSuperclass.class);
    }

    @Test
    public void testWrongPackagePrivateSupers_correctCustomPackage() throws Exception {
        DynamicProxy.builder().withPackageName("net.fushizen.invokedynamic.proxy").withSuperclass(PackagePrivateSuperclass.class);
    }
}
