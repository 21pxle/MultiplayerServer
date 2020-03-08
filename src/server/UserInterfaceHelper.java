package server;

import client.Card;
import misc.ListExtension;

import java.util.List;

public class UserInterfaceHelper {

    public static String init(String user, List<Card> cards, int health, int maxHealth, int index, String imgString) {
        return user + "\t" + ListExtension.cardListToString(cards)  + "\tADD\t" + health + "\t" + maxHealth + "\t" + index + "\t" + imgString;
    }

    public static String modifyBSF(String user, List<Card> cards, List<Card> defenderCards, int health, List<String> turnQueue) {
        return user + "\t" + ListExtension.cardListToString(cards) + "\tMF\t" + ListExtension.cardListToString(defenderCards) + "\t" + health +
                "\t" + ListExtension.stringListToString(turnQueue);
    }

    public static String modifyBSS(String user, List<Card> cards, int health) {
        return user + "\t" + ListExtension.cardListToString(cards) + "\tMS\t" + health;
    }

    public static String modify(String user, List<Card> cards, int health) {
        return user + "\t" + ListExtension.cardListToString(cards) + "\tMOD\t" + health;
    }

    public static String modifyHealth(String user, int health) {
        return user + "\t" + health + "\tMH";
    }

    public static String removeCard(String user, Card card) {
        return user + "\t" + card.getShortName() + "\tRC";
    }

    public static String removeCards(String user, List<Card> cards) {
        return user + "\t" + ListExtension.cardListToString(cards) + "\tRCs";
    }

    public static String modifyCards(String user, List<Card> cards) {
        return user + "\t" + ListExtension.cardListToString(cards) + "\tMC";
    }

    public static String clearCards(String user) {
        return user + "\t\tCC";
    }
}
