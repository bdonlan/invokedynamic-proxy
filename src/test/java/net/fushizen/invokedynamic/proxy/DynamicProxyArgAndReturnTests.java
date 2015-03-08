package net.fushizen.invokedynamic.proxy;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DynamicProxyArgAndReturnTests {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public interface ReturnTester {
        public boolean returnsBool();
        public char returnsChar();
        public short returnsShort();
        public int returnsInt();
        public long returnsLong();
        public float returnsFloat();
        public double returnsDouble();
        public String returnsString();
    }

    @Test
    public void testReturns() throws Throwable {
        DynamicInvocationHandler handler = (lookup, name, type, superMethod) -> {
            Object rv;

            switch (name) {
                case "returnsBool": rv = Boolean.TRUE; break;
                case "returnsChar": rv = (char) 42; break;
                case "returnsShort": rv = (short) 42; break;
                case "returnsInt": rv = 42; break;
                case "returnsLong": rv = (long) 42; break;
                case "returnsFloat": rv = (float) 42; break;
                case "returnsDouble": rv = (double) 42; break;
                case "returnsString": rv = "foo"; break;
                default:
                    throw new AssertionError();
            }

            MethodHandle handle = MethodHandles.constant(type.returnType(), rv);
            handle = MethodHandles.dropArguments(handle, 0, type.parameterList());

            return new ConstantCallSite(handle);
        };

        MethodHandle ctor = DynamicProxy.builder()
                .withInterfaces(ReturnTester.class)
                .withInvocationHandler(handler)
                .build()
                .constructor();

        ReturnTester iface = (ReturnTester)ctor.invoke();

        assertEquals(true, iface.returnsBool());
        assertEquals(42, iface.returnsChar());
        assertEquals(42, iface.returnsShort());
        assertEquals(42, iface.returnsInt());
        assertEquals(42, iface.returnsLong());
        assertEquals(42, iface.returnsFloat(), 0.1);
        assertEquals(42, iface.returnsDouble(), 0.1);
        assertEquals("foo", iface.returnsString());
    }

    public interface ArgTester {
        public int foo(boolean b, char c, short s, int i, long l, float f, double d, Object o);
    }

    @Test
    public void testArgs() throws Throwable {
        ArgTester recipient = Mockito.mock(ArgTester.class);
        MethodHandle handleInvokee = MethodHandles.dropArguments(
                LOOKUP.findVirtual(ArgTester.class, "foo",
                        MethodType.methodType(Integer.TYPE,
                                Boolean.TYPE,
                                Character.TYPE,
                                Short.TYPE,
                                Integer.TYPE,
                                Long.TYPE,
                                Float.TYPE,
                                Double.TYPE,
                                Object.class
                                )).bindTo(recipient),
                0,
                Object.class
        );

        DynamicInvocationHandler handler = (lookup, name, type, superMethod) -> new ConstantCallSite(handleInvokee.asType(type));

        MethodHandle ctor = DynamicProxy.builder()
                .withInterfaces(ArgTester.class)
                .withInvocationHandler(handler)
                .build()
                .constructor();

        ArgTester proxy = (ArgTester)ctor.invoke();

        when(recipient.foo(true, 'c', (short)42, 24, 1234, 1, 2, "blah")).thenReturn(999);

        assertEquals(999, proxy.foo(true, 'c', (short)42, 24, 1234, 1, 2, "blah"));
    }
}
