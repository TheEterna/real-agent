package com.ai.agent.real.web.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_session")
public class AppSession implements Persistable<String> {
    
    public static class AppSessionBuilder {
        private boolean isNew = true;  // 确保Builder也设置isNew
    }
    @Id
    private String id; // appSessionId (UUID)

    private String roleId; // optional, can be null

    private LocalDateTime createdAt;

    @Transient
    private boolean isNew = true; // force insert on first save

    @Override
    public String getId() { return id; }

    @Override
    @Transient
    public boolean isNew() { return isNew; }
}
