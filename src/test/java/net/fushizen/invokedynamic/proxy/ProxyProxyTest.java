package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.lang.invoke.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * Example of how to implement the classic Java Proxy on top of invokedynamic-proxy
 */
public class ProxyProxyTest {
    public interface Interface {
        public void foo();
        public void bar();
    }

    public static abstract class ProxyBase {
        private InvocationHandler invocationHandler;

        public void $$init(InvocationHandler handler) {
            this.invocationHandler = handler;
        }

        public Object $$invoke(Method method, Object[] args) throws Throwable {
            return invocationHandler.invoke(this, method, args);
        }
    }

    private static CallSite bindProxyCall(MethodHandles.Lookup lookup, String name, MethodType type, MethodHandle superMethod) throws Throwable {
        if (name.equals("$$init")) {
            return new ConstantCallSite(superMethod.asType(type));
        }

        Class<?> proxyType = type.parameterType(0);
        Method m = findMethod(proxyType, name, type.dropParameterTypes(0, 1));

        MethodHandle invoker = lookup.findSpecial(
                ProxyBase.class,
                "$$invoke",
                MethodType.methodType(Object.class, Method.class, Object[].class),
                proxyType
                );
        invoker = MethodHandles.insertArguments(invoker, 1, m);
        invoker = invoker.asVarargsCollector(Object[].class);
        invoker = invoker.asType(type);

        return new ConstantCallSite(invoker);
    }

    private static Method findMethod(Class<?> proxyType, String name, MethodType type) throws Throwable {
        try {
            return proxyType.getMethod(name, type.parameterArray());
        } catch (NoSuchMethodException e) {
            // continue
        }

        Method m;
        if (proxyType.getSuperclass() != null && proxyType != Object.class) {
            m = findMethod(proxyType.getSuperclass(), name, type);
            if (m != null) return m;
        }

        for (Class<?> iface : proxyType.getInterfaces()) {
            m = findMethod(iface, name, type);
            if (m != null) return m;
        }

        return null;
    }

    @Test
    public void testProxyInvocation() throws Exception {
        DynamicProxy proxy = DynamicProxy.builder()
                .withInterfaces(Interface.class)
                .withSuperclass(ProxyBase.class)
                .withInvocationHandler(ProxyProxyTest::bindProxyCall)
                .build();
        // Cache the proxy object! Constructing a new one is extremely expensive

        CountDownLatch latch = new CountDownLatch(1);
        InvocationHandler handler = (proxyInstance, method, args) -> {
            if (!method.getName().equals("foo")) throw new UnsupportedOperationException();
            latch.countDown();

            return null;
        };

        Interface obj = (Interface)proxy.supplier().get();
        ((ProxyBase)obj).$$init(handler);

        obj.foo();

        assertEquals(0, latch.getCount());
    }
}
