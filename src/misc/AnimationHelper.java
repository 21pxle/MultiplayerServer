package misc;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.Property;
import javafx.util.Duration;

public class AnimationHelper {

    public static <T> KeyFrame animate(Property<T> property, T endValue, double seconds) {
        return new KeyFrame(Duration.seconds(seconds), new KeyValue(property, endValue));
    }
}
