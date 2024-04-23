package com.dtme.marketplace.helper.entries;



public class RelationMetadata {
    private String propertyName;
    private EntityMetadata inverseEntityMetadata;

    public String getPropertyName() {
        return propertyName;
    }

    public EntityMetadata getInverseEntityMetadata() {
        return inverseEntityMetadata;
    }
}
