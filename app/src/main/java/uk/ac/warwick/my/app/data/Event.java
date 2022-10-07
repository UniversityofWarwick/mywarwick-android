package uk.ac.warwick.my.app.data;

import androidx.annotation.NonNull;

import com.google.common.base.MoreObjects;

import java.util.Date;

public class Event {
    private Integer id;
    private String serverId;

    private String type;
    private String title;

    private Date start;
    private Date end;

    private String parentFullName;
    private String parentShortName;
    private String location;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getParentFullName() {
        return parentFullName;
    }

    public void setParentFullName(String parentFullName) {
        this.parentFullName = parentFullName;
    }

    public String getParentShortName() {
        return parentShortName;
    }

    public void setParentShortName(String parentShortName) {
        this.parentShortName = parentShortName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("serverId", serverId)
                .add("type", type)
                .add("title", title)
                .add("start", start)
                .add("end", end)
                .add("parentFullName", parentFullName)
                .add("parentShortName", parentShortName)
                .add("location", location)
                .toString();
    }
}
