package client;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class ImageCheckbox {
    private Image image;
    private CheckBox box = new CheckBox();

    public ImageCheckbox(Image image) {
        this.image = image;
        box.setLayoutX(20);
    }

    public boolean getSelected() {
        return box.isSelected();
    }

    public VBox get() {
        VBox vBox = new VBox(new ImageView(image), box);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }
}
