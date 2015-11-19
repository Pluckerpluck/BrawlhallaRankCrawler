package com.pluckerpluck;

import com.pluckerpluck.Bracket.Brackets;
import com.pluckerpluck.Bracket.Division;
import com.pluckerpluck.Player.Tier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pluckerpluck
 */
public class Page implements Comparable<Page>{
  
    final static org.slf4j.Logger log = LoggerFactory.getLogger(Page.class);
    
    private final ArrayList<Player> players;
    private final int pageNumber;
    private final Brackets bracket;
    private final Map<Division, Integer> boundaries;
    private boolean isLastPage;
    

    public Page(int pageNumber, Brackets bracket) {
        this.pageNumber = pageNumber;
        this.bracket = bracket;
        players = new ArrayList();
        boundaries = new HashMap();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public Brackets getBracket() {
        return bracket;
    }
    
    public boolean isLastPage(){
        return isLastPage;
    }

    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }
    
    
    
    public boolean isEmpty(){
        return players.isEmpty();
    }

    public Map<Division, Integer> getBoundaries() {
        return boundaries;
    }
    
    public boolean containsBoundary(Division boundary){
        return boundaries.containsKey(boundary);
    }
    
    public int firstRank(){
        return 50*(pageNumber-1) + 1;
    }
    
    public int lastRank(){
        return 50*(pageNumber-1) + players.size();
    }
    
    public Player firstPlayer(){
        return players.get(0);
    }
    
    public Player lastPlayer(){
        return players.get(players.size() - 1);
    }
    
    /**
     * Loads the page from brawlhalla.com
     */
    public void loadPage(){
        String url = bracket.getPageURL(pageNumber);
        
        RankDistribution.requestWait();
        log.info("Loading {}, page {}", bracket, pageNumber);

        try {
            Document doc = Jsoup.connect(url).get();
            
            Elements rows = doc.select("tr:not(#rheader)");
            
            int i = 0;
            if (bracket == Brackets.US_2v2 || bracket == Brackets.EU_2v2 || bracket == Brackets.SEA_2v2){
                i = 1;
            }
            
            for (Element row : rows) {
                String rankS = row.child(1).html();
                rankS = rankS.replace(",", "");
                int rank = Integer.parseInt(rankS);
                String name = row.child(2).html();
                
                String tierS = row.child(3+i).child(0).className();
                
                Tier tier;
                switch (tierS){
                    case "bhicon stardiamond":
                        tier = Tier.DIAMOND;
                        break;
                    case "bhicon starplatinum":
                        tier = Tier.PLATINUM;
                        break;
                    case "bhicon stargold":
                        tier = Tier.GOLD;
                        break;
                    case "bhicon starsilver":
                        tier = Tier.SILVER;
                        break;
                    case "bhicon starbronze":
                        tier = Tier.BRONZE;
                        break;
                    case "bhicon startin":
                        tier = Tier.TIN;
                        break;
                    default:
                        throw new IllegalArgumentException("Not a valid tier: " + tierS);
                }
                
                int elo = Integer.parseInt(row.child(5).html());
                
                Player player = new Player(rank, name, tier, elo);
                players.add(player);
            }
            
            log.debug("{} rows processed",  rows.size());
            
        } catch (IOException ex) {
            log.error("{}", ex.getMessage());
        }
        
        checkForBoundaries();
        
        if (players.size() < 50 && !players.isEmpty()){
            setLastPage(true);
        }
    }
    
    private void checkForBoundaries(){
        // If rank 1, fix stuff
        if (!players.isEmpty() && players.get(0).getRank() == 1){
            for (Division division : Division.values()) {
                // No top tier players exist!
                if (players.get(0).getElo() <= division.elo()){
                    boundaries.put(division, 0);
                }
            }
        }
        
        for (int i = 1; i < players.size(); i++) {
            Player player1 = players.get(i-1); // Higher ranked player
            Player player2 = players.get(i);
            
            // If same Elo then not a boundary point
            if (player1.getElo() == player2.getElo()){
                continue;
            }
            
            for (Division division : Division.values()) {
                if (division.elo() > player2.getElo()
                        && division.elo() <= player1.getElo()){
                    
                    log.info("Found boundary {}", division);
                    boundaries.put(division, player1.getRank());
                    
                    break; // Can only be in one division
                }     
            }
            
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + this.pageNumber;
        hash = 79 * hash + Objects.hashCode(this.bracket);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Page other = (Page) obj;
        if (this.pageNumber != other.pageNumber) {
            return false;
        }
        if (this.bracket != other.bracket) {
            return false;
        }
        return true;
    }

    
    
    @Override
    public int compareTo(Page p) {
        return p.pageNumber - pageNumber;
    }
    
    
}
