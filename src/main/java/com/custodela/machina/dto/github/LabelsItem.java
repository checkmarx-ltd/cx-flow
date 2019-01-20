package com.custodela.machina.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;

public class LabelsItem{

	@JsonProperty("default")
	private boolean jsonMemberDefault;

	@JsonProperty("color")
	private String color;

	@JsonProperty("name")
	private String name;

	@JsonProperty("id")
	private int id;

	@JsonProperty("url")
	private String url;

    @ConstructorProperties({"jsonMemberDefault", "color", "name", "id", "url"})
    public LabelsItem(boolean jsonMemberDefault, String color, String name, int id, String url) {
        this.jsonMemberDefault = jsonMemberDefault;
        this.color = color;
        this.name = name;
        this.id = id;
        this.url = url;
    }

    public LabelsItem() {
    }

    public static LabelsItemBuilder builder() {
        return new LabelsItemBuilder();
    }

    public boolean isJsonMemberDefault() {
        return this.jsonMemberDefault;
    }

    public String getColor() {
        return this.color;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.id;
    }

    public String getUrl() {
        return this.url;
    }

    public void setJsonMemberDefault(boolean jsonMemberDefault) {
        this.jsonMemberDefault = jsonMemberDefault;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String toString() {
        return "LabelsItem(jsonMemberDefault=" + this.isJsonMemberDefault() + ", color=" + this.getColor() + ", name=" + this.getName() + ", id=" + this.getId() + ", url=" + this.getUrl() + ")";
    }

    public static class LabelsItemBuilder {
        private boolean jsonMemberDefault;
        private String color;
        private String name;
        private int id;
        private String url;

        LabelsItemBuilder() {
        }

        public LabelsItem.LabelsItemBuilder jsonMemberDefault(boolean jsonMemberDefault) {
            this.jsonMemberDefault = jsonMemberDefault;
            return this;
        }

        public LabelsItem.LabelsItemBuilder color(String color) {
            this.color = color;
            return this;
        }

        public LabelsItem.LabelsItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LabelsItem.LabelsItemBuilder id(int id) {
            this.id = id;
            return this;
        }

        public LabelsItem.LabelsItemBuilder url(String url) {
            this.url = url;
            return this;
        }

        public LabelsItem build() {
            return new LabelsItem(jsonMemberDefault, color, name, id, url);
        }

        public String toString() {
            return "LabelsItem.LabelsItemBuilder(jsonMemberDefault=" + this.jsonMemberDefault + ", color=" + this.color + ", name=" + this.name + ", id=" + this.id + ", url=" + this.url + ")";
        }
    }
}
