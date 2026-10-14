package com.shopflow.common.model;

import java.util.LinkedHashMap;
import java.util.Map;


    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    /** Full representation, including the hash, for backend persistence only. */
    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> map = toPublicJson();
        map.put("passwordHash", passwordHash);
        return map;
    }

    /** Representation that is safe to send to clients. */
    public Map<String, Object> toPublicJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("role", role);
        map.put("displayName", displayName);
        return map;
    }

    public static User fromJson(Map<String, Object> map) {
        User user = new User();
        user.id = Identifiable.idOrNull(Identifiable.str(map, "id"));
        user.username = Identifiable.str(map, "username");
        user.passwordHash = Identifiable.str(map, "passwordHash");
        String rawRole = Identifiable.str(map, "role");
        user.role = rawRole.isEmpty() ? ROLE_STAFF : rawRole;
        user.displayName = Identifiable.str(map, "displayName");
        return user;
    }

    @Override
    public String toString() {
        return username;
    }
}
