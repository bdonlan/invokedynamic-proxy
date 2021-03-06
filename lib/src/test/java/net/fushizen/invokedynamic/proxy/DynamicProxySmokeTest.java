package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicProxySmokeTest {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public interface TrivialInterface {}

    @Test
    public void testTrivial() throws Throwable {
        MethodHandle handle = DynamicProxy.builder()
                .withInterfaces(TrivialInterface.class)
                .build()
                .constructor();

        assertTrue(handle.invoke() instanceof TrivialInterface);
    }

    public interface OneMethodInterface {
        public void foo();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDefaultHandler() throws Throwable {
        MethodHandle handle = DynamicProxy.builder()
                .withInterfaces(OneMethodInterface.class)
                .build()
                .constructor();

        OneMethodInterface obj = (OneMethodInterface)handle.invoke();

        obj.foo();
    }

    @Test
    public void testCallBinding() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        MethodHandle invokee = LOOKUP.findVirtual(CountDownLatch.class, "countDown", MethodType.methodType(Void.TYPE))
                                     .bindTo(latch);
        MethodHandle boundInvokee = MethodHandles.dropArguments(invokee, 0, Object.class);
        DynamicInvocationHandler handler = (lookup, name, type, superMethod) -> new ConstantCallSite(boundInvokee.asType(type));

        DynamicProxy proxy = DynamicProxy.builder()
                .withInterfaces(OneMethodInterface.class)
                .withInvocationHandler(handler)
                .build();
        ((OneMethodInterface)proxy.supplier().get()).foo();

        assertEquals(0, latch.getCount());
    }

    @Test
    public void testCustomPackage() throws Exception {
        Object o = DynamicProxy.builder().withPackageName("a.b.c.d").build().supplier().get();

        Class<?> aClass = o.getClass();
        assertEquals("a.b.c.d", aClass.getName().replaceFirst("\\.DynamicProxy.*", ""));
    }

    @Test
    public void testCustomName() throws Exception {
        Object o = DynamicProxy.builder().withClassName("ThisIsATestClassName").build().supplier().get();

        assertTrue(o.getClass().getName().endsWith(".ThisIsATestClassName"));
    }
}
