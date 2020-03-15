package misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TwoKeyMap<K1, K2, V> {
    private List<MapNode<K1, K2, V>> nodes;
    private Set<K1> key1Set = new HashSet<>();
    private Set<K2> key2Set = new HashSet<>();

    public TwoKeyMap() {
        nodes = new ArrayList<>();
    }

    public void put(K1 key1, K2 key2, V value) {
        boolean b1 = key1Set.add(key1);
        boolean b2 = key2Set.add(key2);
        if (b1 && b2) {
            nodes.add(new MapNode<>(key1, key2, value));
        } else if (b1) {
            nodes.removeIf(n -> n.key2.equals(key2));
            key1Set.remove(key1);
            nodes.add(new MapNode<>(key1, key2, value));
        } else if (b2) {
            nodes.removeIf(n -> n.key1.equals(key1));
            key2Set.remove(key2);
            nodes.add(new MapNode<>(key1, key2, value));
        } else {
            nodes.removeIf(n -> n.key1.equals(key1));
            nodes.add(new MapNode<>(key1, key2, value));
        }
    }

    public void removeKey1(K1 key1) {
        List<K1> key1List = new ArrayList<>(key1Set);
        List<K2> key2List = new ArrayList<>(key2Set);
        int index = key1List.indexOf(key1);
        if (index != -1) {
            key1Set.remove(key1);
            key2Set.remove(key2List.get(index));
            nodes.removeIf(n -> n.key1 == key1);
        }
    }

    public void removeKey2(K2 key2) {
        List<K1> key1List = new ArrayList<>(key1Set);
        List<K2> key2List = new ArrayList<>(key2Set);
        int index = key2List.indexOf(key2);
        if (index != -1) {
            key1Set.remove(key1List.get(index));
            key2Set.remove(key2);
            nodes.removeIf(n -> n.key2 == key2);
        }
    }

    public V getValueFromKey1(K1 key1) {
        final int[] index = {-1};
        for (int i = 0; i < nodes.size(); i++) {
            MapNode<K1, K2, V> node = nodes.get(i);
            if (node.key1.equals(key1)) {
                index[0] = i;
            }
        }
        return nodes.get(index[0]).value;
    }

    public V getValueFromKey2(K2 key2) {
        final int[] index = {-1};
        for (int i = 0; i < nodes.size(); i++) {
            MapNode<K1, K2, V> node = nodes.get(i);
            if (node.key2.equals(key2)) {
                index[0] = i;
            }
        }
        return nodes.get(index[0]).value;
    }

    public List<V> getValues() {
        List<V> values = new ArrayList<>();
        nodes.forEach(n -> values.add(n.value));
        return values;
    }

    public String toString() {

        StringBuilder string = new StringBuilder();
        for (MapNode node : nodes) {
            string.append("(").append(node.key1).append(",").append(node.key2).append("), ");
        }
        if (string.length() >= 2) {
            string.delete(string.length() - 2, string.length());
        }
        return string.toString();
    }
}
