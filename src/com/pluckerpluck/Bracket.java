package com.pluckerpluck;

import com.pluckerpluck.Player.Tier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pluckerpluck
 */
public class Bracket {

    final static org.slf4j.Logger log = LoggerFactory.getLogger(Bracket.class);
    private int initialStepSize = 300;
    
    private final SortedMap<Integer, Page> pages;
    
    private final Map<Division, Page> boundaries;
    private final Set<Division> boundariesLeft;
    
    /**
     * Division = Tier + Sub-Tier
     */
    public enum Division{
        TIN_0 (200, Tier.TIN),
        TIN_1 (720, Tier.TIN),
        TIN_2 (784, Tier.TIN),
        TIN_3 (848, Tier.TIN),
        TIN_4 (912, Tier.TIN),
        TIN_5 (976, Tier.TIN),
        BRONZE_1 (1040, Tier.BRONZE),
        BRONZE_2 (1088, Tier.BRONZE),
        BRONZE_3 (1136, Tier.BRONZE),
        BRONZE_4 (1184, Tier.BRONZE),
        BRONZE_5 (1232, Tier.BRONZE),
        SILVER_1 (1280, Tier.SILVER),
        SILVER_2 (1312, Tier.SILVER),
        SILVER_3 (1344, Tier.SILVER),
        SILVER_4 (1376, Tier.SILVER),
        SILVER_5 (1408, Tier.SILVER),
        GOLD_1 (1440, Tier.GOLD),
        GOLD_2 (1488, Tier.GOLD),
        GOLD_3 (1536, Tier.GOLD),
        GOLD_4 (1584, Tier.GOLD),
        GOLD_5 (1632, Tier.GOLD),
        PLATINUM_1 (1680, Tier.PLATINUM),
        PLATINUM_2 (1744, Tier.PLATINUM),
        PLATINUM_3 (1808, Tier.PLATINUM),
        PLATINUM_4 (1872, Tier.PLATINUM),
        PLATINUM_5 (1936, Tier.PLATINUM),
        DIAMOND (2000, Tier.DIAMOND);
        
        
        private final int elo; // Represents lower boundry required
        private final Tier tier;
        Division(int elo, Tier tier){
            this.elo = elo;
            this.tier = tier;
        }
        
        public int elo(){
            return elo;
        }
        
        public Tier tier(){
            return tier;
        }
    }
    
    /**
    * 1v1 unless otherwise stated
    */
    public enum Brackets{
        US ("us/1v1/"),
        EU ("eu/1v1/"),
        SEA ("sea/1v1/"),
        US_2v2 ("us/2v2/"),
        EU_2v2 ("eu/2v2"),
        SEA_2v2 ("sea/2v2");

        private final String extension;

        Brackets(String extension){
            this.extension = extension;
        }

        public String getExtension(){
            return extension;
        }

        public String getPageURL(int page){
            String baseURL = "http://www.brawlhalla.com/rankings/";
            return baseURL + extension + page;
        }
    }
    
    private final Brackets bracket;

    public Bracket(Brackets bracket, int initialStepSize) {
        this.bracket = bracket;
        this.pages = new TreeMap<>();
        
        this.initialStepSize = initialStepSize;
        
        boundaries = new TreeMap<>();
        
        // Fill and populate list
        boundariesLeft = new HashSet<>(Arrays.asList(Division.values()));
        
        // Load in first page
        loadPage(1);
        
        // Search and load last page
        findLastPage();
        
        findBoundaries();
    }

    public Map<Division, Page> getBoundaries() {
        return boundaries;
    }
    
    
    
    /**
     * Loads the page number given
     * 
     * @param pageNumber The page to load
     * @return The page object
     */
    private Page loadPage(int pageNumber){
        // Check if it already exists
        if (pages.get(pageNumber) != null){
            log.error("Page already loaded, probably an error: {}", pageNumber);
            return pages.get(pageNumber);
        }
        
        Page page = new Page(pageNumber, bracket);
        page.loadPage();
        
        // Cancel if page is empty
        if (page.isEmpty()){
            return page;
        }

        pages.put(page.getPageNumber(), page);
        
        // Boundaries found, save location
        if (page.getBoundaries().size() > 0){
            for (Division boundary : page.getBoundaries().keySet()) {
                boundariesLeft.remove(boundary);
                boundaries.put(boundary, page);
            }
        }
       
        return page;
    }
    
    private void findBoundaries(){
        for (Division division : Division.values()) {
            if (boundariesLeft.contains(division)){
                findBoundary(division);
            }
        }
    }
    
    private void findBoundary(Division boundary){
        // First find which pages it's between
        
        int higherElo = 0;
        Tier higherTier = Tier.DIAMOND;
        int higherPage = 0;
        
        int targetElo = boundary.elo();
        Tier targetTier = boundary.tier();
        
        for (Page page : pages.values()) {
            int lowerElo = page.firstPlayer().getElo();
            Tier lowerTier = page.firstPlayer().getTier();
            int lowerPage = page.getPageNumber();
            
            
            // Check if boundary lies between these locations
            // First check for sneaky tier stuff
            if(lowerTier.compareTo(targetTier) > 0){
                log.trace("Tier too high ({} vs {}), looking lower...", lowerTier, targetTier);
                higherElo = lowerElo;
                higherTier = lowerTier;
                higherPage = lowerPage;
            }else{
                // Check Elo
                if (lowerElo >= targetElo){
                    log.trace("Elo too high ({} vs {}), looking lower...", lowerElo, targetElo);
                    higherElo = lowerElo;
                    higherTier = lowerTier;
                    higherPage = lowerPage;
                }else{
                    // This page is after where we're looking!
                    if (lowerPage == higherPage + 1){
                        log.trace("Boundary over page divide or non-existant!");
                        boundariesLeft.remove(boundary);
                        boundaries.put(boundary, page);
                        page.getBoundaries().put(boundary, page.lastRank());
                        return; // We're done here
                    }else{
                        // Just half to find next page location
                        int pageDiff = lowerPage - higherPage;
                        int jump = pageDiff/2;
                        
                        
                        loadPage(higherPage + jump);
                        
                        if (boundariesLeft.contains(boundary)){
                            findBoundary(boundary);
                        }
                        break;
                    }
                }
            }
            
        }
    }
    
    public int requestCount(){
        return pages.size();
    }
    
    private void findLastPage(){
        log.info("Searching for last page...");
        // Ensure we don't already have the last page
        if (pages.get(pages.lastKey()).isLastPage()){
            return;
        }
        
        int stepSize = initialStepSize;
        
        int i = pages.get(pages.lastKey()).getPageNumber();
        int j = i + stepSize;
        
        Page outcome = null;
        
        // First need to move past the end
        boolean done = false;
        while(!done){
            // Try to load the later page
            outcome = loadPage(j);
            
            if (outcome.isLastPage()){
                log.debug("Found last page in stage 1");
                done = true; // Found last page easily
            }else if (outcome.isEmpty()){
                log.debug("Progressing to stage 2");
                break; // Move on to stage 2, narrowing in
            }else{
                log.debug("Didn't jump enough, moving on");
                // Incriment, try again
                i = j;
                j = j + stepSize;
            }
        }
        
        
        
        // Stage 2 = Binary Search using interval bisection
        while(!done){
            log.debug("Decreasing j...");
            // Always move j in first
            int oldJ = j;
            
            j = (j+i)/2;
            
            outcome = loadPage(j);
            
            if (j == i+1){
                log.debug("Found last page in stage 2: SideBySide");
                done = true; // Next to eachother, so finished
                outcome.setLastPage(true); // Set true just in case 50 players
            }else if (outcome.isLastPage()){
                log.debug("Found last page in stage 2: Dead On");
                done = true; // Found last page easily
            }else if (!outcome.isEmpty()){
                log.debug("j no longer empty, move up i");
                // Move i to j, and shrink from original j
                i = j;
                j = oldJ;
            }
        }
        
        // Determine which rank is last
        int lowestElo = outcome.lastPlayer().getElo();
        
        for (Division boundary : Division.values()) {
            if (boundary.elo() > lowestElo){
                break;
            }
            boundariesLeft.remove(boundary);
            boundaries.put(boundary, outcome);
            outcome.getBoundaries().put(boundary, outcome.lastRank());
        }
        
        
    }

    
}
