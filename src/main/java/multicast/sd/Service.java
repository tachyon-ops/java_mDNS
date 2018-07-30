package multicast.sd;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Service {
    private final String name;
    private final List<String> labels;

    private static final Pattern SERVICE_PATTERN = Pattern.compile("^((_[a-zA-Z0-9-]+\\.)?_(tcp|udp))\\.?|$");

    public static Service fromName(String name) {
        Matcher matcher = SERVICE_PATTERN.matcher(name);
        if (matcher.find()) {
            return new Service(matcher.group(1));
        } else {
            throw new IllegalArgumentException("Name does not match service syntax");
        }
    }

    private Service(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("A Service's name can't be null or empty");
        }

        this.name = name;
        labels = Arrays.asList(name.split("\\."));
    }

    public String getName() {
        return name;
    }

    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Service service = (Service) o;

        return name.equals(service.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                '}';
    }
}
