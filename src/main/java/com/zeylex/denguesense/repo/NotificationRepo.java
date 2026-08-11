package com.zeylex.denguesense.repo;

import com.zeylex.denguesense.model.Notification;
import com.zeylex.denguesense.model.enums.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

    List<Notification> findByReferenceTypeAndReferenceId(ReferenceType type, Long referenceId);
}
