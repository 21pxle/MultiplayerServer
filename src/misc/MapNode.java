package misc;

public class MapNode<K1, K2, V> {
    K1 key1;
    K2 key2;
    V value;

    public MapNode(K1 key1, K2 key2, V value) {
        this.key1 = key1;
        this.key2 = key2;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MapNode) {
            MapNode node = (MapNode) obj;
            return node.key1.equals(key1) &&
                    node.key2.equals(key2) &&
                    node.value.equals(value);

        }
        return false;
    }
}
