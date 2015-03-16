package net.fushizen.invokedynamic.proxy;

import jdk.nashorn.internal.codegen.CompilerConstants;
import org.openjdk.jmh.annotations.*;

import java.lang.invoke.*;
import java.util.concurrent.TimeUnit;

/*
@Measurement(iterations = 2)
@Fork(value = 2)
@Warmup(iterations = 2)
*/
@State(Scope.Benchmark)
public class IndyProxyBenchmark {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    // Do _not_ make this a constant
    private int one = 1;
    
    public interface VoidMethod {
        public void method();
    }

    private static void noop() {}

    private static final VoidMethod voidMethodProxy;

    static {
        try {
            final MethodHandle noopHandle = LOOKUP.findStatic(IndyProxyBenchmark.class, "noop", MethodType.methodType(Void.TYPE));


            voidMethodProxy = (VoidMethod) DynamicProxy.builder()
                    .withInterfaces(VoidMethod.class)
                    .withInvocationHandler(
                            (lookup, name, type, superMethod) -> {
                                if (superMethod != null) return new ConstantCallSite(superMethod);

                                MethodHandle coercedHandle = MethodHandles.dropArguments(noopHandle, 0, Object.class);

                                return new ConstantCallSite(coercedHandle.asType(type));
                            }
                    )
                    .build()
                    .constructor()
                    .invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void proxy_voidMethod() {
        voidMethodProxy.method();
    }

    public interface NonPrimsMethod {
        public String method(String input);
    }

    private static String noop_String(String input) {
        return input;
    }

    private static final NonPrimsMethod nonPrimsProxy;

    static {
        try {
            final MethodHandle noopHandle = LOOKUP.findStatic(
                    IndyProxyBenchmark.class,
                    "noop_String",
                    MethodType.methodType(String.class, String.class)
            );

            nonPrimsProxy = (NonPrimsMethod) DynamicProxy.builder()
                    .withInterfaces(NonPrimsMethod.class)
                    .withInvocationHandler(
                            (lookup, name, type, superMethod) -> {
                                if (superMethod != null) return new ConstantCallSite(superMethod);

                                MethodHandle coercedHandle = MethodHandles.dropArguments(noopHandle, 0, Object.class);

                                return new ConstantCallSite(coercedHandle.asType(type));
                            }
                    )
                    .build()
                    .constructor()
                    .invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String proxy_nonPrims() {
        return nonPrimsProxy.method("Hello world");
    }

    public interface PrimsMethod {
        public int increment(int x);
    }

    private static int increment(int x) {
        return x + 1;
    }

    private static final PrimsMethod primsMethodProxy;


    static {
        try {
            final MethodHandle incrementHandle = LOOKUP.findStatic(
                    IndyProxyBenchmark.class,
                    "increment",
                    MethodType.methodType(Integer.TYPE, Integer.TYPE)
            );

            primsMethodProxy = (PrimsMethod) DynamicProxy.builder()
                    .withInterfaces(PrimsMethod.class)
                    .withInvocationHandler(
                            (lookup, name, type, superMethod) -> {
                                if (superMethod != null) return new ConstantCallSite(superMethod);

                                MethodHandle coercedHandle = MethodHandles.dropArguments(incrementHandle, 0, Object.class);

                                return new ConstantCallSite(coercedHandle.asType(type));
                            }
                    )
                    .build()
                    .constructor()
                    .invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int proxy_prims() {
        return primsMethodProxy.increment(one);
    }

    private static class PrimsPassThrough implements PrimsMethod {
        @Override
        public int increment(int x) {
            return x + 1;
        }
    }

    public static class PassThroughBase {
        PrimsPassThrough invokee;

        public final void $$setInvokee(PrimsPassThrough invokee) {
            this.invokee = invokee;
        }

        public PassThroughBase() {}
    }

    private static final DynamicProxy primsPassthroughProxyTemplate;

    static {
        try {
            primsPassthroughProxyTemplate = DynamicProxy.builder()
                    .withInterfaces(PrimsMethod.class)
                    .withSuperclass(PassThroughBase.class)
                    .withInvocationHandler((lookup, name, type, superMethod) -> {
                        MethodType withoutReceiverType = type.dropParameterTypes(0, 1);
                        MethodHandle receiverHandle = LOOKUP.findVirtual(PrimsPassThrough.class, name, withoutReceiverType);
                        MethodHandle getReceiver = LOOKUP.findGetter(PassThroughBase.class, "invokee", PrimsPassThrough.class);

                        MethodHandle invokedHandle = MethodHandles.filterArguments(receiverHandle, 0, getReceiver);

                        return new ConstantCallSite(invokedHandle.asType(type));
                    })
                    .build();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static PrimsMethod makePassThrough() {
        try {
            Object proxyInstance = primsPassthroughProxyTemplate.supplier().get();

            ((PassThroughBase)proxyInstance).$$setInvokee(new PrimsPassThrough());

            return (PrimsMethod) proxyInstance;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static final PrimsMethod passthroughStaticProxy = makePassThrough();

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int proxy_passThrough() {
        return passthroughStaticProxy.increment(1);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object proxy_passThrough_ctor_cached() {
        return makePassThrough();
    }
    
    public interface IncrementDecrement {
        public int increment(int x);
        public int decrement(int x);
        public int noop(int x);
    }

    private static CallSite easyDispatch(String name, MethodType type) {
        try {
            MethodHandle method = LOOKUP.findStatic(IndyProxyBenchmark.class, name, type.dropParameterTypes(0, 1));
            method = MethodHandles.dropArguments(method, 0, Object.class);

            return new ConstantCallSite(method.asType(type));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static CallSite incDecDispatchHandler(MethodHandles.Lookup proxyLookup, String name, MethodType type, MethodHandle superMethod) {
        if (superMethod != null) return new ConstantCallSite(superMethod.asType(type));

        switch (name) {
            case "increment": return easyDispatch("increment", type);
            case "decrement": return easyDispatch("decrement", type);
            case "noop": return easyDispatch("noop_int", type);
            default:
                throw new RuntimeException("unknown method " + name);
        }
    }

    // increment is already defined above
    private static int decrement(int x) {
        return x - 1;
    }
    private static int noop_int(int x) {
        return x;
    }

    private static final IncrementDecrement incDecProxy;

    static {
        try {
            incDecProxy = (IncrementDecrement)DynamicProxy.builder()
                    .withInterfaces(IncrementDecrement.class)
                    .withInvocationHandler(IndyProxyBenchmark::incDecDispatchHandler)
                    .build()
                    .supplier().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Group("incDec")
    public int incdec_increment() {
        return incDecProxy.increment(one);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Group("incDec")
    public int incdec_decrement() {
        return incDecProxy.decrement(one);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Group("incDec")
    public int incdec_noop() {
        return incDecProxy.noop(one);
    }
}
