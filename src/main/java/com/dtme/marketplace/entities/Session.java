package com.dtme.marketplace.entities;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableInheritance;
import javax.persistence.TableInheritanceType;

import com.vendure.common.types.DeepPartial;
import com.vendure.common.types.ID;
import com.vendure.entity.base.BaseEntity;
import com.vendure.entity.channel.Channel;
import com.vendure.entity.customer.Customer;
import com.vendure.entity.order.Order;
import com.vendure.entity.user.User;

/**
 * @description
 * A Session is created when a user makes a request to restricted API operations. A Session can be an {@link AnonymousSession}
 * in the case of un-authenticated users, otherwise it is an {@link AuthenticatedSession}.
 *
 * @docsCategory entities
 */
@Entity
@Table(name = "session")
@TableInheritance(strategy = TableInheritanceType.SINGLE_TABLE)
public abstract class Session extends BaseEntity {
    @Index(unique = true)
    @Column
    private String token;

    @Column
    private Date expires;

    @Column
    private boolean invalidated;

    @Column(nullable = true)
    private ID activeOrderId;

    @Index
    @ManyToOne
    private Order activeOrder;

    @Column(nullable = true)
    private ID activeChannelId;

    @Index
    @ManyToOne
    private Channel activeChannel;
}

