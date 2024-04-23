package com.dtme.marketplace.entities;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.springframework.beans.BeanUtils;

/**
 * @description
 * This is the base class from which all entities inherit. The type of
 * the `id` property is defined by the {@link EntityIdStrategy}.
 *
 * @docsCategory entities
 */
@MappedSuperclass
public abstract class VendureEntity {
	public VendureEntity() {
		//TODO nothing to do
	}
    protected VendureEntity(Map<String, Object> input) {
        if (input != null) {
            BeanUtils.copyProperties(input, this, input.keySet().toArray(new String[0]));
        }
    }

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}

