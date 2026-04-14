package org.example.atuo_attend_backend.collab.dto;

public class CollabUserDto {
    private Long id;
    private String email;
    private String name;
    private String role;
    private String phoneE164;
    private Boolean phoneBound;
    private Long linkedTenantAdminUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public void setPhoneE164(String phoneE164) {
        this.phoneE164 = phoneE164;
    }

    public Boolean getPhoneBound() {
        return phoneBound;
    }

    public void setPhoneBound(Boolean phoneBound) {
        this.phoneBound = phoneBound;
    }

    public Long getLinkedTenantAdminUserId() {
        return linkedTenantAdminUserId;
    }

    public void setLinkedTenantAdminUserId(Long linkedTenantAdminUserId) {
        this.linkedTenantAdminUserId = linkedTenantAdminUserId;
    }
}
