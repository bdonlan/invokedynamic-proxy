package net.fushizen.invokedynamic.proxy;

import org.junit.Assume;

public class Utils {
    public static void ignoreOnJava9() {
        Assume.assumeFalse(System.getProperty("java.vm.specification.version").equals("9"));
    }
}
