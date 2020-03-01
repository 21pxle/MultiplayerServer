package misc;

import java.util.*;

public class TwoKeyMap<K1, K2, V> {
    private List<MapNode<K1, K2, V>> nodes;
    private Set<K1> key1Set;
    private Set<K2> key2Set;
    private List<V> values;

    public TwoKeyMap() {
        nodes = new ArrayList<>();
        values = new ArrayList<>();
        key1Set = new HashSet<>();
        key2Set = new HashSet<>();
    }

    public void put(K1 key1, K2 key2, V value) {
        if (key1Set.add(key1) && key2Set.add(key2)) {
            nodes.add(new MapNode<>(key1, key2, value));
            values.add(value);
        } else {
            key1Set.remove(key1);
            key2Set.remove(key2);
        }
    }

    public void removeKey1(K1 key1) {
        List<K1> key1List = new ArrayList<>(key1Set);
        List<K2> key2List = new ArrayList<>(key2Set);
        int index = key1List.indexOf(key1);
        if (index != -1) {
            key1Set.remove(key1);
            key2Set.remove(key2List.get(index));
            nodes.remove(new MapNode<>(key1, key2List.get(index), values.get(index)));
            values.remove(index);
        }
    }

    public void removeKey2(K2 key2) {
        List<K1> key1List = new ArrayList<>(key1Set);
        List<K2> key2List = new ArrayList<>(key2Set);
        int index = key2List.indexOf(key2);
        if (index != -1) {
            key1Set.remove(key1List.get(index));
            key2Set.remove(key2);
            nodes.remove(new MapNode<>(key1List.get(index), key2, values.get(index)));
            values.remove(index);
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

    public V getValueFromKey2(K2 key2) {final int[] index = {-1};
        for (int i = 0; i < nodes.size(); i++) {
            MapNode<K1, K2, V> node = nodes.get(i);
            if (node.key2.equals(key2)) {
                index[0] = i;
            }
        }
        return nodes.get(index[0]).value;
    }

    public List<V> getValues() {
        return values;
    }

    public String toString() {

        StringBuilder string = new StringBuilder();
        for (MapNode node : nodes) {
            string.append("(").append(node.key1).append(",").append(node.key2).append("), ");;
        }
        if (string.length() >= 2) {
            string.delete(string.length() - 2, string.length());
        }
        return string.toString();
    }

    public void putNode(MapNode<K1, K2, V> node) {
        if (key1Set.add(node.key1) && key2Set.add(node.key2)) {
            nodes.add(node);
            values.add(node.value);
        } else {
            key1Set.remove(node.key1);
            key2Set.remove(node.key2);
        }
    }
}
