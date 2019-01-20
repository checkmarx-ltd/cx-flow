package com.custodela.machina.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import java.beans.ConstructorProperties;

public class Note {

    @JsonProperty("id")
    public Integer id;
    @JsonProperty("body")
    public String body;
    @JsonProperty("attachment")
    public Object attachment;
    @JsonProperty("author")
    @Valid
    public Author author;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("updated_at")
    public String updatedAt;
    @JsonProperty("system")
    public Boolean system;
    @JsonProperty("noteable_id")
    public Integer noteableId;
    @JsonProperty("noteable_type")
    public String noteableType;
    @JsonProperty("noteable_iid")
    public Integer noteableIid;
    @JsonProperty("resolvable")
    public Boolean resolvable;

    @ConstructorProperties({"id", "body", "attachment", "author", "createdAt", "updatedAt", "system", "noteableId", "noteableType", "noteableIid", "resolvable"})
    Note(Integer id, String body, Object attachment, @Valid Author author, String createdAt, String updatedAt, Boolean system, Integer noteableId, String noteableType, Integer noteableIid, Boolean resolvable) {
        this.id = id;
        this.body = body;
        this.attachment = attachment;
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.system = system;
        this.noteableId = noteableId;
        this.noteableType = noteableType;
        this.noteableIid = noteableIid;
        this.resolvable = resolvable;
    }

    public static NoteBuilder builder() {
        return new NoteBuilder();
    }

    public Integer getId() {
        return this.id;
    }

    public String getBody() {
        return this.body;
    }

    public Object getAttachment() {
        return this.attachment;
    }

    public @Valid Author getAuthor() {
        return this.author;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public Boolean getSystem() {
        return this.system;
    }

    public Integer getNoteableId() {
        return this.noteableId;
    }

    public String getNoteableType() {
        return this.noteableType;
    }

    public Integer getNoteableIid() {
        return this.noteableIid;
    }

    public Boolean getResolvable() {
        return this.resolvable;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public void setAuthor(@Valid Author author) {
        this.author = author;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public void setNoteableId(Integer noteableId) {
        this.noteableId = noteableId;
    }

    public void setNoteableType(String noteableType) {
        this.noteableType = noteableType;
    }

    public void setNoteableIid(Integer noteableIid) {
        this.noteableIid = noteableIid;
    }

    public void setResolvable(Boolean resolvable) {
        this.resolvable = resolvable;
    }

    public static class NoteBuilder {
        private Integer id;
        private String body;
        private Object attachment;
        private @Valid Author author;
        private String createdAt;
        private String updatedAt;
        private Boolean system;
        private Integer noteableId;
        private String noteableType;
        private Integer noteableIid;
        private Boolean resolvable;

        NoteBuilder() {
        }

        public Note.NoteBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public Note.NoteBuilder body(String body) {
            this.body = body;
            return this;
        }

        public Note.NoteBuilder attachment(Object attachment) {
            this.attachment = attachment;
            return this;
        }

        public Note.NoteBuilder author(@Valid Author author) {
            this.author = author;
            return this;
        }

        public Note.NoteBuilder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Note.NoteBuilder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Note.NoteBuilder system(Boolean system) {
            this.system = system;
            return this;
        }

        public Note.NoteBuilder noteableId(Integer noteableId) {
            this.noteableId = noteableId;
            return this;
        }

        public Note.NoteBuilder noteableType(String noteableType) {
            this.noteableType = noteableType;
            return this;
        }

        public Note.NoteBuilder noteableIid(Integer noteableIid) {
            this.noteableIid = noteableIid;
            return this;
        }

        public Note.NoteBuilder resolvable(Boolean resolvable) {
            this.resolvable = resolvable;
            return this;
        }

        public Note build() {
            return new Note(id, body, attachment, author, createdAt, updatedAt, system, noteableId, noteableType, noteableIid, resolvable);
        }

        public String toString() {
            return "Note.NoteBuilder(id=" + this.id + ", body=" + this.body + ", attachment=" + this.attachment + ", author=" + this.author + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ", system=" + this.system + ", noteableId=" + this.noteableId + ", noteableType=" + this.noteableType + ", noteableIid=" + this.noteableIid + ", resolvable=" + this.resolvable + ")";
        }
    }
}