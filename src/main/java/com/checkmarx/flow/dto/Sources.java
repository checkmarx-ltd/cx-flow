package com.checkmarx.flow.dto;

import java.util.List;

public class Sources {
    List<Source> srouces;

    public List<Source> getSrouces() {
        return srouces;
    }

    public void setSrouces(List<Source> srouces) {
        this.srouces = srouces;
    }

    public static class Source{
        private String path;
        private String file;
        //...more?

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }
}
