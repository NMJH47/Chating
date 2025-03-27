package com.example.webchat;

import java.util.Objects;

public class User {

    public String id;
    public String nickname;

    public User(String id, String nickname) {
        super();
        this.id = id;
        this.nickname = nickname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return id.equals(user.getId());
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    public String getUid() {

        return id;
    }
}


