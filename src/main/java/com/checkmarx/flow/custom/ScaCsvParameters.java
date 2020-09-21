package com.checkmarx.flow.custom;

import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.flow.exception.MachinaRuntimeException;
import com.checkmarx.sdk.dto.ScanResults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ScaCsvParameters {

    VULNERABILITY_ID {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getFinding().getId();
        }
    },

    PACKAGE_NAME {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getVulnerabilityPackage().getName();
        }
    },

    VERSION {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getVulnerabilityPackage().getVersion();
        }
    },

    NEWEST_VERSION {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getVulnerabilityPackage().getNewestVersion();
        }
    },

    PUBLISH_DATE {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getFinding().getPublishDate();
        }
    },

    SEVERITY {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getFinding().getSeverity().name();
        }
    },

    INFORMATION {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getFinding().getDescription();
        }
    },

    REMEDIATION_RECOMMENDATION {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getFinding().getFixResolutionText();
        }
    },

    PACKAGE_PATH {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getFinding().getPackageId();
        }
    },

    CVSS_SCORE {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return String.valueOf(getScaDetail(issue).getFinding().getScore());
        }
    },

    VULNERABILITY_LINK {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return getScaDetail(issue).getVulnerabilityLink();
        }
    },

    APPLICATION {
        @Override
        protected String getCsvValue(ScanResults.XIssue issue, ScanRequest request) {
            return request.getApplication();
        }
    };

    protected abstract String getCsvValue(ScanResults.XIssue issue, ScanRequest request);

    public String getCsvValueWithLog(ScanResults.XIssue issue, ScanRequest request) {
        String csvValue = getCsvValue(issue, request);
        log.debug("{}: {}", name(), csvValue);
        return csvValue;
    }


    protected ScanResults.ScaDetails getScaDetail(ScanResults.XIssue issue) {
        return issue.getScaDetails().stream().findAny().orElseThrow(() -> new MachinaRuntimeException("Can not get SCA details, it is null"));
    }

    public static ScaCsvParameters fromProperties(String name) {
        ScaCsvParameters scaCsvParameters = ScaCsvParameters.valueOf(name.toUpperCase().replace("-", "_"));
        log.debug("Resolved SCA parameter {} to const {}", name, scaCsvParameters);
        return scaCsvParameters;
    }

}