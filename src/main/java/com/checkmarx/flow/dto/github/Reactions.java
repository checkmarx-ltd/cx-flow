package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)

public class Reactions {


    @JsonProperty("url")
    String url;

    @JsonProperty("total_count")
    int totalCount;

    @JsonProperty("laugh")
    int laugh;

    @JsonProperty("hooray")
    int hooray;

    @JsonProperty("confused")
    int confused;

    @JsonProperty("heart")
    int heart;

    @JsonProperty("rocket")
    int rocket;

    @JsonProperty("eyes")
    int eyes;


    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    public int getTotalCount() {
        return totalCount;
    }



    public void setLaugh(int laugh) {
        this.laugh = laugh;
    }
    public int getLaugh() {
        return laugh;
    }

    public void setHooray(int hooray) {
        this.hooray = hooray;
    }
    public int getHooray() {
        return hooray;
    }

    public void setConfused(int confused) {
        this.confused = confused;
    }
    public int getConfused() {
        return confused;
    }

    public void setHeart(int heart) {
        this.heart = heart;
    }
    public int getHeart() {
        return heart;
    }

    public void setRocket(int rocket) {
        this.rocket = rocket;
    }
    public int getRocket() {
        return rocket;
    }

    public void setEyes(int eyes) {
        this.eyes = eyes;
    }
    public int getEyes() {
        return eyes;
    }
}
