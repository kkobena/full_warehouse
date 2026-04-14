package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DashboardLayoutAuthorityId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "layout_id")
    private Integer layoutId;

    @Column(name = "authority_name", length = 50)
    private String authorityName;

    public DashboardLayoutAuthorityId() {}

    public DashboardLayoutAuthorityId(Integer layoutId, String authorityName) {
        this.layoutId = layoutId;
        this.authorityName = authorityName;
    }

    public Integer getLayoutId() { return layoutId; }
    public void setLayoutId(Integer layoutId) { this.layoutId = layoutId; }

    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String authorityName) { this.authorityName = authorityName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DashboardLayoutAuthorityId that)) return false;
        return Objects.equals(layoutId, that.layoutId) && Objects.equals(authorityName, that.authorityName);
    }

    @Override
    public int hashCode() { return Objects.hash(layoutId, authorityName); }
}
