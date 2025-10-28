package de.unknowncity.yabawrtp.configuration;

import de.unknowncity.astralib.libs.com.fasterxml.jackson.annotation.JsonProperty;

public class RtpSettings {
    @JsonProperty
    private final Radius radius = new Radius();
    @JsonProperty
    private final Origin origin = new Origin();

    public RtpSettings() {

    }

    public static class Radius {
        @JsonProperty
        private final int min = 0;
        @JsonProperty
        private final int max = 3000;

        public Radius() {

        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }
    }

    public static class Origin {
        @JsonProperty
        private final int x = 0;
        @JsonProperty
        private final int z = 0;

        public Origin() {

        }

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }
    }

    public Radius radius() {
        return radius;
    }

    public Origin origin() {
        return origin;
    }
}
