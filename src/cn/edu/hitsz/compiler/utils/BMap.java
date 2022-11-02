package cn.edu.hitsz.compiler.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 双射类，提供K->V和V->K的唯一双向映射
 * @param <K>
 * @param <V>
 */
public class BMap<K, V> {
    private final Map<K, V> KVMap = new HashMap<>();
    private final Map<V, K> VKMap = new HashMap<>();

    public void removeByKey(K key) {
        VKMap.remove(KVMap.remove(key));
    }

    public void removeByValue(V value) {
        KVMap.remove(VKMap.remove(value));
    }

    public boolean containKey(K key) {
        return KVMap.containsKey(key);
    }

    public boolean containValue(V value) {
        return VKMap.containsKey(value);
    }

    public void replace(K key, V value) {
        // 对于双射关系, 将会删除交叉项
        removeByKey(key);
        removeByValue(value);
        KVMap.put(key, value);
        VKMap.put(value, key);
    }

    public V getByKey(K key) {
        return KVMap.get(key);
    }

    public K getByValue(V value) {
        return VKMap.get(value);
    }

    public Iterable<K> getAllKeys() {
        return KVMap.keySet();
    }
}
