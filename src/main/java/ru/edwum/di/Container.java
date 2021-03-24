package ru.edwum.di;

import ru.edwum.di.annotation.Inject;
import ru.edwum.di.exception.AmbiguousConstructorException;
import ru.edwum.di.exception.AmbiguousValueNameException;
import ru.edwum.di.exception.ObjectInstantiationException;
import ru.edwum.di.exception.UnmetDependenciesException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    public void wire() {
        HashSet<Class<?>> todo = new HashSet<>(definitions);

        while (!todo.isEmpty()) {
            int initialSize = todo.size();

            final var createdObj = todo.stream() // lazy
                    .map(def -> def.getDeclaredConstructors()[0])
                    .filter(constructor -> constructor.getParameterCount() == 0 || allParameterInValues(constructor))
                    .map(this::instantiate)
                    .collect(Collectors.toMap(
                            Object::getClass,
                            o -> o)
                    );
            objects.putAll(createdObj);

            createdObj.entrySet().stream()
                    .map(o -> {
                        final var interfaces = o.getKey().getInterfaces();
                        final var value = o.getValue();
                        final var ifaces = new HashMap<Class<?>, Object>();
                        for (Class<?> cls : interfaces) {
                            ifaces.put(cls, value);
                        }
                        return ifaces;
                    }).forEach(objects::putAll);

            todo.removeAll(createdObj.keySet());

            int totalSize = todo.size();

            if (createdObj.size() == 0 /*|| initialSize == totalSize*/) { //тут ли проверка на бесконечный?
                // sad path
                String unmet = todo.stream()
                        .map(Class::getName)
                        .collect(Collectors.joining(", "));
                throw new UnmetDependenciesException(unmet);
            }
        }
    }

    private Object instantiate(Constructor<?> constructor) {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(Arrays.stream(constructor.getParameters())
                    .map(parameter -> Optional.ofNullable(objects.get(parameter.getType()))
                            .or(() -> Optional.ofNullable(values.get(
                                    // TODO: NPE
                                    parameter.getAnnotation(Inject.class).value() // arg0
                            )))
                            .orElseThrow(() -> new UnmetDependenciesException(parameter.getName()))
                    ).toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new ObjectInstantiationException(e); // <- e //
        }
    }

    private boolean allParameterInValues(Constructor<?> constructor) {
        final var parameters = new HashSet<>(Arrays.asList(constructor.getParameters()));
        parameters.removeIf(p -> objects.containsKey(p.getType()));
        // TODO: check parameter annotation -> throw exception
        // Service
        parameters.removeAll(
                parameters.stream()
                        .filter(p -> p.isAnnotationPresent(Inject.class))
                        .filter(p -> values.containsKey(p.getAnnotation(Inject.class).value()))
                        .collect(Collectors.toList())
                // "smsUrl", "pushUrl"
        );
        return parameters.isEmpty();
    }
}
