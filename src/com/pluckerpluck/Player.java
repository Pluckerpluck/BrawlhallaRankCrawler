package com.pluckerpluck;

/**
 *
 * @author Pluckerpluck
 */
public class Player implements Comparable<Player>{

    public enum Tier{
        TIN,
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM,
        DIAMOND;
    }
    
    
    private final int rank;
    private final String name;
    private final Tier tier;
    private final int elo;

    public Player(int rank, String name, Tier tier, int Elo) {
        this.rank = rank;
        this.name = name;
        this.tier = tier;
        this.elo = Elo;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return name;
    }

    public Tier getTier() {
        return tier;
    }

    public int getElo() {
        return elo;
    }
    
    
    @Override
    public int compareTo(Player p) {
        return p.rank - rank;
    }
    
    
    
}
