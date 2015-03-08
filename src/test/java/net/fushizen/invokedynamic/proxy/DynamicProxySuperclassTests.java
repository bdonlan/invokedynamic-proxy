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
        I1 obj = (I1) DynamicProxy.builder().withInterfaces(I1.class, I3.class).build().constructor().invoke();

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
        I1 obj = (I1) DynamicProxy.builder().withSuperclass(Inheritor.class).build().constructor().invoke();

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
        I1 obj = (I1) DynamicProxy.builder().withSuperclass(Inheritor2.class).build().constructor().invoke();

        assertEquals("baz", obj.foo());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenSuperclassAndUnrelatedDefaultPresent_callIsAmbiguous() throws Throwable {
        I1 obj = (I1) DynamicProxy.builder().withSuperclass(Inheritor2.class).withInterfaces(I2.class).build().constructor().invoke();

        obj.foo();
    }
}
