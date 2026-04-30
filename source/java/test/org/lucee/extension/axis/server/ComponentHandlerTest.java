package org.lucee.extension.axis.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis.MessageContext;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.server.AxisServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import lucee.commons.lang.types.RefBoolean;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.config.Config;
import lucee.runtime.util.Cast;

/**
 * Tests for ComponentHandler.setupService() concurrency fix.
 *
 * The fix:
 * 1. Checks cache BEFORE calling getJavaAccessClass() (skips all reflection on hit)
 * 2. Registers the proxy class in the AxisEngine ClassCache on each hit (fixes
 *    classloader visibility across requests in OSGi)
 * 3. Uses ConcurrentHashMap for thread safety
 * 4. Per-key locking prevents thundering herd on cache miss
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ComponentHandlerTest {

    private ComponentHandler handler;
    private MockedStatic<CFMLEngineFactory> engineFactoryMock;
    private AxisServer axisServer;

    @Mock private CFMLEngine cfmlEngine;
    @Mock private Component component;
    @Mock private PageContext pageContext;
    @Mock private PageSource pageSource;
    @Mock private Config config;
    @Mock private Cast castUtil;

    private static final String DISPLAY_PATH_A = "/app/components/ServiceA.cfc";
    private static final String DISPLAY_PATH_B = "/app/components/ServiceB.cfc";

    @BeforeEach
    void setUp() throws Exception {
        handler = new ComponentHandler();

        clearServiceCache();
        clearLockMap();

        axisServer = new AxisServer();

        engineFactoryMock = mockStatic(CFMLEngineFactory.class);
        engineFactoryMock.when(CFMLEngineFactory::getInstance).thenReturn(cfmlEngine);

        when(cfmlEngine.getThreadPageContext()).thenReturn(pageContext);
        when(cfmlEngine.getThreadConfig()).thenReturn(config);
        when(cfmlEngine.getCastUtil()).thenReturn(castUtil);

        when(component.getPageSource()).thenReturn(pageSource);
        when(pageSource.getDisplayPath()).thenReturn(DISPLAY_PATH_A);

        when(castUtil.toPageException(any(Throwable.class)))
            .thenAnswer(inv -> {
                lucee.runtime.exp.PageException pe = mock(lucee.runtime.exp.PageException.class);
                Throwable t = inv.getArgument(0);
                when(pe.getMessage()).thenReturn(t.getMessage());
                when(pe.toString()).thenReturn(t.toString());
                return pe;
            });
    }

    @AfterEach
    void tearDown() {
        if (engineFactoryMock != null) {
            engineFactoryMock.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void clearServiceCache() throws Exception {
        Field field = ComponentHandler.class.getDeclaredField("serviceCache");
        field.setAccessible(true);
        ((ConcurrentHashMap<String, ?>) field.get(null)).clear();
    }

    @SuppressWarnings("unchecked")
    private void clearLockMap() throws Exception {
        Field field = ComponentHandler.class.getDeclaredField("lockMap");
        field.setAccessible(true);
        ((ConcurrentHashMap<String, ?>) field.get(null)).clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getServiceCache() throws Exception {
        Field field = ComponentHandler.class.getDeclaredField("serviceCache");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(null);
    }

    private void invokeSetupService(MessageContext msgContext) throws Exception {
        Method m = ComponentHandler.class.getDeclaredMethod("setupService", MessageContext.class);
        m.setAccessible(true);
        try {
            m.invoke(handler, msgContext);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw new RuntimeException(cause);
        }
    }

    private MessageContext createMessageContext() {
        MessageContext msgContext = new MessageContext(axisServer);
        msgContext.setProperty(Constants.COMPONENT, component);
        return msgContext;
    }

    private void stubGetJavaAccessClass(Class<?> clazz) throws Exception {
        when(component.getJavaAccessClass(
                any(PageContext.class), any(RefBoolean.class),
                eq(false), eq(true), eq(true), eq(true)))
            .thenAnswer(invocation -> clazz);
    }

    // ========================================================================
    // Basic functionality tests
    // ========================================================================

    @Test
    @DisplayName("Cache miss: creates SOAPService and populates cache")
    void testCacheMissCreatesNewService() throws Exception {
        MessageContext msgContext = createMessageContext();
        stubGetJavaAccessClass(TestServiceA.class);

        invokeSetupService(msgContext);

        assertNotNull(msgContext.getService());
        assertEquals(TestServiceA.class.getName(), msgContext.getService().getName());

        Map<String, ?> cache = getServiceCache();
        assertEquals(1, cache.size());
        assertTrue(cache.containsKey(DISPLAY_PATH_A));
    }

    @Test
    @DisplayName("Cache hit: reuses SOAPService, skips getJavaAccessClass")
    void testCacheHitReusesService() throws Exception {
        stubGetJavaAccessClass(TestServiceA.class);

        MessageContext msgContext1 = createMessageContext();
        invokeSetupService(msgContext1);
        SOAPService firstService = msgContext1.getService();

        MessageContext msgContext2 = createMessageContext();
        invokeSetupService(msgContext2);
        SOAPService secondService = msgContext2.getService();

        assertSame(firstService, secondService,
                "Cache hit should reuse the same SOAPService instance");
    }

    @Test
    @DisplayName("Cache hit skips getJavaAccessClass entirely")
    void testCacheHitSkipsReflection() throws Exception {
        stubGetJavaAccessClass(TestServiceA.class);

        invokeSetupService(createMessageContext()); // cache miss
        invokeSetupService(createMessageContext()); // cache hit
        invokeSetupService(createMessageContext()); // cache hit

        // getJavaAccessClass should only be called once (cache miss)
        verify(component, times(1)).getJavaAccessClass(
                any(PageContext.class), any(RefBoolean.class),
                eq(false), eq(true), eq(true), eq(true));
    }

    @Test
    @DisplayName("Proxy class is registered in AxisEngine ClassCache on cache hit")
    void testProxyClassRegisteredInClassCache() throws Exception {
        stubGetJavaAccessClass(TestServiceA.class);

        invokeSetupService(createMessageContext()); // cache miss

        // Create a new AxisServer (simulates engine change)
        AxisServer newEngine = new AxisServer();
        assertFalse(newEngine.getClassCache().isClassRegistered(TestServiceA.class.getName()),
                "New engine should not have the class registered yet");

        // Cache hit with the new engine
        MessageContext msgContext = new MessageContext(newEngine);
        msgContext.setProperty(Constants.COMPONENT, component);
        invokeSetupService(msgContext);

        // The class should now be registered in the new engine's ClassCache
        assertTrue(newEngine.getClassCache().isClassRegistered(TestServiceA.class.getName()),
                "Cache hit should register the proxy class in the current engine's ClassCache");
    }

    @Test
    @DisplayName("generateWSDL also calls setupService")
    void testGenerateWSDLCallsSetupService() throws Exception {
        MessageContext msgContext = createMessageContext();
        stubGetJavaAccessClass(TestServiceA.class);

        handler.generateWSDL(msgContext);

        assertNotNull(msgContext.getService());
    }

    @Test
    @DisplayName("Multiple components cached independently")
    void testMultipleComponentsCachedSeparately() throws Exception {
        stubGetJavaAccessClass(TestServiceA.class);
        MessageContext msgContext1 = createMessageContext();
        invokeSetupService(msgContext1);

        reset(component, pageSource);
        PageSource pageSourceB = mock(PageSource.class);
        when(component.getPageSource()).thenReturn(pageSourceB);
        when(pageSourceB.getDisplayPath()).thenReturn(DISPLAY_PATH_B);
        stubGetJavaAccessClass(TestServiceB.class);
        MessageContext msgContext2 = createMessageContext();
        invokeSetupService(msgContext2);

        Map<String, ?> cache = getServiceCache();
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(DISPLAY_PATH_A));
        assertTrue(cache.containsKey(DISPLAY_PATH_B));
        assertNotSame(msgContext1.getService(), msgContext2.getService());
    }

    // ========================================================================
    // Thread safety tests
    // ========================================================================

    @Test
    @DisplayName("serviceCache uses ConcurrentHashMap")
    void testConcurrentHashMapUsed() throws Exception {
        Field field = ComponentHandler.class.getDeclaredField("serviceCache");
        field.setAccessible(true);
        assertTrue(field.get(null) instanceof ConcurrentHashMap);
    }

    @Test
    @DisplayName("Cache key is displayPath (available without reflection)")
    void testCacheKeyIsDisplayPath() throws Exception {
        stubGetJavaAccessClass(TestServiceA.class);
        invokeSetupService(createMessageContext());

        Map<String, ?> cache = getServiceCache();
        assertTrue(cache.containsKey(DISPLAY_PATH_A));
        assertFalse(cache.containsKey(TestServiceA.class.getName()));
    }

    // ========================================================================
    // Concurrency tests
    // ========================================================================

    private void setupForConcurrentTest() {
        engineFactoryMock.close();
        engineFactoryMock = null;
        CFMLEngineFactory.registerInstance(cfmlEngine);
    }

    @Test
    @DisplayName("Per-key locking: only 1 thread does reflection, others get cache hit")
    void testConcurrentRequestsOnlyOneDoesReflection() throws Exception {
        setupForConcurrentTest();

        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger reflectionCallCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        when(component.getJavaAccessClass(
                any(PageContext.class), any(RefBoolean.class),
                eq(false), eq(true), eq(true), eq(true)))
            .thenAnswer(invocation -> {
                reflectionCallCount.incrementAndGet();
                Thread.sleep(100);
                return TestServiceA.class;
            });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    MessageContext msgContext = createMessageContext();
                    barrier.await(5, TimeUnit.SECONDS);
                    invokeSetupService(msgContext);
                } catch (Throwable e) {
                    firstError.compareAndSet(null, e);
                    throw new RuntimeException(e);
                }
            }));
        }

        int successCount = 0;
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
                successCount++;
            } catch (Exception e) {
                // may fail
            }
        }
        executor.shutdown();

        if (reflectionCallCount.get() == 0 && firstError.get() != null) {
            fail("All threads failed: " + firstError.get().getMessage(), firstError.get());
        }

        // Only 1 thread should do the expensive reflection (cache miss).
        // All others should get a cache hit after the lock is released.
        assertEquals(1, reflectionCallCount.get(),
                "Only 1 thread should call getJavaAccessClass. " +
                successCount + "/" + threadCount + " succeeded.");
    }

    // ========================================================================
    // Error handling tests
    // ========================================================================

    @Test
    @DisplayName("invoke wraps exceptions as AxisFault")
    void testInvokeWrapsExceptions() throws Exception {
        MessageContext msgContext = createMessageContext();
        when(component.getJavaAccessClass(
                any(PageContext.class), any(RefBoolean.class),
                eq(false), eq(true), eq(true), eq(true)))
            .thenThrow(new RuntimeException("test error"));

        assertThrows(org.apache.axis.AxisFault.class, () -> handler.invoke(msgContext));
    }

    @Test
    @DisplayName("generateWSDL wraps exceptions as AxisFault")
    void testGenerateWSDLWrapsExceptions() throws Exception {
        MessageContext msgContext = createMessageContext();
        when(component.getJavaAccessClass(
                any(PageContext.class), any(RefBoolean.class),
                eq(false), eq(true), eq(true), eq(true)))
            .thenThrow(new RuntimeException("test error"));

        assertThrows(org.apache.axis.AxisFault.class, () -> handler.generateWSDL(msgContext));
    }
}
