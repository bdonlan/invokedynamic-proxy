package net.fushizen.invokedynamic.proxy;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;

import static org.junit.Assert.assertTrue;

/**
 * Created by bd on 3/7/15.
 */
public class ProxyTest {
    public interface TrivialInterface {}

    @Test
    public void testTrivial() throws Throwable {
        MethodHandle handle = DynamicProxy.builder(TrivialInterface.class)
                .build()
                .constructor();

        assertTrue(handle.invoke() instanceof TrivialInterface);
    }

    public static void main(String[] args) throws Throwable {
        ClassReader reader = new ClassReader("net/fushizen/invokedynamic/proxy/ProxyTest");
        reader.accept(new TraceClassVisitor(new ClassWriter(0), new ASMifier(), new PrintWriter(System.out)), 0);
    }
}
