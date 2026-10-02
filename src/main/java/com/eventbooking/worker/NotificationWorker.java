package com.eventbooking.worker;

import com.eventbooking.queue.NotificationQueue;
import com.eventbooking.queue.NotificationTask;

public class NotificationWorker implements Runnable {

    @Override
    public void run() {

        System.out.println(
                "Notification Worker started..."
        );

        while (true) {

            try {

                NotificationTask task =
                        NotificationQueue.consume();

                processNotification(task);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println(
                        "Notification Worker stopped."
                );

                break;
            }
        }
    }

    private void processNotification(
            NotificationTask task) {

        System.out.println(
                "Processing notification for booking "
                        + task.getBookingId()
        );

        System.out.println(
                "Message: " + task.getMessage()
        );

        // Simulate notification processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(
                "Notification sent for booking "
                        + task.getBookingId()
        );
    }
}