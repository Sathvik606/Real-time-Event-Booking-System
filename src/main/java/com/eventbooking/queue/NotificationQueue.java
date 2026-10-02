package com.eventbooking.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationQueue {

    private static final BlockingQueue<NotificationTask> QUEUE =
            new LinkedBlockingQueue<>();

    public static void publish(NotificationTask task) {

        QUEUE.offer(task);

        System.out.println(
                "Notification added to queue for booking "
                        + task.getBookingId()
        );
    }

    public static NotificationTask consume()
            throws InterruptedException {

        return QUEUE.take();
    }
}