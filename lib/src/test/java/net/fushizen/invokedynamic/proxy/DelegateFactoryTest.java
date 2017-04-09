package net.fushizen.invokedynamic.proxy;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;

public class DelegateFactoryTest {
    @Test
    public void smokeTest() throws Throwable {
        DynamicProxy proxy = DelegateFactory.createDelegatingProxy(ReallyBlockingQueue.class, BlockingQueue.class);
        LinkedBlockingQueue<Object> lbq = new LinkedBlockingQueue<>();
        ReallyBlockingQueue queue = (ReallyBlockingQueue) proxy.constructor().invoke(lbq);

        queue.offer(14);
        queue.put(15);

        assertEquals(14, queue.poll());
        assertEquals(15, queue.take());
    }

    public abstract static class ReallyBlockingQueue<T> implements BlockingQueue<T> {
        private final BlockingQueue<T> delegate;

        protected ReallyBlockingQueue(BlockingQueue<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean offer(T t) {
            try {
                delegate.put(t);
                return true;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
