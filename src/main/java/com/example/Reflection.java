package com.example;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflection {
    public static void setFieldValue(Object obj, String name, Object val) throws Exception{
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }
    public static Object invokeMethod(Object obj, String name, Class[] parameterTypes, Object[] args) throws Exception{
        Method m = obj.getClass().getDeclaredMethod(name, parameterTypes);
        m.setAccessible(true);
        return m.invoke(obj, args);
    }
}