package searchengine.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ForkJoinPool;

@Component
public class ForkJoinManager {

    private static ForkJoinPool forkJoinPool;

    private ForkJoinManager(){}

    public static ForkJoinPool getForkJoinPool() {
        if (forkJoinPool == null){
            forkJoinPool = new ForkJoinPool();
        }
        return forkJoinPool;
    }
}
