package net.fushizen.invokedynamic.proxy;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by bd on 3/7/15.
 */
public class DynamicProxy {
    private static final AtomicInteger CLASS_COUNT = new AtomicInteger();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private final Class<?> proxyClass;
    private final MethodHandle constructor;

    public DynamicProxy(Class<?> proxyClass, MethodHandle constructor) {
        this.proxyClass = proxyClass;
        this.constructor = constructor;
    }

    public Class<?> proxyClass() {
        return proxyClass;
    }

    public MethodHandle constructor() {
        return constructor;
    }

    public static Builder builder(Class<?>... interfaces) {
        Builder builder = new Builder();

        builder.addInterfaces(interfaces);

        return builder;
    }

    public static class Builder {
        private Class<?> superclass = Object.class;
        private ArrayList<Class<?>> interfaces = new ArrayList<>();

        public void addInterfaces(Class<?>[] interfaces) {
            this.interfaces.addAll(Arrays.asList(interfaces));
        }

        public DynamicProxy build() throws Exception {
            Class<?> proxyClass = generateProxyClass(this);
            MethodHandle constructor = LOOKUP.findConstructor(proxyClass, MethodType.methodType(Void.TYPE));

            return new DynamicProxy(proxyClass, constructor);
        }

    }

    private static Class<?> generateProxyClass(Builder builder) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String binaryName = String.format("net/fushizen/invokedynamic/proxy/generated/Proxy$%d",
                CLASS_COUNT.incrementAndGet());
        String superclassName = Type.getInternalName(builder.superclass);
        String[] interfaceNames = new String[builder.interfaces.size()];
        for (int i = 0; i < builder.interfaces.size(); i++) {
            interfaceNames[i] = Type.getInternalName(builder.interfaces.get(i));
        }

        cw.visit(Opcodes.V1_7,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                binaryName,
                null,
                superclassName,
                interfaceNames);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superclassName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();

        byte[] classData = cw.toByteArray();

        ProxyLoader loader = new ProxyLoader(DynamicProxy.class.getClassLoader());
        return loader.loadClass(classData);
    }

    private static class ProxyLoader extends ClassLoader {
        protected ProxyLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> loadClass(byte[] buf) {
            return defineClass(null, buf, 0, buf.length);
        }
    }
}
