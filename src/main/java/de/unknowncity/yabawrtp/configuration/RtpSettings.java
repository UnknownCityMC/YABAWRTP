package de.unknowncity.yabawrtp.configuration;

import de.unknowncity.astralib.libs.com.fasterxml.jackson.annotation.JsonProperty;

public class RtpSettings {
    @JsonProperty
    private Radius radius = new Radius();
    @JsonProperty
    private Origin origin = new Origin();

    public RtpSettings() {

    }

    public class Radius {
        @JsonProperty
        private int min = 0;
        @JsonProperty
        private int max = 1000;

        public Radius() {

        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }
    }

    public class Origin {
        @JsonProperty
        private int x = 0;
        @JsonProperty
        private int z = 0;

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
