package es.luepg.ecs.timings;

import lombok.Getter;

import java.io.PrintStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author elmexl
 * Created on 19.07.2019.
 */
public class TimingHandler {

    private static Queue<TimingHandler> HANDLERS = new ConcurrentLinkedQueue<>();

    @Getter
    private final String name;

    private long start;
    private long totalTime;

    private int totalTicks;


    public TimingHandler(String name) {
        this.name = name;
        HANDLERS.add(this);
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


    public static void print(PrintStream printStream) {
        printStream.println("Timings:");
        for (TimingHandler handler : HANDLERS) {
            printStream.print("   " + handler.getName() + " Time: " + handler.totalTime + " Times: " + handler.totalTicks);
            if (handler.totalTicks != 0) {
                long avg = handler.totalTime / handler.totalTicks;
                printStream.println(" Avg: " + avg);
            }
        }
    }

    public static void resetAll() {
        HANDLERS.forEach(TimingHandler::reset);
    }
}
