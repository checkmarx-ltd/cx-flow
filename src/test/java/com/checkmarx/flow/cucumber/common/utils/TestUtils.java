package com.checkmarx.flow.cucumber.common.utils;

import com.checkmarx.flow.CxFlowRunner;
import org.springframework.boot.DefaultApplicationArguments;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

public class TestUtils {

    private interface Revertible {
        void revert();
    }

    private static final Stack<Revertible> propertiesChanges = new Stack<>();

    public static void changePropertiesBack() {
        while (!propertiesChanges.isEmpty()) {
            propertiesChanges.pop().revert();
        }
    }

    public static void runCxFlow(CxFlowRunner runner, String args) throws InvocationTargetException {
        runner.run(new DefaultApplicationArguments(args.split(" ")));
    }
}
