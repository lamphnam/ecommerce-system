package com.techlab.ecommerce.gateway.auth.repository;

import com.techlab.ecommerce.gateway.auth.entity.GatewayUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GatewayUserRepository extends JpaRepository<GatewayUser, Long> {

    Optional<GatewayUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
