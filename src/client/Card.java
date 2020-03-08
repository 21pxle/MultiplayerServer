package client;

import javafx.scene.image.Image;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;

public class Card implements Serializable {
    public static final Card ACE_OF_SPADES = new Card("As");
    public static Image CARD_BACK;

    static {
        try {
            CARD_BACK = new Image(new FileInputStream("res/CardBack.jpg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private final String suits = "cdhs", ranks = "A23456789TJQK";
    private int rank, suit;
    /**
     * Constructor for the Card class. Note that the string used is not the full name of the card,
     * but it is the abbreviated form of the card. For example, for a card that states, "Two of Hearts,"
     * use "2h" in the constructor.
     * @param s The short name for the card.
     */
    public Card(String s) {
        rank = ranks.indexOf(s.charAt(0)) + 1;
        suit = suits.indexOf(s.charAt(1)) + 1;
    }

    public Card(int rank, int suit) {
        this.rank = rank;
        this.suit = suit;
    }

    /**
     * @return The numerical rank of the card.
     */
    public int getRank() {
        return rank;
    }

    /**
     * @return The full name of the suit of the card.
     */
    public String getRankName() {
        switch (rank) {
            case 1: return "Ace";
            case 11: return "Jack";
            case 12: return "Queen";
            case 13: return "King";
        }
        if (rank < 11 && rank > 1)
            return rank + "";
        return "Joker";
    }

    /**
     * @return The numerical suit of the card.
     */
    public int getSuit() {
        return suit;
    }
    /**
     * @return The full name of the suit of the card.
     */
    public String getSuitName() {
        switch (suit) {
            case 1: return "Clubs";
            case 2: return "Diamonds";
            case 3: return "Hearts";
            case 4: return "Spades";
        }
        return "Joker";
    }

    /**
     * @return The full name of the card.
     */
    @Override
    public String toString() {
        return getRankName() + " of " + getSuitName();
    }

    /**
     *
     * @return The image of the card if the image can be found in the specified directory.
     * @throws FileNotFoundException If the image cannot be found.
     */
    public Image getImage() throws FileNotFoundException {
        return new Image(new FileInputStream("res/" + toString() + ".jpg"));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Card) {
            return toString().equals(obj.toString());
        }
        return false;
    }

    /**
     *
     * @return The short name of the card.
     */
    public String getShortName() {
        return ranks.charAt(rank - 1) + "" + suits.charAt(suit - 1);
    }
}