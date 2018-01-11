package com.icthh.xm.ms.scheduler.manager;

/**
 *
 */ // TODO - to think if we really need this interface
public interface Expirable extends Runnable {

    boolean isExpired();

}
