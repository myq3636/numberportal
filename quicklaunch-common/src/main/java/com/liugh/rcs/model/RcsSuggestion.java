package com.liugh.rcs.model;

/**
 * 建议回复/动作。按 type 使用不同字段子集。
 */
public class RcsSuggestion {

    private SuggestionType type = SuggestionType.REPLY;
    /** 按钮文案，<=25 */
    private String text;
    /** 回传数据，<=2048 */
    private String postbackData;

    /** OPEN_URL */
    private String url;
    /** FULL / TALL */
    private String webviewViewMode;

    /** DIAL */
    private String phoneNumber;

    /** SHOW_LOCATION */
    private Double latitude;
    private Double longitude;
    private String label;

    /** CALENDAR */
    private String startTime;
    private String endTime;
    private String calTitle;
    private String calDescription;

    public SuggestionType getType() {
        return type;
    }

    public void setType(SuggestionType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPostbackData() {
        return postbackData;
    }

    public void setPostbackData(String postbackData) {
        this.postbackData = postbackData;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWebviewViewMode() {
        return webviewViewMode;
    }

    public void setWebviewViewMode(String webviewViewMode) {
        this.webviewViewMode = webviewViewMode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getCalTitle() {
        return calTitle;
    }

    public void setCalTitle(String calTitle) {
        this.calTitle = calTitle;
    }

    public String getCalDescription() {
        return calDescription;
    }

    public void setCalDescription(String calDescription) {
        this.calDescription = calDescription;
    }
}
