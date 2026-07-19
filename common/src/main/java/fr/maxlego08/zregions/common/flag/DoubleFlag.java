package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * A numeric flag ({@code double}), optionally bounded. Parsing rejects
 * NaN/Infinite and clamps to {@code [min, max]}; serialization uses a short,
 * locale-independent form so the value round-trips exactly through storage.
 *
 * <p>Its {@code defaultValue} is only a fallback for {@link #resolveFlag}; the
 * player-state flags that use it apply solely on an explicit value, resolved via
 * {@code resolveFlagIfSet}.</p>
 */
public final class DoubleFlag implements Flag<Double> {

    private final String key;
    private final double defaultValue;
    private final double min;
    private final double max;

    public DoubleFlag(String key, double defaultValue) {
        this(key, defaultValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public DoubleFlag(String key, double defaultValue, double min, double max) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public Double getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Optional<Double> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        double value;
        try {
            value = Double.parseDouble(input.trim());
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return Optional.empty();
        }
        return Optional.of(Math.max(this.min, Math.min(this.max, value)));
    }

    @Override
    public String serialize(Double value) {
        // integers print without a decimal point; the rest keep a stable trimmed form
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString(value.longValue());
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    @Override
    public String toString() {
        return "DoubleFlag{" + this.key + "}";
    }
}
