package com.shopflow.common.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An application user who can log in to the desktop client.
 * The password is stored only as a salted hash on the backend;
 * the hash is never sent to the frontend.
 */
public class User implements Identifiable {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";

    private String id;
    private String username;
    private String passwordHash;
    private String role = ROLE_STAFF;
    private String displayName;

    public User() {
    }

    public User(String id, String username, String passwordHash, String role, String displayName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
