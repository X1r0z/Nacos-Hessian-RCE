package com.example;

import org.springframework.aop.target.HotSwappableTargetSource;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

public class HashColl {
    public static HashMap<Object, Object> makeMapWrap(Object v1, Object v2) throws Exception {
        return makeMap(new HotSwappableTargetSource(v1), new HotSwappableTargetSource(v2));
    }
    public static HashMap<Object, Object> makeMap(Object v1, Object v2) throws Exception {
        HashMap<Object, Object> map = new HashMap<>();
        Method putValMethod = HashMap.class.getDeclaredMethod("putVal", int.class, Object.class, Object.class, boolean.class, boolean.class);
        putValMethod.setAccessible(true);
        putValMethod.invoke(map, 0, v1, 123, false, true);
        putValMethod.invoke(map, 1, v2, 123, false, true);
        return map;
    }
    public static HashMap<Object, Object> makeMapOriginal(Object v1, Object v2) throws Exception {
        HashMap<Object, Object> s = new HashMap<>();
        Reflection.setFieldValue(s, "size", 2);
        Class<?> nodeC;
        try {
            nodeC = Class.forName("java.util.HashMap$Node");
        }
        catch (ClassNotFoundException e) {
            nodeC = Class.forName("java.util.HashMap$Entry");
        }
        Constructor<?> nodeCons = nodeC.getDeclaredConstructor(int.class, Object.class, Object.class, nodeC);
        nodeCons.setAccessible(true);

        Object tbl = Array.newInstance(nodeC, 2);
        Array.set(tbl, 0, nodeCons.newInstance(0, v1, v1, null));
        Array.set(tbl, 1, nodeCons.newInstance(0, v2, v2, null));
        Reflection.setFieldValue(s, "table", tbl);
        return s;
    }

    public static String unhash(int hash) {
        int target = hash;
        StringBuilder answer = new StringBuilder();
        if (target < 0) {
            // String with hash of Integer.MIN_VALUE, 0x80000000
            answer.append("\\u0915\\u0009\\u001e\\u000c\\u0002");

            if (target == Integer.MIN_VALUE)
                return answer.toString();
            // Find target without sign bit set
            target = target & Integer.MAX_VALUE;
        }

        unhash0(answer, target);
        return answer.toString();
    }


    private static void unhash0(StringBuilder partial, int target) {
        int div = target / 31;
        int rem = target % 31;

        if (div <= Character.MAX_VALUE) {
            if ( div != 0 )
                partial.append((char) div);
            partial.append((char) rem);
        }
        else {
            unhash0(partial, div);
            partial.append((char) rem);
        }
    }
}
