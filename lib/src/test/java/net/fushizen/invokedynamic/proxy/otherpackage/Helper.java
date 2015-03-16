package net.fushizen.invokedynamic.proxy.otherpackage;

public class Helper {
    protected static class Superclass {}

    public static Class<?> getSuperclass() {
        return Superclass.class;
    }
}
