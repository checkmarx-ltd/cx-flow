package com.checkmarx.flow.cucumber.common.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PathComparators {

    public static final String INCOMPARABLY = "/incomparably()";

    /*
        Compare with equals
     */
    public static String compare(Object o1, Object o2, String path) {
        String result = null;
        if (o1 == o2) {
        } else if (o1 == null || o2 == null) {
            result = path + INCOMPARABLY;
        } else if (!o1.equals(o2)) {
            result = path;
        }
        return result;
    }

    public static <T> String compare(T t1, T t2, String path, PathComparator<T>... comparators) {
        String result = null;
        if (t1 == t2) {
        } else if (t1 == null || t2 == null) {
            result = path + INCOMPARABLY;
        } else {
            for (PathComparator<T> comparator : comparators) {
                String compared = comparator.compare(t1, t2, path);
                if (compared != null) {
                    result = compared;
                    break;
                }
            }
        }
        return result;
    }

    private static <T, R> String compare(T t1, T t2, String path, Function<T, R> resolver, PathComparator<R> comparator) {
        String result = null;
        if (t1 == t2) {
        } else if (t1 == null || t2 == null) {
            result = path + INCOMPARABLY;
        } else {
            result = comparator.compare(resolver.apply(t1), resolver.apply(t2), path);
        }
        return result;
    }

    private static <T, R> String compare2(T t1, T t2, String path, Function<T, Iterable<R>> resolver, PathComparator<R> itemComparator) {
        String result = null;
        if (t1 == t2) {
        } else if (t1 == null || t2 == null) {
            result = path + INCOMPARABLY;
        } else {
            int i = 0;
            Iterator<R> itr1 = resolver.apply(t1).iterator(), itr2 = resolver.apply(t2).iterator();
            while (itr1.hasNext()) {
                if (!itr2.hasNext()) {
                    result = path + "/size()";
                    break;
                }
                String compared = itemComparator.compare(itr1.next(), itr2.next(), "");
                if (compared != null) {
                    result = path + "[" + i + "]" + compared;
                    break;
                }
                i++;
            }
            if (result == null && itr2.hasNext()) {
                result = path + "/size()";
            }
        }
        return result;
    }

    public static final <T, R> PathComparator<T> construct(String path, Function<T, R> resolver, PathComparator<R> comparator) {
        return (T t1, T t2, String p) -> compare(t1, t2, p + path, resolver, comparator);
    }

    public static <T, R> PathComparator<T> iterate(String path, Function<T, Iterable<R>> resolver, PathComparator<R> comparator) {
        return (T l1, T l2, String p) -> compare2(l1, l2, p + path, resolver, comparator);
    }

    public static void f() {
//        PathComparator<List<String>> p = iterate("", PathComparators::compare);
    }

//    public static <T> PathComparator<Iterable<T>> iterate1(String path, PathComparator<T> comparator) {
//        return (l1, l2, p) -> compare(l1, l2, p + path, null ,comparator);
//    }

    public static final PathComparator<Object> EQUALS_COMP = PathComparators::compare;

    public static final PathComparator<Collection<?>> COLLECTION_SIZE_COMP = (c1, c2, p) -> compare(c1, c2, p, construct("/size()", Collection::size, EQUALS_COMP));

    public static final PathComparator<Collection<?>> COLLECTION_COMP = (c1, c2, p) -> compare(c1, c2, p, COLLECTION_SIZE_COMP, PathComparators::compare);

    public static final PathComparator<Map<?, ?>> MAP_SIZE_COMP = (c1, c2, p) -> compare(c1, c2, p, construct("/size()", Map::size, EQUALS_COMP));

    public static final PathComparator<Map<?, ?>> MAP_COMP = (c1, c2, p) -> compare(c1, c2, p, MAP_SIZE_COMP, PathComparators::compare);
}
