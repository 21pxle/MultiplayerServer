package misc;

import client.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ListExtension {
    public static String cardListToString(List<Card> cards) {
        if (cards.size() == 0) {
            return "";
        }
        StringBuilder string = new StringBuilder();
        for (Card card : cards) {
            string.append(card.getShortName()).append(" ");
        }
        return string.toString().trim();
    }

    public static List<Card> stringToCardList(String cardList) {
        if (cardList.length() == 0) {
            return new ArrayList<>();
        }
        List<Card> cards = new ArrayList<>();
        String[] cardArray = cardList.split(" ");
        for (String card : cardArray) {
            cards.add(new Card(card));
        }
        return cards;
    }

    public static String stringListToString(Collection<String> list) {
        StringBuilder string = new StringBuilder();
        for (String str : list) {
            string.append(str).append(":");
        }
        string.deleteCharAt(string.lastIndexOf(":"));
        return string.toString();
    }

    public static List<String> stringToStringList(String string) {
        String[] strings = string.split(":");
        return new ArrayList<>(Arrays.asList(strings));
    }
}
