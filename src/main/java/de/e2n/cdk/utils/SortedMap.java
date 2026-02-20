package de.e2n.cdk.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * insertion-ordered map for consistent & reproducible cdk-stack outputs (e.g. buildspec.yml entry order)
 */
public class SortedMap {

    @SuppressWarnings ("unchecked")
    public static <K, V> Map<K,V> of(Object... keyVals) {
        if (keyVals.length % 2 != 0) {
            throw new IllegalArgumentException("Uneven number of arguments. Keys and values must be paired.");
        }

        LinkedHashMap<K,V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            K key = (K) keyVals[i];
            V value = (V) keyVals[i + 1];
            if (map.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate key: " + key);
            }
            map.put(key, value);
        }

        return map;
    }

}
