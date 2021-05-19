package es.luepg.ecs.timings;

import com.artemis.BaseSystem;
import com.artemis.SystemInvocationStrategy;
import lombok.Getter;
import lombok.Setter;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Time how long which {@link BaseSystem} took to run
 * <p>
 * Could be improved... at some time
 *
 * @author elmexl
 * Created on 19.07.2019.
 */
public class TimingInvocationStrategy extends SystemInvocationStrategy {

    @Getter
    @Setter
    private boolean enableSystemTiming = false;

    private final Map<Integer, SystemTimingHandler> timingHandlerMap = new HashMap<>();


    /**
     * Processes all systems in order.
     * <p>
     * Should guarantee artemis is in a sane state using calls to #updateEntityStates
     * before each call to a system, and after the last system has been called, or if no
     * systems have been called at all.
     */
    @Override
    protected void process() {
        BaseSystem[] systemsData = systems.getData();
        SystemTimingHandler timer = null;
        for (int i = 0, s = systems.size(); s > i; i++) {
            if (disabled.get(i))
                continue;


            updateEntityStates();

            if (enableSystemTiming) {
                timer = getTimer(i, systemsData[i]);
                timer.startTiming();
            }

            systemsData[i].process();

            if (enableSystemTiming) {
                timer.stopTiming();
            }
        }

        updateEntityStates();
    }

    private SystemTimingHandler getTimer(int i, BaseSystem baseSystem) {
        SystemTimingHandler timingHandler = timingHandlerMap.get(i);
        if (timingHandler == null) {
            timingHandler = new SystemTimingHandler(baseSystem.getClass().getName());
            timingHandlerMap.put(i, timingHandler);
        }
        return timingHandler;
    }

    public void reset() {
        timingHandlerMap.values().forEach(SystemTimingHandler::reset);
    }

    public void clear() {
        this.timingHandlerMap.clear();
    }


    public void print(PrintStream printStream) {
        printStream.println("Timings:");
        for (Map.Entry<Integer, SystemTimingHandler> e : timingHandlerMap.entrySet()) {
            SystemTimingHandler handler = e.getValue();
            printStream.print("   " + e.getKey() + ": " + handler.getName() + " Time: " + handler.getTotalTime()
                    + " Times: " + handler.getTotalTicks());
            if (handler.getTotalTicks() != 0) {
                long avg = handler.getTotalTime() / handler.getTotalTicks();
                printStream.println(" Avg: " + avg + " ns");
            }
        }
    }

}
