package com.techlab.ecommerce.notification.simulator;

import com.techlab.ecommerce.notification.entity.Notification;

public interface NotificationProviderSimulator {

    NotificationDeliveryResult send(Notification notification);
}
