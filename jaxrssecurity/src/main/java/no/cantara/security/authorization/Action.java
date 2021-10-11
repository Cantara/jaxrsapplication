package no.cantara.security.authorization;

import java.util.Objects;

public class Action {

    public static final Action ALL_ACTIONS_ON_ALL_SERVICES = new Action("*", "*");

    private final String service;
    private final String action;
    private final String serviceAndAction;

    public static Action from(String serviceAndAction) {
        Objects.requireNonNull(serviceAndAction);
        String[] parts = serviceAndAction.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("serviceAndAction must contain service and action separated by colon");
        }
        return new Action(parts[0], parts[1]);
    }

    public Action(String service, String action) {
        this.service = Objects.requireNonNull(service).trim();
        this.action = Objects.requireNonNull(action).trim();
        if (this.service.isEmpty()) {
            throw new IllegalArgumentException("service cannot be empty");
        }
        if (this.action.isEmpty()) {
            throw new IllegalArgumentException("action cannot be empty");
        }
        this.serviceAndAction = this.service + ":" + this.action;
    }

    public String getService() {
        return service;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return serviceAndAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Action action = (Action) o;

        return serviceAndAction.equals(action.serviceAndAction);
    }

    @Override
    public int hashCode() {
        return serviceAndAction.hashCode();
    }
}
