package com.checkmarx.flow.cucumber.common.utils;

public interface PathComparator<T> {

    String compare(T t1, T t2, String path);
}
