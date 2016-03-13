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
    public void whenShadowedSupermethodIsCalled_superMethodHandleIsAvailable() throws Exception {
        Super3 obj = (Super3)DynamicProxy.builder().withInterfaces(SuperSuper.class).build().construct();

        assertEquals(3, obj.method());
    }

    public interface Super3 {
        default int method() { return 3; }
    }

    public static interface SuperSuper extends Super3 {
        default int method() { return 3; }
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
    public void whenDefaultMethodIsAccessibleThroughSuperclass_noErrorThrown() throws Throwable {
        B b = (B) DynamicProxy.builder()
                .withSuperclass(C.class)
                .withInterfaces(B.class)
                .withInvocationHandler((lookup, name, superType, superMethod) -> new ConstantCallSite(superMethod.asType(superType)))
                .build()
                .supplier()
                .get();
        b.method();
    }

    @Test
    public void whenDefaultMethodIsAccessibleThroughSuperinterface_noErrorThrown() throws Throwable {
        B b = (B) DynamicProxy.builder()
                .withInterfaces(B.class)
                .withInvocationHandler((lookup, name, superType, superMethod) -> new ConstantCallSite(superMethod.asType(superType)))
                .build()
                .supplier()
                .get();
        b.method();
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenDefaultMethodProviderIsInaccessible_exceptionThrown() throws Exception {
        DynamicProxy.builder()
                .withInterfaces(PB.class)
                .withInvocationHandler((lookup, name, superType, superMethod) -> new ConstantCallSite(superMethod.asType(superType)))
                .build();
    }

    @Test
    public void whenDefaultMethodProviderIsInaccessible_andPackageSet_noExceptionThrown() throws Exception {
        DynamicProxy.builder()
                .withInterfaces(PB.class)
                .withInvocationHandler((lookup, name, superType, superMethod) -> new ConstantCallSite(superMethod.asType(superType)))
                .withPackageName(A.class.getPackage().getName())
                .build();
    }

    @Test
    public void whenDefaultMethodProviderIsInaccessible_andPackageSetImplicitly_noExceptionThrown() throws Exception {
        DynamicProxy.builder()
                .withInterfaces(PB.class, D.class)
                .withInvocationHandler((lookup, name, superType, superMethod) -> new ConstantCallSite(superMethod.asType(superType)))
                .build();
    }

    interface A { default void method() { } }
    interface B extends A {}
    public abstract static class C implements B {
    }
    public interface PB extends A {};
    interface D {}
}
