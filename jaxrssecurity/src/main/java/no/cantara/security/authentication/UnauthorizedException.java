package no.cantara.security.authentication;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }
}
