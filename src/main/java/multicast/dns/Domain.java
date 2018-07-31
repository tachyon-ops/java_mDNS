package multicast.dns;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Domain {
    private final String name;
    private final List<String> labels;

    public static final Domain LOCAL = new Domain("local.");

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("((.*)_(tcp|udp)\\.)?(.*?)\\.?");

    public static Domain fromName(String name) {
        Matcher matcher = DOMAIN_PATTERN.matcher(name);
        if (matcher.matches()) {
            return new Domain(matcher.group(4));
        } else {
            throw new IllegalArgumentException("Name does not match domain syntax");
        }
    }

    public Domain(String name) {
        this.name = name;
        labels = Arrays.asList(name.split("\\."));
    }

    public String getName() {
        return name;
    }

    public List<String> getLabels() {
        return labels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Domain domain = (Domain) o;

        return name.equals(domain.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Domain{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                '}';
    }
}
