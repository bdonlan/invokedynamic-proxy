package net.fushizen.invokedynamic.proxy;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Created by bd on 3/7/15.
 */
public interface DynamicInvocationHandler {
    public CallSite handleInvocation(MethodHandles.Lookup proxyLookup, String methodName, MethodType methodType, MethodHandle superMethod)
            throws Throwable;
}
