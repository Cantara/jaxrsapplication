package no.cantara.security.authentication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CantaraApplicationAuthentication implements ApplicationAuthentication {

    private final String applicationId;
    private final String forwardingToken;
    private final Supplier<List<ApplicationTag>> tagsSupplier;
    private final AtomicReference<List<ApplicationTag>> tagsRef = new AtomicReference<>();
    private final String accessGroupTagName;

    public CantaraApplicationAuthentication(String applicationId, String forwardingToken, Supplier<List<ApplicationTag>> tagsSupplier, String accessGroupTagName) {
        this.applicationId = applicationId;
        this.forwardingToken = forwardingToken;
        this.tagsSupplier = tagsSupplier;
        this.accessGroupTagName = accessGroupTagName;
    }

    @Override
    public String ssoId() {
        return applicationId;
    }

    @Override
    public Instant expires() {
        return null;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CantaraApplicationAuthentication.class.getSimpleName() + "[", "]")
                .add("applicationId='" + applicationId + "'")
                .toString();
    }

    @Override
    public String forwardingToken() {
        return forwardingToken;
    }

    @Override
    public List<String> groups() {
        List<String> groups = new ArrayList<>();
        tags().stream()
                .filter(tag -> accessGroupTagName.equalsIgnoreCase(tag.getName()))
                .findFirst()
                .ifPresent(groupTag -> {
                    String[] parts = groupTag.getValue().split("[ ,:;]+");
                    for (String part : parts) {
                        groups.add(part.trim());
                    }
                });
        return groups;
    }

    @Override
    public List<ApplicationTag> tags() {
        List<ApplicationTag> tags = tagsRef.get();
        if (tags == null) {
            tags = tagsSupplier.get();
            tagsRef.set(tags);
        }
        return tags;
    }
}
