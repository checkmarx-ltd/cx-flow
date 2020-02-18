package com.checkmarx.flow.cucumber.common.utils;

import com.checkmarx.flow.CxFlowRunner;
import com.checkmarx.flow.cucumber.component.parse.TestContext;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Stack;

public class TestUtils {
    public static InputStream getResourceAsStream(String relativePath) {
        String srcResourcePath = Paths.get(TestContext.CUCUMBER_DATA_DIR, relativePath)
                .toString();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader.getResourceAsStream(srcResourcePath);
    }

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
