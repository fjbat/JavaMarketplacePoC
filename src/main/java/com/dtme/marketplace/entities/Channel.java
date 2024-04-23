package com.dtme.marketplace.entities;
import javax.persistence.*;
import java.util.List;

@Entity
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = true)
    private String description;

//    @ManyToOne
//    private Seller seller;
//
//    @Column(nullable = true)
//    private Long sellerId;

    @Column(nullable = false)
    private String defaultLanguageCode;

//    @ElementCollection
//    private List<String> availableLanguageCodes;
//
//    @ManyToOne
//    private Zone defaultTaxZone;
//
//    @ManyToOne
//    private Zone defaultShippingZone;

    @Column(nullable = false)
    private String defaultCurrencyCode;

//    @ElementCollection
//    private List<String> availableCurrencyCodes;

    @Column(nullable = false)
    private boolean trackInventory;

    @Column(nullable = false)
    private int outOfStockThreshold;

//    @OneToOne(mappedBy = "channel", cascade = CascadeType.ALL)
//    private CustomChannelFields customFields;

    @Column(nullable = false)
    private boolean pricesIncludeTax;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<Product> products;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<ProductVariant> productVariants;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<FacetValue> facetValues;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<Facet> facets;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<Collection> collections;
//
//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<Promotion> promotions;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<PaymentMethod> paymentMethods;
//
//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<ShippingMethod> shippingMethods;
//
//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<Customer> customers;

    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
    private List<Role> roles;

//    @ManyToMany(mappedBy = "channels", cascade = CascadeType.ALL)
//    private List<StockLocation> stockLocations;

    // Constructors, getters, and setters
}

