package com.checkmarx.flow.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sources {
    List<Source> sources;
    Map<String, Integer> languageStats;

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public void addSource(String path, String file){
        if(sources == null){
            sources = new ArrayList<>();
        }
        Source s = new Source();
        s.setFile(file);
        s.setPath(path);
        sources.add(s);
    }

    public Map<String, Integer> getLanguageStats() {
        return languageStats;
    }

    public void setLanguageStats(Map<String, Integer> languageStats) {
        this.languageStats = languageStats;
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
