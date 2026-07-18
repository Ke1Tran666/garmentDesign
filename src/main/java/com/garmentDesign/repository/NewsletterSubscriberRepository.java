package com.garmentDesign.repository;

import com.garmentDesign.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterSubscriberRepository
        extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber>
    findByEmailAndDeletedAtIsNull(String email);

    Optional<NewsletterSubscriber>
    findByIdUserAndDeletedAtIsNull(String idUser);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}