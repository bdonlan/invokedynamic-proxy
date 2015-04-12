package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.lang.invoke.MethodHandle;

import static org.junit.Assert.assertEquals;

public class ArbitraryConstructorTests {
    public static class Superclass {
        final boolean b;
        final char c;
        final short s;
        final int i;
        final long l;
        final float f;
        final double d;
        final Object o;

        public Superclass(boolean b, char c, short s, int i, long l, float f, double d, Object o) {
            this.b = b;
            this.c = c;
            this.s = s;
            this.i = i;
            this.l = l;
            this.f = f;
            this.d = d;
            this.o = o;
        }
    }

    @Test
    public void testArbitraryConstructor() throws Throwable {
        MethodHandle ctor = DynamicProxy.builder()
                .withSuperclass(Superclass.class)
                .withConstructor(
                        Boolean.TYPE,
                        Character.TYPE,
                        Short.TYPE,
                        Integer.TYPE,
                        Long.TYPE,
                        Float.TYPE,
                        Double.TYPE,
                        Object.class
                )
                .build()
                .constructor();

        Superclass obj = (Superclass)(Object)ctor.invoke(
                true,
                'a',
                (short)1234,
                0x7FFFFFFF,
                0x1_0000_0000L,
                0.123f,
                0.555d,
                "hello, world"
        );

        assertEquals(true, obj.b);
        assertEquals('a', obj.c);
        assertEquals(1234, obj.s);
        assertEquals(0x7FFFFFFF, obj.i);
        assertEquals(0x1_0000_0000L, obj.l);
        assertEquals(0.123f, obj.f, 0.01);
        assertEquals(0.555d, obj.d, 0.01);
        assertEquals("hello, world", obj.o);
    }
}
