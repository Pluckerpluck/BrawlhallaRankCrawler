package com.pluckerpluck;

import com.pluckerpluck.Bracket.Brackets;
import com.pluckerpluck.Bracket.Division;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pluckerpluck
 */
public class RankDistribution {
  
    final static org.slf4j.Logger log = LoggerFactory.getLogger(RankDistribution.class);
    private static long lastCall = 0;
    final static int waitTime = 5; // In seconds
    
    
    public static void main(String[] args) {
        parseRankings(Brackets.US, 30);
    }
    
    public static void parseRankings(Brackets bracket, int step){
        long start = System.nanoTime();
        
        Bracket br = new Bracket(bracket, step);
        
        Map<Division, Page> boundaries = br.getBoundaries();
        
        long end = System.nanoTime();
        
        log.info("----------------------------------------");
        for (Division division : boundaries.keySet()) {
            Page page = boundaries.get(division);
            Integer rank = page.getBoundaries().get(division);
            log.info("Division Boundary: {}, Rank: {}", division, rank);
        }
        log.info("Requests Made: {}", br.requestCount());
        
        long duration = end - start;
        Duration dur = Duration.ofNanos(duration);
        long minutes = dur.toMinutes();
        long seconds = dur.minusMinutes(minutes).getSeconds();
        log.info("Time taken: {}:{} minutes", minutes, seconds);
    }
    
    public static void requestWait(){
        try {
            while (System.nanoTime() - lastCall < TimeUnit.SECONDS.toNanos(waitTime)){
                // Not enough time has passed
                log.debug("Waiting for next call...");
                long remainingNano = TimeUnit.SECONDS.toNanos(waitTime+1) - (System.nanoTime() - lastCall);
                // Ensure positive, just in case
                if (remainingNano > 0){
                    TimeUnit.NANOSECONDS.sleep(remainingNano);
                }
            }
            lastCall = System.nanoTime();
            
        } catch (InterruptedException ex) {
            Logger.getLogger(RankDistribution.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  
  
}
