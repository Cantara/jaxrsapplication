package no.cantara.jaxrsapp.test;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinTestClient implements TestClient {

    private final AtomicInteger nextDelegate = new AtomicInteger();
    private final TestClient[] delegates;

    private RoundRobinTestClient(TestClient[] delegates) {
        this.delegates = delegates;
    }

    public static RoundRobinTestClient newClient(TestClient... delegates) {
        return new RoundRobinTestClient(delegates);
    }

    public static RoundRobinTestClient newClient(Collection<TestClient> delegates) {
        return new RoundRobinTestClient(delegates.toArray(new TestClient[0]));
    }

    private TestClient select() {
        int indexChoice = nextDelegate.get();
        while (!nextDelegate.compareAndSet(indexChoice, (indexChoice + 1) % delegates.length)) {
            indexChoice = nextDelegate.get();
        }
        return delegates[indexChoice];
    }

    @Override
    public TestClient useAuthorization(String authorization) {
        return select().useAuthorization(authorization);
    }

    @Override
    public FakeApplicationAuthorizationBuilder useFakeApplicationAuth() {
        return select().useFakeApplicationAuth();
    }

    public TestClient useFakeApplicationAuth(String applicationId) {
        return select().useFakeApplicationAuth(applicationId);
    }

    public FakeUserAuthorizationBuilder useFakeUserAuth() {
        return select().useFakeUserAuth();
    }

    public TestClient useHeader(String header, String value) {
        return select().useHeader(header, value);
    }

    public String getHost() {
        return select().getHost();
    }

    public int getPort() {
        return select().getPort();
    }

    public RequestBuilder get() {
        return select().get();
    }

    public RequestBuilder post() {
        return select().post();
    }

    public RequestBuilder put() {
        return select().put();
    }

    public RequestBuilder options() {
        return select().options();
    }

    public RequestBuilder head() {
        return select().head();
    }

    public RequestBuilder delete() {
        return select().delete();
    }

    public RequestBuilder patch() {
        return select().patch();
    }

    public RequestBuilder trace() {
        return select().trace();
    }
}
