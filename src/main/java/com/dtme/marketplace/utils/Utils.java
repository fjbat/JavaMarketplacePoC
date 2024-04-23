package com.dtme.marketplace.utils;
import java.util.List;
import java.util.Map;

public class Utils {

}

public class DeepPartial<T> {
    private Map<String, Object> data;

    // Getter and setter for 'data'
}

public class DeepRequired<T> {
    private T data;

    // Getter and setter for 'data'
}

public class Type<T> {
    // You can define any required methods or fields here
}

public class AdminUiConfig {
    private String apiHost;
    private Object apiPort;
    private String adminApiPath;
    private String tokenMethod;
    private String authTokenHeaderKey;
    private String channelTokenKey;
    private LanguageCode defaultLanguage;
    private String defaultLocale;
    private List<LanguageCode> availableLanguages;
    private List<String> availableLocales;
    private String loginUrl;
    private String brand;
    private boolean hideVendureBranding;
    private boolean hideVersion;
    private String loginImageUrl;
    private List<String> cancellationReasons;

    // Getters and setters for all fields
}

public class AdminUiAppConfig {
    private String path;
    private String route;
    private Runnable compile;

    // Getters and setters for all fields
}

public class AdminUiAppDevModeConfig {
    private String sourcePath;
    private int port;
    private String route;
    private Runnable compile;

    // Getters and setters for all fields
}

public enum LanguageCode {
    EN, // Add other language codes as needed
}
