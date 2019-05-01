package com.checkmarx.flow.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;

public class Creator{

	@JsonProperty("gists_url")
	private String gistsUrl;

	@JsonProperty("repos_url")
	private String reposUrl;

	@JsonProperty("following_url")
	private String followingUrl;

	@JsonProperty("starred_url")
	private String starredUrl;

	@JsonProperty("login")
	private String login;

	@JsonProperty("followers_url")
	private String followersUrl;

	@JsonProperty("type")
	private String type;

	@JsonProperty("url")
	private String url;

	@JsonProperty("subscriptions_url")
	private String subscriptionsUrl;

	@JsonProperty("received_events_url")
	private String receivedEventsUrl;

	@JsonProperty("avatar_url")
	private String avatarUrl;

	@JsonProperty("events_url")
	private String eventsUrl;

	@JsonProperty("html_url")
	private String htmlUrl;

	@JsonProperty("site_admin")
	private boolean siteAdmin;

	@JsonProperty("id")
	private int id;

	@JsonProperty("gravatar_id")
	private String gravatarId;

	@JsonProperty("organizations_url")
	private String organizationsUrl;

    @ConstructorProperties({"gistsUrl", "reposUrl", "followingUrl", "starredUrl", "login", "followersUrl", "type", "url", "subscriptionsUrl", "receivedEventsUrl", "avatarUrl", "eventsUrl", "htmlUrl", "siteAdmin", "id", "gravatarId", "organizationsUrl"})
    public Creator(String gistsUrl, String reposUrl, String followingUrl, String starredUrl, String login, String followersUrl, String type, String url, String subscriptionsUrl, String receivedEventsUrl, String avatarUrl, String eventsUrl, String htmlUrl, boolean siteAdmin, int id, String gravatarId, String organizationsUrl) {
        this.gistsUrl = gistsUrl;
        this.reposUrl = reposUrl;
        this.followingUrl = followingUrl;
        this.starredUrl = starredUrl;
        this.login = login;
        this.followersUrl = followersUrl;
        this.type = type;
        this.url = url;
        this.subscriptionsUrl = subscriptionsUrl;
        this.receivedEventsUrl = receivedEventsUrl;
        this.avatarUrl = avatarUrl;
        this.eventsUrl = eventsUrl;
        this.htmlUrl = htmlUrl;
        this.siteAdmin = siteAdmin;
        this.id = id;
        this.gravatarId = gravatarId;
        this.organizationsUrl = organizationsUrl;
    }

    public Creator() {
    }

    public static CreatorBuilder builder() {
        return new CreatorBuilder();
    }

    public String getGistsUrl() {
        return this.gistsUrl;
    }

    public String getReposUrl() {
        return this.reposUrl;
    }

    public String getFollowingUrl() {
        return this.followingUrl;
    }

    public String getStarredUrl() {
        return this.starredUrl;
    }

    public String getLogin() {
        return this.login;
    }

    public String getFollowersUrl() {
        return this.followersUrl;
    }

    public String getType() {
        return this.type;
    }

    public String getUrl() {
        return this.url;
    }

    public String getSubscriptionsUrl() {
        return this.subscriptionsUrl;
    }

    public String getReceivedEventsUrl() {
        return this.receivedEventsUrl;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    public String getEventsUrl() {
        return this.eventsUrl;
    }

    public String getHtmlUrl() {
        return this.htmlUrl;
    }

    public boolean isSiteAdmin() {
        return this.siteAdmin;
    }

    public int getId() {
        return this.id;
    }

    public String getGravatarId() {
        return this.gravatarId;
    }

    public String getOrganizationsUrl() {
        return this.organizationsUrl;
    }

    public void setGistsUrl(String gistsUrl) {
        this.gistsUrl = gistsUrl;
    }

    public void setReposUrl(String reposUrl) {
        this.reposUrl = reposUrl;
    }

    public void setFollowingUrl(String followingUrl) {
        this.followingUrl = followingUrl;
    }

    public void setStarredUrl(String starredUrl) {
        this.starredUrl = starredUrl;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setFollowersUrl(String followersUrl) {
        this.followersUrl = followersUrl;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSubscriptionsUrl(String subscriptionsUrl) {
        this.subscriptionsUrl = subscriptionsUrl;
    }

    public void setReceivedEventsUrl(String receivedEventsUrl) {
        this.receivedEventsUrl = receivedEventsUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setSiteAdmin(boolean siteAdmin) {
        this.siteAdmin = siteAdmin;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setGravatarId(String gravatarId) {
        this.gravatarId = gravatarId;
    }

    public void setOrganizationsUrl(String organizationsUrl) {
        this.organizationsUrl = organizationsUrl;
    }


    public String toString() {
        return "Creator(gistsUrl=" + this.getGistsUrl() + ", reposUrl=" + this.getReposUrl() + ", followingUrl=" + this.getFollowingUrl() + ", starredUrl=" + this.getStarredUrl() + ", login=" + this.getLogin() + ", followersUrl=" + this.getFollowersUrl() + ", type=" + this.getType() + ", url=" + this.getUrl() + ", subscriptionsUrl=" + this.getSubscriptionsUrl() + ", receivedEventsUrl=" + this.getReceivedEventsUrl() + ", avatarUrl=" + this.getAvatarUrl() + ", eventsUrl=" + this.getEventsUrl() + ", htmlUrl=" + this.getHtmlUrl() + ", siteAdmin=" + this.isSiteAdmin() + ", id=" + this.getId() + ", gravatarId=" + this.getGravatarId() + ", organizationsUrl=" + this.getOrganizationsUrl() + ")";
    }

    public static class CreatorBuilder {
        private String gistsUrl;
        private String reposUrl;
        private String followingUrl;
        private String starredUrl;
        private String login;
        private String followersUrl;
        private String type;
        private String url;
        private String subscriptionsUrl;
        private String receivedEventsUrl;
        private String avatarUrl;
        private String eventsUrl;
        private String htmlUrl;
        private boolean siteAdmin;
        private int id;
        private String gravatarId;
        private String organizationsUrl;

        CreatorBuilder() {
        }

        public Creator.CreatorBuilder gistsUrl(String gistsUrl) {
            this.gistsUrl = gistsUrl;
            return this;
        }

        public Creator.CreatorBuilder reposUrl(String reposUrl) {
            this.reposUrl = reposUrl;
            return this;
        }

        public Creator.CreatorBuilder followingUrl(String followingUrl) {
            this.followingUrl = followingUrl;
            return this;
        }

        public Creator.CreatorBuilder starredUrl(String starredUrl) {
            this.starredUrl = starredUrl;
            return this;
        }

        public Creator.CreatorBuilder login(String login) {
            this.login = login;
            return this;
        }

        public Creator.CreatorBuilder followersUrl(String followersUrl) {
            this.followersUrl = followersUrl;
            return this;
        }

        public Creator.CreatorBuilder type(String type) {
            this.type = type;
            return this;
        }

        public Creator.CreatorBuilder url(String url) {
            this.url = url;
            return this;
        }

        public Creator.CreatorBuilder subscriptionsUrl(String subscriptionsUrl) {
            this.subscriptionsUrl = subscriptionsUrl;
            return this;
        }

        public Creator.CreatorBuilder receivedEventsUrl(String receivedEventsUrl) {
            this.receivedEventsUrl = receivedEventsUrl;
            return this;
        }

        public Creator.CreatorBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public Creator.CreatorBuilder eventsUrl(String eventsUrl) {
            this.eventsUrl = eventsUrl;
            return this;
        }

        public Creator.CreatorBuilder htmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
            return this;
        }

        public Creator.CreatorBuilder siteAdmin(boolean siteAdmin) {
            this.siteAdmin = siteAdmin;
            return this;
        }

        public Creator.CreatorBuilder id(int id) {
            this.id = id;
            return this;
        }

        public Creator.CreatorBuilder gravatarId(String gravatarId) {
            this.gravatarId = gravatarId;
            return this;
        }

        public Creator.CreatorBuilder organizationsUrl(String organizationsUrl) {
            this.organizationsUrl = organizationsUrl;
            return this;
        }

        public Creator build() {
            return new Creator(gistsUrl, reposUrl, followingUrl, starredUrl, login, followersUrl, type, url, subscriptionsUrl, receivedEventsUrl, avatarUrl, eventsUrl, htmlUrl, siteAdmin, id, gravatarId, organizationsUrl);
        }

        public String toString() {
            return "Creator.CreatorBuilder(gistsUrl=" + this.gistsUrl + ", reposUrl=" + this.reposUrl + ", followingUrl=" + this.followingUrl + ", starredUrl=" + this.starredUrl + ", login=" + this.login + ", followersUrl=" + this.followersUrl + ", type=" + this.type + ", url=" + this.url + ", subscriptionsUrl=" + this.subscriptionsUrl + ", receivedEventsUrl=" + this.receivedEventsUrl + ", avatarUrl=" + this.avatarUrl + ", eventsUrl=" + this.eventsUrl + ", htmlUrl=" + this.htmlUrl + ", siteAdmin=" + this.siteAdmin + ", id=" + this.id + ", gravatarId=" + this.gravatarId + ", organizationsUrl=" + this.organizationsUrl + ")";
        }
    }
}
