package com.lyricist.server.utils;

import com.lyricist.server.database.User;

public class PrivateUser {
    private String id;
    private String name;
    private String[] role;
    private String email;
    private String image;
    private String token;
    private String password;

    public PrivateUser(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.role = user.getRole();
        this.email = user.getEmail();
        this.image = user.getImage();
        this.token = user.getToken();
        this.password = user.getPassword();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getRole() {
        return role;
    }

    public void setRole(String[] role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
