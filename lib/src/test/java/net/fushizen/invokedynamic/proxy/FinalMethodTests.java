package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

public class FinalMethodTests {
    @Test
    public void overrideAndFinalizeMethod() throws Exception {
        B construct = DynamicProxy.builder()
            .withSuperclass(B.class)
            .withInvocationHandler((proxyLookup, methodName, methodType, superMethod) -> null)
            .build()
            .construct(); // no VerifyError
        construct.finalMethod();
    }

    public static class A {
        protected void finalMethod() {}
    }

    public static class B extends A {
        @Override protected final void finalMethod() {}
    }
}
