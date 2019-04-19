package com.custodela.machina.dto;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;

/**
 * Representation of Issues for a particular product/scan
 */
public class ScanResults{

    private Boolean osa;
    private String  projectId;
    private String  team;
    private String  project;
    private String  link;
    private String  files;
    private String  loc;
    private String  scanType;
    private List<XIssue> xIssues;
    private String output;

    @ConstructorProperties({"osa", "projectId", "team", "project", "link", "files", "loc", "scanType", "xIssues"})
    public ScanResults(Boolean osa, String projectId, String team, String project, String link, String files, String loc, String scanType, List<XIssue> xIssues) {
        this.osa = osa;
        this.projectId = projectId;
        this.team = team;
        this.project = project;
        this.link = link;
        this.files = files;
        this.loc = loc;
        this.scanType = scanType;
        this.xIssues = xIssues;
    }


    public ScanResults() {
    }

    public static ScanResultsBuilder builder() {
        return new ScanResultsBuilder();
    }

    public String getProjectId(){
        return this.projectId;
    }

    public void setProjectId(String projectId){
        this.projectId = projectId;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public List<XIssue> getxIssues() {
        return xIssues;
    }

    public void setxIssues(List<XIssue> xIssues) {
        this.xIssues = xIssues;
    }

    public Boolean getOsa() {
        return this.osa;
    }

    public String getScanType() {
        return this.scanType;
    }

    public List<XIssue> getXIssues() {
        return this.xIssues;
    }

    public String getLink(){
        return this.link;
    }

    public void setLink(String link){
        this.link = link;
    }

    public String getFiles(){
        return this.files;
    }

    public void setFiles(String files){
        this.files = files;
    }

    public String getLoc(){
        return this.loc;
    }

    public void setLoc(String loc){
        this.loc = loc;
    }

    public void setOsa(Boolean osa) {
        this.osa = osa;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public void setXIssues(List<XIssue> xIssues) {
        this.xIssues = xIssues;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String toString() {
        return "ScanResults(osa=" + this.getOsa()  + ", link=" + this.getLink() + ", files=" + this.getFiles() + ", loc=" + this.getLoc() + ", scanType=" + this.getScanType() + ", xIssues=" + this.getXIssues() + ")";
    }

    public static class XIssue{
        private String vulnerability;
        private String similarityId;
        private String cwe;
        private String cve;
        private String description;
        private String language;
        private String severity;
        private String link;
        private String filename;
        private String gitUrl;
        private List<OsaDetails> osaDetails;
        private Map<Integer, String>  details;

        @ConstructorProperties({"vulnerability", "similarityId", "cwe", "cve", "description", "language",
                "severity", "link", "filename", "gitUrl", "osaDetails", "details"})
        XIssue(String vulnerability, String similarityId, String cwe, String cve, String description, String language,
               String severity, String link, String filename, String gitUrl, List<OsaDetails> osaDetails, Map<Integer, String> details) {
            this.vulnerability = vulnerability;
            this.similarityId = similarityId;
            this.cwe = cwe;
            this.cve = cve;
            this.description = description;
            this.language = language;
            this.severity = severity;
            this.link = link;
            this.filename = filename;
            this.gitUrl = gitUrl;
            this.osaDetails = osaDetails;
            this.details = details;
        }

        public static XIssueBuilder builder() {
            return new XIssueBuilder();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            XIssue issue = (XIssue) o;

            if (!vulnerability.equals(issue.vulnerability)) return false;
            return filename.equals(issue.filename);
        }

        @Override
        public int hashCode() {
            int result = vulnerability.hashCode();
            result = 5225 * result + filename.hashCode();
            return result;
        }

        public String getSimilarityId() {
            return similarityId;
        }

        public void setSimilarityId(String similarityId) {
            this.similarityId = similarityId;
        }

        public String getVulnerability() {
            return this.vulnerability;
        }

        public String getCwe() {
            return this.cwe;
        }

        public String getCve() {
            return this.cve;
        }

        public String getDescription() {
            return this.description;
        }

        public String getLanguage() {
            return this.language;
        }

        public String getSeverity() {
            return this.severity;
        }

        public String getLink() {
            return this.link;
        }

        public String getFilename() {
            return this.filename;
        }

        public String getGitUrl() {
            return this.gitUrl;
        }

        public List<OsaDetails> getOsaDetails() {
            return this.osaDetails;
        }

        public Map<Integer, String> getDetails() {
            return this.details;
        }

        public void setVulnerability(String vulnerability) {
            this.vulnerability = vulnerability;
        }

        public void setCwe(String cwe) {
            this.cwe = cwe;
        }

        public void setCve(String cve) {
            this.cve = cve;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public void setGitUrl(String gitUrl) {
            this.gitUrl = gitUrl;
        }

        public void setOsaDetails(List<OsaDetails> osaDetails) {
            this.osaDetails = osaDetails;
        }

        public void setDetails(Map<Integer, String> details) {
            this.details = details;
        }

        public static class XIssueBuilder {
            private String vulnerability;
            private String similarityId;
            private String cwe;
            private String cve;
            private String description;
            private String language;
            private String severity;
            private String link;
            private String file;
            private List<OsaDetails> osaDetails;
            private Map<Integer, String> details;

            XIssueBuilder() {
            }

            public XIssue.XIssueBuilder vulnerability(String vulnerability) {
                this.vulnerability = vulnerability;
                return this;
            }

            public XIssue.XIssueBuilder similarityId(String similarityId) {
                this.similarityId = similarityId;
                return this;
            }

            public XIssue.XIssueBuilder cwe(String cwe) {
                this.cwe = cwe;
                return this;
            }

            public XIssue.XIssueBuilder cve(String cve) {
                this.cve = cve;
                return this;
            }

            public XIssue.XIssueBuilder description(String description) {
                this.description = description;
                return this;
            }

            public XIssue.XIssueBuilder language(String language) {
                this.language = language;
                return this;
            }

            public XIssue.XIssueBuilder severity(String severity) {
                this.severity = severity;
                return this;
            }

            public XIssue.XIssueBuilder link(String link) {
                this.link = link;
                return this;
            }

            public XIssue.XIssueBuilder file(String file) {
                this.file = file;
                return this;
            }

            public XIssue.XIssueBuilder osaDetails(List<OsaDetails> osaDetails) {
                this.osaDetails = osaDetails;
                return this;
            }

            public XIssue.XIssueBuilder details(Map<Integer, String> details) {
                this.details = details;
                return this;
            }

            public XIssue build() {
                return new XIssue(vulnerability, similarityId, cwe, cve, description, language, severity, link, file, "", osaDetails, details);
            }

            public String toString() {
                return "ScanResults.XIssue.XIssueBuilder(simiarlityId="+ this.similarityId +",vulnerability=" + this.vulnerability + ", cwe=" + this.cwe + ", cve=" + this.cve + ", description=" + this.description + ", language=" + this.language + ", severity=" + this.severity + ", link=" + this.link + ", filename=" + this.file + ", osaDetails=" + this.osaDetails + ", details=" + this.details + ")";
            }
        }
    }

    public static class OsaDetails {
        private String cve;
        private String description;
        private String recommendation;
        private String severity;
        private String url;
        private String version;

        @ConstructorProperties({"cve", "description", "recommendation", "severity", "url", "version"})
        OsaDetails(String cve, String description, String recommendation, String severity, String url, String version) {
            this.cve = cve;
            this.description = description;
            this.recommendation = recommendation;
            this.severity = severity;
            this.url = url;
            this.version = version;
        }

        public static OsaDetailsBuilder builder() {
            return new OsaDetailsBuilder();
        }

        public String getCve() {
            return this.cve;
        }

        public String getDescription() {
            return this.description;
        }

        public String getRecommendation() {
            return this.recommendation;
        }

        public String getSeverity() {
            return this.severity;
        }

        public String getUrl() {
            return this.url;
        }

        public String getVersion() {
            return this.version;
        }

        public void setCve(String cve) {
            this.cve = cve;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public static class OsaDetailsBuilder {
            private String cve;
            private String description;
            private String recommendation;
            private String severity;
            private String url;
            private String version;

            OsaDetailsBuilder() {
            }

            public OsaDetails.OsaDetailsBuilder cve(String cve) {
                this.cve = cve;
                return this;
            }

            public OsaDetails.OsaDetailsBuilder description(String description) {
                this.description = description;
                return this;
            }

            public OsaDetails.OsaDetailsBuilder recommendation(String recommendation) {
                this.recommendation = recommendation;
                return this;
            }

            public OsaDetails.OsaDetailsBuilder severity(String severity) {
                this.severity = severity;
                return this;
            }

            public OsaDetails.OsaDetailsBuilder url(String url) {
                this.url = url;
                return this;
            }

            public OsaDetails.OsaDetailsBuilder version(String version) {
                this.version = version;
                return this;
            }

            public OsaDetails build() {
                return new OsaDetails(cve, description, recommendation, severity, url, version);
            }

            public String toString() {
                return "ScanResults.OsaDetails.OsaDetailsBuilder(cve=" + this.cve + ", description=" + this.description + ", recommendation=" + this.recommendation + ", severity=" + this.severity + ", url=" + this.url + ", version=" + this.version + ")";
            }
        }
    }

    public static class ScanResultsBuilder {
        private Boolean osa;
        private String projectId;
        private String team;
        private String project;
        private String link;
        private String files;
        private String loc;
        private String scanType;
        private List<XIssue> xIssues;

        ScanResultsBuilder() {
        }

        public ScanResults.ScanResultsBuilder osa(Boolean osa) {
            this.osa = osa;
            return this;
        }


        public ScanResults.ScanResultsBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public ScanResults.ScanResultsBuilder project(String project) {
            this.project = project;
            return this;
        }

        public ScanResults.ScanResultsBuilder team(String team) {
            this.team = team;
            return this;
        }

        public ScanResults.ScanResultsBuilder link(String link) {
            this.link = link;
            return this;
        }

        public ScanResults.ScanResultsBuilder files(String filesScanned) {
            this.files = filesScanned;
            return this;
        }

        public ScanResults.ScanResultsBuilder loc(String locScanned) {
            this.loc = locScanned;
            return this;
        }

        public ScanResults.ScanResultsBuilder scanType(String scanType) {
            this.scanType = scanType;
            return this;
        }

        public ScanResults.ScanResultsBuilder xIssues(List<XIssue> xIssues) {
            this.xIssues = xIssues;
            return this;
        }

        public ScanResults build() {
            return new ScanResults(osa, projectId, team, project, link, files, loc, scanType, xIssues);
        }

        public String toString() {
            return "ScanResults.ScanResultsBuilder(osa=" + this.osa + ", link=" + this.link + ", files=" + this.files + ", loc=" + this.loc + ", scanType=" + this.scanType + ", xIssues=" + this.xIssues + ")";
        }
    }
}
