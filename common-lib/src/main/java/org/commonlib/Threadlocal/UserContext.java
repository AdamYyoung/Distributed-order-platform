package org.commonlib.Threadlocal;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserContext {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    public static void set(Long userId){
        userIdHolder.set(userId);
    }

    public static Long get(){
        return userIdHolder.get();
    }

    public static void remove(){
        userIdHolder.remove();
    }
}
