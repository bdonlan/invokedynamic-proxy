# invokedynamic-proxy

An alternate to the built-in Java Proxy; uses Java 7 features and invokedynamic to provide better performance and access
to features unavailable through standard Proxies.

## Usage

    public class Demo {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        public interface HelloWorld {
            public void sayHi();
        }

        private static void impl_sayHi(Object proxyObject) {
            System.out.println("Hello world!");
        }

        public static void main(String[] args) throws Throwable {
            DynamicInvocationHandler handler = (lookup, name, type, superMethod) -> {
                if (name.equals("sayHi")) {
                    return new ConstantCallSite(
                        LOOKUP.findStatic(
                                 Demo.class,
                                 "impl_sayHi",
                                 MethodType.methodType(Void.TYPE, Object.class)
                               ).asType(type)
                    );
                } else {
                    return new ConstantCallSite(superMethod);
                }
            };

            DynamicProxy proxy = DynamicProxy.builder()
                    .withInterfaces(HelloWorld.class)
                    .withInvocationHandler(handler)
                    .build();

            HelloWorld helloWorld = (HelloWorld)proxy.constructor().invoke();
            helloWorld.sayHi();
        }
    }

## That looks more complicated than using Proxy

It is, a little. You do however get two big benefits from this method:

* Better performance. You avoid reflective overhead and boxing by using invokedynamic instead of going through proxy
  invocation handlers. You also avoid running your dispatch logic each time you invoke the method; it runs once, and the
  JVM then binds directly to the call site.
* Access to superclass and default methods. Currently there is no documented/supported way to access interface default
  method implementations from a Proxy method handler. With invokedynamic-proxy, the superclass or default method is
  passed in to your handler (and you also get a Lookup instance with access to private and super-methods of the proxy
  class).

## What version of Java does this need?

Java 8. Not for any particularly compelling reason, but if you're on Java 7 you really ought to move on to 8 anyway,
just for the security support.

## License

Copyright © 2015 Bryan Donlan

Distributed under the 2-clause BSD license.