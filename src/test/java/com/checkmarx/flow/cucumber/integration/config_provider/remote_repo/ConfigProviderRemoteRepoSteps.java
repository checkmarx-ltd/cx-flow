package com.checkmarx.flow.cucumber.integration.config_provider.remote_repo;

import com.checkmarx.configprovider.ConfigProvider;
import com.checkmarx.configprovider.dto.SourceProviderType;
import com.checkmarx.configprovider.readers.RepoReader;
import com.checkmarx.flow.CxFlowApplication;
import com.checkmarx.flow.config.GitHubProperties;
import com.checkmarx.flow.config.external.ASTConfig;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.config.AstProperties;
import com.checkmarx.sdk.config.ScaConfig;
import com.checkmarx.sdk.config.ScaProperties;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;

import javax.naming.ConfigurationException;

@SpringBootTest(classes = {CxFlowApplication.class})
@RequiredArgsConstructor
public class ConfigProviderRemoteRepoSteps {

    private final GitHubProperties gitHubProperties;

    private SourceProviderType sourceProviderType;
    private ScaConfig scaConfiguration;
    private ASTConfig astConfiguration;
    private String uid = "12345";

    @Given("github repo contains a Checkmarx configuration")
    public void setSourceProviderType() {
        sourceProviderType = SourceProviderType.GITHUB;
    }

    @When("initializing config provider")
    public void initConfigProvider() throws ConfigurationException {
        String nameSpace = "cxflowtestuser";
        String repoName = "configProviderTestRepo";
        String branchName = "main";

        ConfigProvider configProvider = ConfigProvider.getInstance();
        configProvider.init(uid, new RepoReader(gitHubProperties.getApiUrl(),
                nameSpace, repoName, branchName,
                gitHubProperties.getToken(), sourceProviderType));
    }

    @And("getting {string} config provider configuration")
    public void setVulnerabilityScanner(String scanner) {
        ConfigProvider configProvider = ConfigProvider.getInstance();
        switch (scanner) {
            case ScaProperties.CONFIG_PREFIX:
                scaConfiguration = configProvider.getConfiguration(uid, ScaProperties.CONFIG_PREFIX, ScaConfig.class);
                break;
            case AstProperties.CONFIG_PREFIX:
                astConfiguration = configProvider.getConfiguration(uid, AstProperties.CONFIG_PREFIX, ASTConfig.class);
                break;
            default:
                throw new MachinaRuntimeException("Scanner: " + scanner + " is not yet supported");
        }
    }

    @Then("{string} configuration on Cx-Flow side should match the remote repo configuration data")
    public void validateScannerConfigurationProperties(String scanner) {
        switch (scanner) {
            case ScaProperties.CONFIG_PREFIX:
                assertScaConfiguration();
                break;
            case AstProperties.CONFIG_PREFIX:
                assertAstConfiguration();
                break;
            default:
                throw new MachinaRuntimeException("Scanner: " + scanner + " is not yet supported");
        }

    }

    private void assertAstConfiguration() {
        Assert.assertEquals("https://ast.astcheckmarx.com//config-provider-test", astConfiguration.getApiUrl());
        Assert.assertEquals("default", astConfiguration.getPreset());
        Assert.assertFalse(astConfiguration.isIncremental());
        Assert.assertEquals(System.getenv("JAVA_HOME"), astConfiguration.getClientSecret());
        Assert.assertEquals("astClientId", astConfiguration.getClientId());
    }

    private void assertScaConfiguration() {
        Assert.assertEquals("https://sca.scacheckmarx.com//config-provider-test", scaConfiguration.getAppUrl());
        Assert.assertEquals("https://api.scacheckmarx.com//config-provider-test", scaConfiguration.getApiUrl());
        Assert.assertEquals("https://platform.checkmarx.net//config-provider-test", scaConfiguration.getAccessControlUrl());
        Assert.assertEquals("cxflow", scaConfiguration.getTenant());
        Assert.assertEquals("{LOW=25, MEDIUM=18, HIGH=15}", StringUtils.join(scaConfiguration.getThresholdsSeverity()));
        Assert.assertEquals((Double)8.5, scaConfiguration.getThresholdsScore());
    }
}