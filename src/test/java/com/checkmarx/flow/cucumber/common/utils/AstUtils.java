package com.checkmarx.flow.cucumber.common.utils;

import com.cx.restclient.ast.dto.sast.report.Finding;
import org.junit.Assert;

import java.util.*;

public class AstUtils {

    public static void validateDescriptions(List<Finding> findings) {

        Map<String, Set<String>> mapDescriptions = new HashMap<>();

        findings.forEach(finding -> {
            Set<String> listDescriptions = mapDescriptions.get(finding.getQueryID());
            if(listDescriptions == null){
                listDescriptions = new HashSet<>();
            }
            listDescriptions.add(finding.getDescription());
            mapDescriptions.put(finding.getQueryID(), listDescriptions);
        });

        Set<String> uniqueDescriptions = new HashSet<>();

        //validate for each queryId there is exactly one corresponding description
        for( Map.Entry<String, Set<String>> entry :mapDescriptions.entrySet()){
            Assert.assertEquals( 1, entry.getValue().size());
            uniqueDescriptions.add((String)entry.getValue().toArray()[0]);
        }

        //validate all descriptions are unique
        Assert.assertEquals(uniqueDescriptions.size(),mapDescriptions.size() );
    }
}
