package com.checkmarx.flow.service;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.GitLabProperties;
import com.checkmarx.flow.config.JiraProperties;
import com.checkmarx.flow.dto.CxProfile;
import com.checkmarx.flow.dto.Sources;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.config.CxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@Import(GitLabProperties.class)
@SpringBootTest
public class HelperServiceTest {

    @Test
    public void testGetPresetFromSources() {
        FlowProperties properties = new FlowProperties();
        CxProperties cxProperties = new CxProperties();
        JiraProperties jiraProperties = new JiraProperties();
        cxProperties.setScanPreset(Constants.CX_DEFAULT_PRESET);

        CxScannerService cxScannerService = new CxScannerService(cxProperties,null, null, null, null );

        HelperService helperService = new HelperService(properties, cxScannerService,
                                                        jiraProperties, null);
        Sources sources = new Sources();
        Sources.Source src1 = new Sources.Source();
        src1.setFile("abc.java");
        src1.setPath("abc.java");
        Sources.Source src2 = new Sources.Source();
        src2.setFile("abc.html");
        src2.setPath("abc.html");
        Sources.Source src3 = new Sources.Source();
        src3.setFile("abc.css");
        src3.setPath("abc.css");
        Sources.Source src4 = new Sources.Source();
        src4.setFile("buildspec.yml");
        src4.setPath("buildspec.yml");
        Map<String, Integer> sourceWeight = new HashMap<>();
        sourceWeight.put("Java", 65);
        sourceWeight.put("CSS", 15);
        sourceWeight.put("HTML", 20);
        sources.setLanguageStats(sourceWeight);
        sources.setSources(Arrays.asList(src1, src2,src3,src4));
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(HelperService.class.getResource(".").getPath());
        File file = new File(
                getClass().getClassLoader().getResource("CxProfile.json").getFile()
        );
        try {
            CxProfile[] cxProfiles = mapper.readValue(file, CxProfile[].class);
            helperService.setProfiles(Arrays.asList(cxProfiles));
            String preset = helperService.getPresetFromSources(sources);
            assertEquals(preset, "Checkmarx Express");
        }catch (IOException e){
            fail("Unexpected IO Exception");
        }
    }
}