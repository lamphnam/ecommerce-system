package com.techlab.ecommerce.gateway.auth.entity;

import com.techlab.ecommerce.common.entity.BaseEntity;
import com.techlab.ecommerce.gateway.auth.enums.GatewayUserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gateway_users")
public class GatewayUser extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 120)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 40)
    private GatewayUserRole role;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
