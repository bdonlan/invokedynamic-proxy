package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DynamicProxyVisibilityTests {
    @Test(expected = NoSuchMethodException.class)
    public void whenFinalizerNotRequested_itIsNotCreated() throws Exception {
        Class<?> proxy = DynamicProxy.builder().build().proxyClass();

        proxy.getDeclaredMethod("finalize");
    }

    @Test
    public void whenFinalizerNotRequested_itIsCreated() throws Exception {
        Class<?> proxy = DynamicProxy.builder().withFinalizer().build().proxyClass();

        assertNotEquals(Object.class, proxy.getDeclaredMethod("finalize"));
    }

    @Test
    public void whenAllContributorsAreProtected_methodIsProtected() throws Exception {
        Class<?> proxy = DynamicProxy.builder().build().proxyClass();

        Method clone = proxy.getDeclaredMethod("clone");

        assertEquals(Modifier.PROTECTED, clone.getModifiers() & Modifier.PROTECTED);
    }

    public interface PublicClone {
        public Object clone();
    }

    @Test
    public void whenAnyContributorsIsPublic_methodIsPublic() throws Exception {
        Class<?> proxy = DynamicProxy.builder().withInterfaces(PublicClone.class).build().proxyClass();

        Method clone = proxy.getDeclaredMethod("clone");

        assertEquals(Modifier.PUBLIC, clone.getModifiers() & Modifier.PUBLIC);
    }
}
