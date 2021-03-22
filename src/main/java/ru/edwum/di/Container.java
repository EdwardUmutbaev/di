package ru.edwum.di;

import ru.edwum.di.exception.AmbiguousConstructorException;
import ru.edwum.di.exception.AmbiguousValueNameException;

import java.util.*;
import java.util.stream.Collectors;

public class Container {
    private final Map<String, Object> values = new HashMap<>();
    private final Map<Class<?>, Object> objects = new HashMap<>();
    private final Set<Class<?>> definitions = new HashSet<>();

    public void register(String name, Object value) {
        if (values.containsKey(name)) {
            throw new AmbiguousValueNameException(String.format("%s with value %s", name, value.toString()));
        }

        values.put(name, value);
    }

    public void register(Class<?>... definitions) {
        String badDefinitions = Arrays.stream(definitions)
                .filter(definition -> definition.getDeclaredConstructors().length != 1)
                .map(Class::getName)
                .collect(Collectors.joining(", "));

        if (!badDefinitions.isEmpty()) {
            throw new AmbiguousConstructorException(badDefinitions);
        }

        this.definitions.addAll(Arrays.asList(definitions));
    }




}
