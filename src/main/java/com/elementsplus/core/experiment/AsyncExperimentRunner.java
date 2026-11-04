package com.elementsplus.core.experiment;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 在独立线程上运行实验测例。
 *
 * <p>后台线程只操作实验自身的副本和上下文，不直接修改方块实体或发送网络包。
 * 所有宿主回调（测例进度、失败信息、完成）都先进入队列，由服务器线程每 tick
 * 取出并应用一次，因此完成状态、物品写回和网络同步仍然发生在主线程。</p>
 */
public final class AsyncExperimentRunner implements AutoCloseable {
    private final BaseExperiment experiment;
    private final ItemStack itemStack;
    private final ConcurrentLinkedQueue<Consumer<ExperimentHost>> events = new ConcurrentLinkedQueue<>();
    private final Object pauseLock = new Object();
    private final Thread thread;

    private volatile boolean paused;
    private volatile boolean stopped;
    private volatile boolean done;
    private volatile boolean finished;
    private volatile boolean failed;
    private volatile int testIndex;

    public AsyncExperimentRunner(BaseExperiment experiment, ItemStack itemStack, int testIndex,
                                 @Nullable Object experimentState, int time, @Nullable UUID playerUuid,
                                 long worldSeed) {
        this.experiment = experiment;
        this.itemStack = itemStack.copy();
        this.testIndex = testIndex;

        BaseExperiment.Context context = new BaseExperiment.Context(new AsyncHost(), null, this.itemStack);
        context.testIndex = testIndex;
        context.experimentState = experimentState;
        context.time = time;
        context.playerUuid = playerUuid;
        context.worldSeed = worldSeed;
        context.hasWorldSeed = true;
        context.random = RandomSource.create();

        this.thread = new Thread(() -> run(context), "elements-plus-experiment");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private void run(BaseExperiment.Context context) {
        try {
            while (!stopped) {
                if (paused) {
                    awaitResume();
                    continue;
                }

                boolean ok;
                try {
                    ok = experiment.tick(context);
                } catch (Throwable t) {
                    ElementsPlus.LOGGER.error("Async experiment failed", t);
                    if (!finished) {
                        enqueueError(t);
                        requestFinish(false);
                    }
                    break;
                }

                testIndex = context.testIndex;
                context.time++;
                if (finished || stopped) {
                    break;
                }
                if (!ok) {
                    // 正常情况下 tick 返回 false 前会调用 complete；这里兜底避免实验永远停在 BUSY。
                    if (!finished) {
                        enqueueError(null);
                        requestFinish(false);
                    }
                    break;
                }
                Thread.yield();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            ElementsPlus.LOGGER.error("Async experiment runner failed", t);
            if (!finished) {
                enqueueError(t);
                requestFinish(false);
            }
        } finally {
            done = true;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    private void awaitResume() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused && !stopped) {
                pauseLock.wait();
            }
        }
    }

    private void enqueueError(@Nullable Throwable throwable) {
        if (throwable == null) {
            events.add(host -> host.setErrorLines(List.of(
                    Component.translatable("experiment.elements-plus.async.stopped"))));
            return;
        }
        String detail = throwable.toString();
        events.add(host -> host.setErrorLines(List.of(
                Component.translatable("experiment.elements-plus.async.error", detail))));
    }

    private void requestFinish(boolean success) {
        if (finished) {
            return;
        }
        failed = !success;
        events.add(host -> host.finishExperiment(success));
        finished = true;
    }

    /**
     * 在服务器线程上取出并应用当前所有后台回调。
     *
     * @return 本次应用的更新数量
     */
    public int drain(ExperimentHost host) {
        int count = 0;
        Consumer<ExperimentHost> event;
        while ((event = events.poll()) != null) {
            event.accept(host);
            count++;
        }
        return count;
    }

    public void setPaused(boolean value) {
        paused = value;
        if (!value) {
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    public int testIndex() {
        return testIndex;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isFailed() {
        return failed;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public void stop() {
        stopped = true;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        thread.interrupt();
    }

    @Override
    public void close() {
        stop();
    }

    private final class AsyncHost implements ExperimentHost {
        @Override
        public void finishExperiment(boolean success) {
            requestFinish(success);
        }

        @Override
        public void setErrorLines(List<Component> lines) {
            List<Component> copy = lines == null ? List.of() : List.copyOf(lines);
            events.add(host -> host.setErrorLines(copy));
        }

        @Override
        public void setFailureInfo(ExperimentFailureInfo info) {
            events.add(host -> host.setFailureInfo(info));
        }

        @Override
        public void setSequentialFailureInfo(SequentialFailureInfo info) {
            events.add(host -> host.setSequentialFailureInfo(info));
        }

        @Override
        public void reportTestResult(int index, boolean pass) {
            events.add(host -> host.reportTestResult(index, pass));
        }

        @Override
        public void transformActiveItem(Function<ItemStack, ItemStack> function) {
            events.add(host -> host.transformActiveItem(function));
        }
    }
}
