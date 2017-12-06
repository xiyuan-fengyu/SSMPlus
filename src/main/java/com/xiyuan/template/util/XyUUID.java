package com.xiyuan.template.util;

import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * Created by xiyuan_fengyu on 2017/12/6 15:14.
 */
public class XyUUID {

    private static final int hash;

    private static final SecureRandom random = new SecureRandom();

    private static long lastTimestamp = 0;

    static {
        int hostHasNum;
        try {
            hostHasNum = (InetAddress.getLocalHost().toString().hashCode() % 10 + 10) % 10 * 10 + (Thread.currentThread().hashCode() % 10 + 10) % 10;
        } catch (Exception e) {
            hostHasNum = (e.hashCode() % 10 + 10) % 10 * 10 + (Thread.currentThread().hashCode() % 10 + 10) % 10;
        }
        hash = hostHasNum;
    }

    public static synchronized long get() {
        long now = System.currentTimeMillis();
        if (now <= lastTimestamp) {
            lastTimestamp++;
            now = lastTimestamp;
        }
        else {
            lastTimestamp = now;
        }

        long rand = random.nextInt(10000);
        return (now * 100 + hash) * 10000 + rand;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(get());
        System.out.println(get());
        System.out.println(get());
    }

}
