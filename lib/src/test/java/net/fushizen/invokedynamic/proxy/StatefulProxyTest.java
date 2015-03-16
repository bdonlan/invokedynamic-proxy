package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static org.junit.Assert.assertEquals;

public class StatefulProxyTest {
    public abstract static class StateSuperclass {
        private int counter;

        protected void invokeIncrement() {
            counter++;
        }

        protected int invokeGet() {
            return counter;
        }
    }

    public interface Counter {
        public void increment();
        public int get();
    }

    @Test
    public void testStatefulProxy() throws Exception {
        DynamicInvocationHandler handler = (lookup, name, type, superMethod) -> {
            Class<?> proxyType = type.parameterArray()[0];
            MethodHandle mh;

            if (superMethod != null) {
                // If there's a supermethod implementation, this is probably some Object method. Pass it through to the
                // superclass so our IDE doesn't have problems doing toString or something.

                // Note that we call asType to coerce the methodhandle type - this is because the supermethod probably
                // wants Object as its first argument, while we'll be getting proxyType, and invokedynamic demands that
                // we have an exact type match.
                mh = superMethod.asType(type);

                // The Counter methods have no concrete supermethod, so we'll fall through to one of these branches:
            } else if (name.equals("increment")) {
                mh = lookup.findSpecial(
                        StateSuperclass.class,
                        "invokeIncrement",
                        MethodType.methodType(Void.TYPE),
                        proxyType
                ).asType(type); // downcast the 'this' parameter
            } else if (name.equals("get")) {
                mh = lookup.findSpecial(
                        StateSuperclass.class,
                        "invokeGet",
                        MethodType.methodType(Integer.TYPE),
                        proxyType
                ).asType(type); // downcast the 'this' parameter
            } else {
                // How did we get here? Oh well, have a LinkageError.
                throw new UnsupportedOperationException();
            }

            return new ConstantCallSite(mh);
        };

        DynamicProxy proxy = DynamicProxy.builder()
                .withSuperclass(StateSuperclass.class)
                .withInterfaces(Counter.class)
                .withInvocationHandler(handler)
                .build();

        Counter counter1 = (Counter)proxy.supplier().get();
        Counter counter2 = (Counter)proxy.supplier().get();

        counter1.increment();

        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());
    }


}
