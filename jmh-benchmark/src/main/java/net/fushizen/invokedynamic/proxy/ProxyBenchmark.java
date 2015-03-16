/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.fushizen.invokedynamic.proxy;

import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

/*
@Measurement(iterations = 2)
@Fork(value = 2)
@Warmup(iterations = 2)
*/
@State(Scope.Benchmark)
public class ProxyBenchmark {
    // Do _not_ make constant
    private int one = 1;

    interface VoidMethod {
        public void method();
    }

    private static final class NullInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    private static final VoidMethod voidMethodProxy = (VoidMethod)Proxy.newProxyInstance(VoidMethod.class.getClassLoader(), new Class[] { VoidMethod.class }, new NullInvocationHandler());

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void proxy_voidMethod() {
        voidMethodProxy.method();
    }

    interface NonPrimsMethod {
        public String method(String input);
    }

    private static final class NonPrimsInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return args[0];
        }
    }

    private static final NonPrimsMethod nonPrimsProxy =
            (NonPrimsMethod)Proxy.newProxyInstance(
                    NonPrimsMethod.class.getClassLoader(),
                    new Class[] { NonPrimsMethod.class },
                    new NonPrimsInvocationHandler()
            );

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String proxy_nonPrims() {
        return nonPrimsProxy.method("Hello world");
    }

    interface PrimsMethod {
        public int increment(int x);
    }

    private static final class PrimsMethodInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return ((Integer)args[0]) + 1;
        }
    }

    private static final PrimsMethod primsMethodProxy =
            (PrimsMethod)Proxy.newProxyInstance(
                    PrimsMethod.class.getClassLoader(),
                    new Class[] { PrimsMethod.class },
                    new PrimsMethodInvocationHandler()
            );

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int proxy_prims() {
        return primsMethodProxy.increment(1);
    }

    private static class PrimsPassThrough implements PrimsMethod {
        @Override
        public int increment(int x) {
            return x + 1;
        }
    }

    private static final class PassthroughMethodInvocationHandler implements InvocationHandler {
        PrimsPassThrough passThrough = new PrimsPassThrough();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(passThrough, args);
        }
    }

    private static PrimsMethod makePassThrough() {
        return (PrimsMethod) Proxy.newProxyInstance(
                PrimsMethod.class.getClassLoader(),
                new Class[]{PrimsMethod.class},
                new PassthroughMethodInvocationHandler()
        );
    }

    private static final PrimsMethod primsPassThroughProxy = makePassThrough();

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int proxy_passThrough() {
        return primsPassThroughProxy.increment(one);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object proxy_passThrough_ctor() {
        return makePassThrough();
    }

    private static final Constructor proxyCtor;

    static {
        try {
            proxyCtor = Proxy.getProxyClass(
                    PrimsMethod.class.getClassLoader(),
                    new Class[]{PrimsMethod.class}
            ).getConstructor(InvocationHandler.class);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Object proxy_passThrough_ctor_cached() throws Throwable {
        return proxyCtor.newInstance(new PassthroughMethodInvocationHandler());
    }

    public interface IncrementDecrement {
        public int increment(int x);
        public int decrement(int x);
        public int noop(int x);
    }

    private static final class IncDecDispatch implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "increment": return ((Integer)args[0]) + 1;
                case "decrement": return ((Integer)args[0]) - 1;
                case "noop":  return args[0];
                default: throw new RuntimeException("unknown method " + method);
            }
        }
    }

    private static final IncrementDecrement incDecProxy;

    static {
        try {
            incDecProxy = (IncrementDecrement)Proxy.newProxyInstance(
                    IncrementDecrement.class.getClassLoader(),
                    new Class[] {IncrementDecrement.class},
                    new IncDecDispatch()
            );
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
