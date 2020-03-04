package com.checkmarx.flow.dto.rally;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "_rallyAPIMajor",
        "_rallyAPIMinor",
        "_ref",
        "_refObjectUUID",
        "_objectVersion",
        "_refObjectName",
        "CreationDate",
        "_CreatedAt",
        "ObjectID",
        "ObjectUUID",
        "VersionId",
        "Subscription",
        "Workspace",
        "Changesets",
        "Connections",
        "CreatedBy",
        "Description",
        "Discussion",
        "DisplayColor",
        "Expedite",
        "FormattedID",
        "LastUpdateDate",
        "LatestDiscussionAgeInMinutes",
        "Milestones",
        "Name",
        "Notes",
        "Owner",
        "Project",
        "Ready",
        "RevisionHistory",
        "Tags",
        "FlowState",
        "FlowStateChangedDate",
        "LastBuild",
        "LastRun",
        "PassingTestCaseCount",
        "ScheduleState",
        "ScheduleStatePrefix",
        "TestCaseCount",
        "AcceptedDate",
        "AffectsDoc",
        "Attachments",
        "Blocked",
        "BlockedReason",
        "Blocker",
        "ClosedDate",
        "DefectSuites",
        "DragAndDropRank",
        "Duplicates",
        "Environment",
        "FixedInBuild",
        "FoundInBuild",
        "InProgressDate",
        "Iteration",
        "OpenedDate",
        "Package",
        "PlanEstimate",
        "Priority",
        "Recycled",
        "Release",
        "ReleaseNote",
        "Requirement",
        "Resolution",
        "SalesforceCaseID",
        "SalesforceCaseNumber",
        "Severity",
        "State",
        "SubmittedBy",
        "TargetBuild",
        "TargetDate",
        "TaskActualTotal",
        "TaskEstimateTotal",
        "TaskRemainingTotal",
        "TaskStatus",
        "Tasks",
        "TestCase",
        "TestCaseResult",
        "TestCaseStatus",
        "TestCases",
        "VerifiedInBuild",
        "_type"
})
public class Result {

    @JsonProperty("_rallyAPIMajor")
    private String rallyAPIMajor;
    @JsonProperty("_rallyAPIMinor")
    private String rallyAPIMinor;
    @JsonProperty("_ref")
    private String ref;
    @JsonProperty("_refObjectUUID")
    private String refObjectUUID;
    @JsonProperty("_objectVersion")
    private String objectVersion;
    @JsonProperty("_refObjectName")
    private String refObjectName;
    @JsonProperty("CreationDate")
    private String creationDate;
    @JsonProperty("_CreatedAt")
    private String createdAt;
    @JsonProperty("ObjectID")
    private Long objectID;
    @JsonProperty("ObjectUUID")
    private String objectUUID;
    @JsonProperty("VersionId")
    private String versionId;
    @JsonProperty("Subscription")
    private Subscription subscription;
    @JsonProperty("Workspace")
    private Workspace workspace;
    @JsonProperty("Changesets")
    private Changesets changesets;
    @JsonProperty("Connections")
    private Connections connections;
    @JsonProperty("CreatedBy")
    private CreatedBy createdBy;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("Discussion")
    private Discussion discussion;
    @JsonProperty("DisplayColor")
    private String displayColor;
    @JsonProperty("Expedite")
    private Boolean expedite;
    @JsonProperty("FormattedID")
    private String formattedID;
    @JsonProperty("LastUpdateDate")
    private String lastUpdateDate;
    @JsonProperty("LatestDiscussionAgeInMinutes")
    private String latestDiscussionAgeInMinutes;
    @JsonProperty("Milestones")
    private Milestones milestones;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Notes")
    private String notes;
    @JsonProperty("Owner")
    private Object owner;
    @JsonProperty("Project")
    private Project project;
    @JsonProperty("Ready")
    private Boolean ready;
    @JsonProperty("RevisionHistory")
    private RevisionHistory revisionHistory;
    @JsonProperty("Tags")
    private Tags tags;
    @JsonProperty("FlowState")
    private FlowState flowState;
    @JsonProperty("FlowStateChangedDate")
    private String flowStateChangedDate;
    @JsonProperty("LastBuild")
    private Object lastBuild;
    @JsonProperty("LastRun")
    private Object lastRun;
    @JsonProperty("PassingTestCaseCount")
    private Long passingTestCaseCount;
    @JsonProperty("ScheduleState")
    private String scheduleState;
    @JsonProperty("ScheduleStatePrefix")
    private String scheduleStatePrefix;
    @JsonProperty("TestCaseCount")
    private Long testCaseCount;
    @JsonProperty("AcceptedDate")
    private Object acceptedDate;
    @JsonProperty("AffectsDoc")
    private Boolean affectsDoc;
    @JsonProperty("Attachments")
    private Attachments attachments;
    @JsonProperty("Blocked")
    private Boolean blocked;
    @JsonProperty("BlockedReason")
    private Object blockedReason;
    @JsonProperty("Blocker")
    private Object blocker;
    @JsonProperty("ClosedDate")
    private Object closedDate;
    @JsonProperty("DefectSuites")
    private DefectSuites defectSuites;
    @JsonProperty("DragAndDropRank")
    private String dragAndDropRank;
    @JsonProperty("Duplicates")
    private Duplicates duplicates;
    @JsonProperty("Environment")
    private String environment;
    @JsonProperty("FixedInBuild")
    private Object fixedInBuild;
    @JsonProperty("FoundInBuild")
    private Object foundInBuild;
    @JsonProperty("InProgressDate")
    private Object inProgressDate;
    @JsonProperty("Iteration")
    private Object iteration;
    @JsonProperty("OpenedDate")
    private String openedDate;
    @JsonProperty("Package")
    private Object _package;
    @JsonProperty("PlanEstimate")
    private Object planEstimate;
    @JsonProperty("Priority")
    private String priority;
    @JsonProperty("Recycled")
    private Boolean recycled;
    @JsonProperty("Release")
    private Object release;
    @JsonProperty("ReleaseNote")
    private Boolean releaseNote;
    @JsonProperty("Requirement")
    private Object requirement;
    @JsonProperty("Resolution")
    private String resolution;
    @JsonProperty("SalesforceCaseID")
    private Object salesforceCaseID;
    @JsonProperty("SalesforceCaseNumber")
    private Object salesforceCaseNumber;
    @JsonProperty("Severity")
    private String severity;
    @JsonProperty("State")
    private String state;
    @JsonProperty("SubmittedBy")
    private SubmittedBy submittedBy;
    @JsonProperty("TargetBuild")
    private Object targetBuild;
    @JsonProperty("TargetDate")
    private Object targetDate;
    @JsonProperty("TaskActualTotal")
    private Double taskActualTotal;
    @JsonProperty("TaskEstimateTotal")
    private Double taskEstimateTotal;
    @JsonProperty("TaskRemainingTotal")
    private Double taskRemainingTotal;
    @JsonProperty("TaskStatus")
    private String taskStatus;
    @JsonProperty("Tasks")
    private Tasks tasks;
    @JsonProperty("TestCase")
    private Object testCase;
    @JsonProperty("TestCaseResult")
    private Object testCaseResult;
    @JsonProperty("TestCaseStatus")
    private String testCaseStatus;
    @JsonProperty("TestCases")
    private TestCases testCases;
    @JsonProperty("VerifiedInBuild")
    private Object verifiedInBuild;
    @JsonProperty("_type")
    private String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("_rallyAPIMajor")
    public String getRallyAPIMajor() {
        return rallyAPIMajor;
    }

    @JsonProperty("_rallyAPIMajor")
    public void setRallyAPIMajor(String rallyAPIMajor) {
        this.rallyAPIMajor = rallyAPIMajor;
    }

    @JsonProperty("_rallyAPIMinor")
    public String getRallyAPIMinor() {
        return rallyAPIMinor;
    }

    @JsonProperty("_rallyAPIMinor")
    public void setRallyAPIMinor(String rallyAPIMinor) {
        this.rallyAPIMinor = rallyAPIMinor;
    }

    @JsonProperty("_ref")
    public String getRef() {
        return ref;
    }

    @JsonProperty("_ref")
    public void setRef(String ref) {
        this.ref = ref;
    }

    @JsonProperty("_refObjectUUID")
    public String getRefObjectUUID() {
        return refObjectUUID;
    }

    @JsonProperty("_refObjectUUID")
    public void setRefObjectUUID(String refObjectUUID) {
        this.refObjectUUID = refObjectUUID;
    }

    @JsonProperty("_objectVersion")
    public String getObjectVersion() {
        return objectVersion;
    }

    @JsonProperty("_objectVersion")
    public void setObjectVersion(String objectVersion) {
        this.objectVersion = objectVersion;
    }

    @JsonProperty("_refObjectName")
    public String getRefObjectName() {
        return refObjectName;
    }

    @JsonProperty("_refObjectName")
    public void setRefObjectName(String refObjectName) {
        this.refObjectName = refObjectName;
    }

    @JsonProperty("CreationDate")
    public String getCreationDate() {
        return creationDate;
    }

    @JsonProperty("CreationDate")
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("_CreatedAt")
    public String getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("_CreatedAt")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("ObjectID")
    public Long getObjectID() {
        return objectID;
    }

    @JsonProperty("ObjectID")
    public void setObjectID(Long objectID) {
        this.objectID = objectID;
    }

    @JsonProperty("ObjectUUID")
    public String getObjectUUID() {
        return objectUUID;
    }

    @JsonProperty("ObjectUUID")
    public void setObjectUUID(String objectUUID) {
        this.objectUUID = objectUUID;
    }

    @JsonProperty("VersionId")
    public String getVersionId() {
        return versionId;
    }

    @JsonProperty("VersionId")
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    @JsonProperty("Subscription")
    public Subscription getSubscription() {
        return subscription;
    }

    @JsonProperty("Subscription")
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @JsonProperty("Workspace")
    public Workspace getWorkspace() {
        return workspace;
    }

    @JsonProperty("Workspace")
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @JsonProperty("Changesets")
    public Changesets getChangesets() {
        return changesets;
    }

    @JsonProperty("Changesets")
    public void setChangesets(Changesets changesets) {
        this.changesets = changesets;
    }

    @JsonProperty("Connections")
    public Connections getConnections() {
        return connections;
    }

    @JsonProperty("Connections")
    public void setConnections(Connections connections) {
        this.connections = connections;
    }

    @JsonProperty("CreatedBy")
    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    @JsonProperty("CreatedBy")
    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("Description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("Description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("Discussion")
    public Discussion getDiscussion() {
        return discussion;
    }

    @JsonProperty("Discussion")
    public void setDiscussion(Discussion discussion) {
        this.discussion = discussion;
    }

    @JsonProperty("DisplayColor")
    public String getDisplayColor() {
        return displayColor;
    }

    @JsonProperty("DisplayColor")
    public void setDisplayColor(String displayColor) {
        this.displayColor = displayColor;
    }

    @JsonProperty("Expedite")
    public Boolean getExpedite() {
        return expedite;
    }

    @JsonProperty("Expedite")
    public void setExpedite(Boolean expedite) {
        this.expedite = expedite;
    }

    @JsonProperty("FormattedID")
    public String getFormattedID() {
        return formattedID;
    }

    @JsonProperty("FormattedID")
    public void setFormattedID(String formattedID) {
        this.formattedID = formattedID;
    }

    @JsonProperty("LastUpdateDate")
    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    @JsonProperty("LastUpdateDate")
    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    @JsonProperty("LatestDiscussionAgeInMinutes")
    public String getLatestDiscussionAgeInMinutes() {
        return latestDiscussionAgeInMinutes;
    }

    @JsonProperty("LatestDiscussionAgeInMinutes")
    public void setLatestDiscussionAgeInMinutes(String latestDiscussionAgeInMinutes) {
        this.latestDiscussionAgeInMinutes = latestDiscussionAgeInMinutes;
    }

    @JsonProperty("Milestones")
    public Milestones getMilestones() {
        return milestones;
    }

    @JsonProperty("Milestones")
    public void setMilestones(Milestones milestones) {
        this.milestones = milestones;
    }

    @JsonProperty("Name")
    public String getName() {
        return name;
    }

    @JsonProperty("Name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("Notes")
    public String getNotes() {
        return notes;
    }

    @JsonProperty("Notes")
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @JsonProperty("Owner")
    public Object getOwner() {
        return owner;
    }

    @JsonProperty("Owner")
    public void setOwner(Object owner) {
        this.owner = owner;
    }

    @JsonProperty("Project")
    public Project getProject() {
        return project;
    }

    @JsonProperty("Project")
    public void setProject(Project project) {
        this.project = project;
    }

    @JsonProperty("Ready")
    public Boolean getReady() {
        return ready;
    }

    @JsonProperty("Ready")
    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    @JsonProperty("RevisionHistory")
    public RevisionHistory getRevisionHistory() {
        return revisionHistory;
    }

    @JsonProperty("RevisionHistory")
    public void setRevisionHistory(RevisionHistory revisionHistory) {
        this.revisionHistory = revisionHistory;
    }

    @JsonProperty("Tags")
    public Tags getTags() {
        return tags;
    }

    @JsonProperty("Tags")
    public void setTags(Tags tags) {
        this.tags = tags;
    }

    @JsonProperty("FlowState")
    public FlowState getFlowState() {
        return flowState;
    }

    @JsonProperty("FlowState")
    public void setFlowState(FlowState flowState) {
        this.flowState = flowState;
    }

    @JsonProperty("FlowStateChangedDate")
    public String getFlowStateChangedDate() {
        return flowStateChangedDate;
    }

    @JsonProperty("FlowStateChangedDate")
    public void setFlowStateChangedDate(String flowStateChangedDate) {
        this.flowStateChangedDate = flowStateChangedDate;
    }

    @JsonProperty("LastBuild")
    public Object getLastBuild() {
        return lastBuild;
    }

    @JsonProperty("LastBuild")
    public void setLastBuild(Object lastBuild) {
        this.lastBuild = lastBuild;
    }

    @JsonProperty("LastRun")
    public Object getLastRun() {
        return lastRun;
    }

    @JsonProperty("LastRun")
    public void setLastRun(Object lastRun) {
        this.lastRun = lastRun;
    }

    @JsonProperty("PassingTestCaseCount")
    public Long getPassingTestCaseCount() {
        return passingTestCaseCount;
    }

    @JsonProperty("PassingTestCaseCount")
    public void setPassingTestCaseCount(Long passingTestCaseCount) {
        this.passingTestCaseCount = passingTestCaseCount;
    }

    @JsonProperty("ScheduleState")
    public String getScheduleState() {
        return scheduleState;
    }

    @JsonProperty("ScheduleState")
    public void setScheduleState(String scheduleState) {
        this.scheduleState = scheduleState;
    }

    @JsonProperty("ScheduleStatePrefix")
    public String getScheduleStatePrefix() {
        return scheduleStatePrefix;
    }

    @JsonProperty("ScheduleStatePrefix")
    public void setScheduleStatePrefix(String scheduleStatePrefix) {
        this.scheduleStatePrefix = scheduleStatePrefix;
    }

    @JsonProperty("TestCaseCount")
    public Long getTestCaseCount() {
        return testCaseCount;
    }

    @JsonProperty("TestCaseCount")
    public void setTestCaseCount(Long testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    @JsonProperty("AcceptedDate")
    public Object getAcceptedDate() {
        return acceptedDate;
    }

    @JsonProperty("AcceptedDate")
    public void setAcceptedDate(Object acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    @JsonProperty("AffectsDoc")
    public Boolean getAffectsDoc() {
        return affectsDoc;
    }

    @JsonProperty("AffectsDoc")
    public void setAffectsDoc(Boolean affectsDoc) {
        this.affectsDoc = affectsDoc;
    }

    @JsonProperty("Attachments")
    public Attachments getAttachments() {
        return attachments;
    }

    @JsonProperty("Attachments")
    public void setAttachments(Attachments attachments) {
        this.attachments = attachments;
    }

    @JsonProperty("Blocked")
    public Boolean getBlocked() {
        return blocked;
    }

    @JsonProperty("Blocked")
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    @JsonProperty("BlockedReason")
    public Object getBlockedReason() {
        return blockedReason;
    }

    @JsonProperty("BlockedReason")
    public void setBlockedReason(Object blockedReason) {
        this.blockedReason = blockedReason;
    }

    @JsonProperty("Blocker")
    public Object getBlocker() {
        return blocker;
    }

    @JsonProperty("Blocker")
    public void setBlocker(Object blocker) {
        this.blocker = blocker;
    }

    @JsonProperty("ClosedDate")
    public Object getClosedDate() {
        return closedDate;
    }

    @JsonProperty("ClosedDate")
    public void setClosedDate(Object closedDate) {
        this.closedDate = closedDate;
    }

    @JsonProperty("DefectSuites")
    public DefectSuites getDefectSuites() {
        return defectSuites;
    }

    @JsonProperty("DefectSuites")
    public void setDefectSuites(DefectSuites defectSuites) {
        this.defectSuites = defectSuites;
    }

    @JsonProperty("DragAndDropRank")
    public String getDragAndDropRank() {
        return dragAndDropRank;
    }

    @JsonProperty("DragAndDropRank")
    public void setDragAndDropRank(String dragAndDropRank) {
        this.dragAndDropRank = dragAndDropRank;
    }

    @JsonProperty("Duplicates")
    public Duplicates getDuplicates() {
        return duplicates;
    }

    @JsonProperty("Duplicates")
    public void setDuplicates(Duplicates duplicates) {
        this.duplicates = duplicates;
    }

    @JsonProperty("Environment")
    public String getEnvironment() {
        return environment;
    }

    @JsonProperty("Environment")
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @JsonProperty("FixedInBuild")
    public Object getFixedInBuild() {
        return fixedInBuild;
    }

    @JsonProperty("FixedInBuild")
    public void setFixedInBuild(Object fixedInBuild) {
        this.fixedInBuild = fixedInBuild;
    }

    @JsonProperty("FoundInBuild")
    public Object getFoundInBuild() {
        return foundInBuild;
    }

    @JsonProperty("FoundInBuild")
    public void setFoundInBuild(Object foundInBuild) {
        this.foundInBuild = foundInBuild;
    }

    @JsonProperty("InProgressDate")
    public Object getInProgressDate() {
        return inProgressDate;
    }

    @JsonProperty("InProgressDate")
    public void setInProgressDate(Object inProgressDate) {
        this.inProgressDate = inProgressDate;
    }

    @JsonProperty("Iteration")
    public Object getIteration() {
        return iteration;
    }

    @JsonProperty("Iteration")
    public void setIteration(Object iteration) {
        this.iteration = iteration;
    }

    @JsonProperty("OpenedDate")
    public String getOpenedDate() {
        return openedDate;
    }

    @JsonProperty("OpenedDate")
    public void setOpenedDate(String openedDate) {
        this.openedDate = openedDate;
    }

    @JsonProperty("Package")
    public Object getPackage() {
        return _package;
    }

    @JsonProperty("Package")
    public void setPackage(Object _package) {
        this._package = _package;
    }

    @JsonProperty("PlanEstimate")
    public Object getPlanEstimate() {
        return planEstimate;
    }

    @JsonProperty("PlanEstimate")
    public void setPlanEstimate(Object planEstimate) {
        this.planEstimate = planEstimate;
    }

    @JsonProperty("Priority")
    public String getPriority() {
        return priority;
    }

    @JsonProperty("Priority")
    public void setPriority(String priority) {
        this.priority = priority;
    }

    @JsonProperty("Recycled")
    public Boolean getRecycled() {
        return recycled;
    }

    @JsonProperty("Recycled")
    public void setRecycled(Boolean recycled) {
        this.recycled = recycled;
    }

    @JsonProperty("Release")
    public Object getRelease() {
        return release;
    }

    @JsonProperty("Release")
    public void setRelease(Object release) {
        this.release = release;
    }

    @JsonProperty("ReleaseNote")
    public Boolean getReleaseNote() {
        return releaseNote;
    }

    @JsonProperty("ReleaseNote")
    public void setReleaseNote(Boolean releaseNote) {
        this.releaseNote = releaseNote;
    }

    @JsonProperty("Requirement")
    public Object getRequirement() {
        return requirement;
    }

    @JsonProperty("Requirement")
    public void setRequirement(Object requirement) {
        this.requirement = requirement;
    }

    @JsonProperty("Resolution")
    public String getResolution() {
        return resolution;
    }

    @JsonProperty("Resolution")
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @JsonProperty("SalesforceCaseID")
    public Object getSalesforceCaseID() {
        return salesforceCaseID;
    }

    @JsonProperty("SalesforceCaseID")
    public void setSalesforceCaseID(Object salesforceCaseID) {
        this.salesforceCaseID = salesforceCaseID;
    }

    @JsonProperty("SalesforceCaseNumber")
    public Object getSalesforceCaseNumber() {
        return salesforceCaseNumber;
    }

    @JsonProperty("SalesforceCaseNumber")
    public void setSalesforceCaseNumber(Object salesforceCaseNumber) {
        this.salesforceCaseNumber = salesforceCaseNumber;
    }

    @JsonProperty("Severity")
    public String getSeverity() {
        return severity;
    }

    @JsonProperty("Severity")
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    @JsonProperty("State")
    public String getState() {
        return state;
    }

    @JsonProperty("State")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("SubmittedBy")
    public SubmittedBy getSubmittedBy() {
        return submittedBy;
    }

    @JsonProperty("SubmittedBy")
    public void setSubmittedBy(SubmittedBy submittedBy) {
        this.submittedBy = submittedBy;
    }

    @JsonProperty("TargetBuild")
    public Object getTargetBuild() {
        return targetBuild;
    }

    @JsonProperty("TargetBuild")
    public void setTargetBuild(Object targetBuild) {
        this.targetBuild = targetBuild;
    }

    @JsonProperty("TargetDate")
    public Object getTargetDate() {
        return targetDate;
    }

    @JsonProperty("TargetDate")
    public void setTargetDate(Object targetDate) {
        this.targetDate = targetDate;
    }

    @JsonProperty("TaskActualTotal")
    public Double getTaskActualTotal() {
        return taskActualTotal;
    }

    @JsonProperty("TaskActualTotal")
    public void setTaskActualTotal(Double taskActualTotal) {
        this.taskActualTotal = taskActualTotal;
    }

    @JsonProperty("TaskEstimateTotal")
    public Double getTaskEstimateTotal() {
        return taskEstimateTotal;
    }

    @JsonProperty("TaskEstimateTotal")
    public void setTaskEstimateTotal(Double taskEstimateTotal) {
        this.taskEstimateTotal = taskEstimateTotal;
    }

    @JsonProperty("TaskRemainingTotal")
    public Double getTaskRemainingTotal() {
        return taskRemainingTotal;
    }

    @JsonProperty("TaskRemainingTotal")
    public void setTaskRemainingTotal(Double taskRemainingTotal) {
        this.taskRemainingTotal = taskRemainingTotal;
    }

    @JsonProperty("TaskStatus")
    public String getTaskStatus() {
        return taskStatus;
    }

    @JsonProperty("TaskStatus")
    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    @JsonProperty("Tasks")
    public Tasks getTasks() {
        return tasks;
    }

    @JsonProperty("Tasks")
    public void setTasks(Tasks tasks) {
        this.tasks = tasks;
    }

    @JsonProperty("TestCase")
    public Object getTestCase() {
        return testCase;
    }

    @JsonProperty("TestCase")
    public void setTestCase(Object testCase) {
        this.testCase = testCase;
    }

    @JsonProperty("TestCaseResult")
    public Object getTestCaseResult() {
        return testCaseResult;
    }

    @JsonProperty("TestCaseResult")
    public void setTestCaseResult(Object testCaseResult) {
        this.testCaseResult = testCaseResult;
    }

    @JsonProperty("TestCaseStatus")
    public String getTestCaseStatus() {
        return testCaseStatus;
    }

    @JsonProperty("TestCaseStatus")
    public void setTestCaseStatus(String testCaseStatus) {
        this.testCaseStatus = testCaseStatus;
    }

    @JsonProperty("TestCases")
    public TestCases getTestCases() {
        return testCases;
    }

    @JsonProperty("TestCases")
    public void setTestCases(TestCases testCases) {
        this.testCases = testCases;
    }

    @JsonProperty("VerifiedInBuild")
    public Object getVerifiedInBuild() {
        return verifiedInBuild;
    }

    @JsonProperty("VerifiedInBuild")
    public void setVerifiedInBuild(Object verifiedInBuild) {
        this.verifiedInBuild = verifiedInBuild;
    }

    @JsonProperty("_type")
    public String getType() {
        return type;
    }

    @JsonProperty("_type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
