package com.checkmarx.flow.cucumber.integration.ziputils;

import com.checkmarx.flow.utils.ZipUtils;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class zipUtilsSteps {


    @When("Creating zip File at a time")
   public void testZipFileSuccess() throws IOException {
        // Creating Zip File
        ZipUtils zipService = new ZipUtils();
        File zippedFile = zipService.zipToTempFile(".","");

        // Checking presence of zip file
        assertTrue(zippedFile.exists());
        assertTrue(zippedFile.length() >= 1); // Adjust based on size of your file

        // Deleting zip file
        boolean deleted = zippedFile.delete();

        if (deleted) {
            log.info("File successfully deleted!");
        } else {
            log.info("Error deleting file.");
        }
    }
}
