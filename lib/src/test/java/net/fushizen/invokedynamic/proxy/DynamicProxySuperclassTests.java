package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.Assert.assertEquals;

public class DynamicProxySuperclassTests {
    @Test
    public void whenNoHandlerInstalled_canCallToString() throws Throwable {
        DynamicProxy.builder().build().constructor().invoke().toString();
    }

    public interface I1 {
        default String foo() { return "foo"; }
    }

    @Test
    public void whenDefaultImplementationAvailable_itIsDelegatedTo() throws Throwable {
        I1 obj = (I1) DynamicProxy.builder().withInterfaces(I1.class).build().constructor().invoke();

        assertEquals("foo", obj.foo());
    }

    @Test
    public void whenDefaultImplementationAvailable_itCanBeOverridden() throws Throwable {
        DynamicInvocationHandler handler = (lookup, name, type, superMethod) -> {
            MethodHandle h;
            if (name.equals("foo")) {
                h = MethodHandles.dropArguments(
                        MethodHandles.constant(String.class, "bar"),
                        0,
                        Object.class
                ).asType(type);
            } else {
                h = superMethod.asType(type);
            }

            return new ConstantCallSite(h);
        };

        I1 obj = (I1) DynamicProxy.builder()
                .withInvocationHandler(handler)
                .withInterfaces(I1.class)
                .build()
                .constructor()
                .invoke();

        assertEquals("bar", obj.foo());
    }

    public interface I2 {
        default String foo() { return "foo"; }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenCallAmbiguous_noSuperAvailable() throws Throwable {
        I1 obj = (I1) DynamicProxy.builder().withInterfaces(I1.class, I2.class).build().constructor().invoke();

        obj.foo();
    }

    public interface I3 {
        String foo();
    }

    @Test
    public void whenAbstractVariantsAvailable_callIsNotAmbiguous() throws Throwable {
        I1 obj = DynamicProxy.builder().withInterfaces(I1.class, I3.class).build().construct();

        obj.foo();
    }

    public static abstract class Inheritor implements I1 {
        @Override
        public String foo() {
            return "bar";
        }
    }

    @Test
    public void whenSuperclassOverridesDefault_callIsNotAmbiguous() throws Throwable {
        I1 obj = DynamicProxy.builder().withSuperclass(Inheritor.class).build().construct();

        assertEquals("bar", obj.foo());
    }

    public static abstract class Inheritor2 extends Inheritor {
        @Override
        public String foo() {
            return "baz";
        }
    }

    @Test
    public void whenSuperclassOverridesSupersuper_callIsNotAmbiguous() throws Throwable {
        I1 obj = DynamicProxy.builder().withSuperclass(Inheritor2.class).build().construct();

        assertEquals("baz", obj.foo());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenSuperclassAndUnrelatedDefaultPresent_callIsAmbiguous() throws Throwable {
        I1 obj = DynamicProxy.builder().withSuperclass(Inheritor2.class).withInterfaces(I2.class).build().construct();

        obj.foo();
    }

    public static abstract class Inheritor3 implements I3 {

    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenSuperclassImplementsInterface_interfaceMethodsAreProxied() throws Throwable {
        Inheritor3 obj = DynamicProxy.builder().withSuperclass(Inheritor3.class).build().construct();

        obj.foo();
    }

    public interface IFaceA {
        // MethodHandles.constant does not support primitive void, so use Object here to make the handler simpler
        public Object a();
    }

    public interface IFaceB {
        public Object b();
    }

    public interface IFaceC extends IFaceA, IFaceB {}

    @Test
    public void whenInterfacesHaveMultipleInheritance_interfaceMethodsAreProxied() throws Throwable {
        DynamicInvocationHandler handler = (lookup, name, type, supermethod) -> new ConstantCallSite(
                MethodHandles.dropArguments(
                        MethodHandles.constant(type.returnType(), null),
                        0,
                        type.parameterList()
                )
        );

        IFaceC obj = DynamicProxy.builder()
                .withInvocationHandler(handler)
                .withInterfaces(IFaceC.class)
                .build()
                .construct();

        obj.a();
        obj.b();
    }

    @Test
    public void indirectSuperinterfaceDefaultMethodTest() throws Throwable {
        C c = (C) DynamicProxy.builder()
                .withSuperclass(C.class)
                .withInterfaces(B.class)
                .withInvocationHandler((lookup, name, superType, superMethod) -> new ConstantCallSite(superMethod.asType(superType)))
                .build()
                .supplier()
                .get();
        c.method();
    }

    interface A { default void method() { } }
    interface B extends A {}
    public abstract static class C implements B {
    }
}
