package com.lyricist.server.utils;

import com.lyricist.server.database.User;

import java.time.Instant;

public class UserSessionModel {
    public User user;
    public int otp;
    public long time;

    public UserSessionModel(User user, int otp) {
        this.user = user;
        this.otp = otp;
        this.time = Instant.now().toEpochMilli();
    }
}
