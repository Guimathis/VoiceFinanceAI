package com.voicefinance.domain;

import com.voicefinance.domain.annotation.Confirmable;
import com.voicefinance.domain.annotation.FieldSpec;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ConfirmableFields {

    private final Map<Class<?>, List<FieldSpec>> cache = new ConcurrentHashMap<>();

    public List<FieldSpec> all(Class<?> type) {
        return cache.computeIfAbsent(type, this::scan);
    }

    public boolean isValid(Class<?> type, String field) {
        return all(type).stream().anyMatch(f -> f.name().equals(field));
    }

    public List<String> isValid(Class<?> type, List<String> fields) {
        return fields.stream().filter(f -> !isValid(type, f)).toList();
    }

    /** Labels dos campos obrigatórios ainda sem valor. */
    public List<String> missing(Object target) {
        var accessor = PropertyAccessorFactory.forDirectFieldAccess(target);
        return all(target.getClass()).stream()
                .filter(FieldSpec::required)
                .filter(f -> accessor.getPropertyValue(f.name()) == null)
                .map(FieldSpec::label)
                .toList();
    }

    private List<FieldSpec> scan(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Confirmable.class))
                .map(f -> {
                    var a = f.getAnnotation(Confirmable.class);
                    return new FieldSpec(f.getName(), a.label(), a.required());
                })
                .toList();
    }

}