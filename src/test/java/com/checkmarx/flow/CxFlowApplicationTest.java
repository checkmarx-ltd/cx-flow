package com.checkmarx.flow;

import com.checkmarx.flow.utils.JasyptConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {JasyptConfig.class})

public class CxFlowApplicationTest {

    @Test
    public void contextLoads() throws Exception {
    }

}