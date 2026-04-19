package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnRouteResolution {
    private final KeepScreenOnRoute route;
    private final KeepScreenOnState state;

    public KeepScreenOnRouteResolution(KeepScreenOnRoute route, KeepScreenOnState state) {
        this.route = route;
        this.state = state;
    }

    public KeepScreenOnRoute getRoute() {
        return route;
    }

    public KeepScreenOnState getState() {
        return state;
    }
}
