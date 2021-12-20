package com.lyricist.server.utils;

import com.lyricist.server.database.User;

import java.time.Instant;

public class UserSessionModel {
    public User user;
    public int otp;
    public long time;
    public String pin;
    public int tries = 0;

    public UserSessionModel(User user, int otp) {
        this.user = user;
        this.otp = otp;
        this.time = Instant.now().toEpochMilli();
    }

    public UserSessionModel(User user, String pin) {
        this.user = user;
        this.pin = pin;
        this.time = Instant.now().toEpochMilli();
    }

    public void addTry() {
        this.tries = this.tries + 1;
    }
}
