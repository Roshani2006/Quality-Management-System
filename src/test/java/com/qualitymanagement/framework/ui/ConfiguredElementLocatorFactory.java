package com.qualitymanagement.framework.ui;

import com.qualitymanagement.framework.config.QmsConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

import java.lang.reflect.Field;
import java.util.List;

public class ConfiguredElementLocatorFactory implements ElementLocatorFactory {
    private final SearchContext searchContext;

    public ConfiguredElementLocatorFactory(SearchContext searchContext) {
        this.searchContext = searchContext;
    }

    @Override
    public ElementLocator createLocator(Field field) {
        ConfiguredBy configuredBy = field.getAnnotation(ConfiguredBy.class);
        if (configuredBy == null) {
            return null;
        }
        final By locator = By.cssSelector(QmsConfig.require(configuredBy.value()));
        return new ElementLocator() {
            @Override
            public WebElement findElement() {
                return searchContext.findElement(locator);
            }

            @Override
            public List<WebElement> findElements() {
                return searchContext.findElements(locator);
            }
        };
    }
}
