package com.checkmarx.flow.dto.azure;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "self",
        "repository",
        "threads"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentLinks {

    @JsonProperty("self")
    private  CommentSelf self;

    @JsonProperty("repository")
    private Repository_ repository;


    @JsonProperty("threads")
    private Thread threads;

    @JsonProperty("repository")
    public void setRepository(Repository_ repository){
        this.repository=repository;
    }

    @JsonProperty("self")
    public void setSelf(CommentSelf self){
        this.self=self;
    }

    @JsonProperty("self")
    public CommentSelf getSelf(){
        return self;
    }

    @JsonProperty("repository")
    public  Repository_ getRepository()
    {
        return repository;
    }
    @JsonProperty("threads")
    public  void setThreads(Thread threads){
        this.threads=threads;
    }

    @JsonProperty("threads")
    public  Thread getThreads(){
        return threads;
    }



}
