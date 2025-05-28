package com.damors.zuji.model.response;

import com.damors.zuji.model.User;

public class LoginResponse extends BaseResponse<LoginResponse.Data> {
    public static class Data {
        private User user;
        private String token;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
