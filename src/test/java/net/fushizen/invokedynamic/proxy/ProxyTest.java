package net.fushizen.invokedynamic.proxy;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by bd on 3/7/15.
 */
public class ProxyTest {
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
        DynamicInvocationHandler handler = (lookup, name, type) -> new ConstantCallSite(boundInvokee.asType(type));

        DynamicProxy proxy = DynamicProxy.builder()
                .withInterfaces(OneMethodInterface.class)
                .withInvocationHandler(handler)
                .build();
        ((OneMethodInterface)proxy.constructor().invoke()).foo();

        assertEquals(0, latch.getCount());
    }
}
