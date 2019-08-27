package es.luepg.ecs.timings;

import lombok.Getter;

/**
 * A simple timing handler for {@link com.artemis.BaseSystem}s
 * <p>
 * Used as example by the {@link TimingInvocationStrategy}
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
public class SystemTimingHandler {

    @Getter
    private final String name;

    @Getter
    private long start;
    @Getter
    private long totalTime;
    @Getter
    private int totalTicks;


    public SystemTimingHandler(String name) {
        this.name = name;
    }

    public void startTiming() {
        this.start = System.nanoTime();
    }

    public void stopTiming() {
        long diff = System.nanoTime() - this.start;
        this.totalTime += diff;

        this.totalTicks++;
        this.start = 0;
    }

    public void reset() {
        this.totalTicks = 0;
        this.start = 0;
        this.totalTime = 0;
    }


}
