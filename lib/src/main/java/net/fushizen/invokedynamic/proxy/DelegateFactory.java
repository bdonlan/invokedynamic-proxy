package net.fushizen.invokedynamic.proxy;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

public class DelegateFactory {
    public static <T> DynamicProxy createDelegatingProxy(Class<T> delegateClass, Class<? super T> interfaceClass) {
        try {
            DynamicProxy proxy = DynamicProxy.builder()
                    .withConstructor(interfaceClass)
                    .withInterfaces(interfaceClass)
                    .withSuperclass(delegateClass)
                    .withInvocationHandler(new DelegateInvocationHandler(delegateClass, interfaceClass))
                    .build();
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class DelegateInvocationHandler implements DynamicInvocationHandler {
        private final Class delegateType;
        private final Class interfaceType;

        public DelegateInvocationHandler(Class delegateType, Class interfaceType) {
            this.delegateType = delegateType;
            this.interfaceType = interfaceType;
        }

        @Override
        public CallSite handleInvocation(
                Lookup lookup,
                String methodName,
                MethodType methodType,
                MethodHandle superMethod
        ) throws Throwable {
            if (superMethod != null) {
                return new ConstantCallSite(superMethod.asType(methodType));
            }

            Field field = delegateType.getDeclaredField("delegate");
            field.setAccessible(true);
            MethodHandle delegateGetter = lookup.unreflectGetter(field);

            MethodType methodArgs = methodType.dropParameterTypes(0, 1);
            MethodHandle interfaceMethod = lookup.findVirtual(interfaceType, methodName, methodArgs);
            MethodHandle syntheticMethod = MethodHandles.filterArguments(interfaceMethod, 0, delegateGetter);
            return new ConstantCallSite(syntheticMethod.asType(methodType));
        }
    }
}
