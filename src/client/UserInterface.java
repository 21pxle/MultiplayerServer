package client;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

public class UserInterface extends VBox {

    private Text txtHealth;
    private SimpleIntegerProperty maxHealth = new SimpleIntegerProperty(0);
    private SimpleIntegerProperty health = new SimpleIntegerProperty(0);
    private SimpleListProperty<Card> cardList = new SimpleListProperty<>(FXCollections.observableArrayList());
    private String username;
    private Image image = null;

    public UserInterface(String username, List<Card> cardList, int health, int maxHealth, String image) {
        this.maxHealth.set(maxHealth);
        this.health.set(health);
        this.username = username;
        setAlignment(Pos.CENTER);
        Text txtUser = new Text(username);
        Rectangle rectangle = new Rectangle(100, 100);
        rectangle.setStroke(Color.BLACK);
        if (!image.equals("null") && !image.isEmpty()) {
            this.image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(image)), 100, 100, false, true);
            rectangle.setFill(new ImagePattern(this.image, 0, 0, 1, 1, true));
            rectangle.setWidth(this.image.getWidth());
            rectangle.setHeight(this.image.getHeight());
        }
        Text txtCardsLeft = new Text();
        txtHealth = new Text("Health: " + health);
        this.cardList.addAll(cardList);
        ProgressBar healthBar = new ProgressBar(1);
        healthBar.progressProperty().bind(Bindings.createDoubleBinding(() -> (double)(this.health.get()) / this.maxHealth.get(),
                this.health, this.maxHealth));
        healthBar.setStyle("-fx-accent: green;");
        txtHealth.textProperty().bind(Bindings.createStringBinding(() ->
                "Health: " + this.health.get() + " / " + this.maxHealth.get(), this.health, this.maxHealth));
        txtCardsLeft.textProperty().bind(Bindings.createStringBinding(() ->
                "Cards Left: " + this.cardList.size(), this.cardList.sizeProperty()));
        getChildren().addAll(txtUser, rectangle, txtCardsLeft, txtHealth, healthBar);

    }

    public SimpleIntegerProperty healthProperty() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth.get();
    }

    public Text getHealthText() {
        return txtHealth;
    }

    public int getHealth() {
        return health.get();
    }

    public void setHealth(int health) {
        this.health.set(health);
    }

    public int getCardsLeft() {
        return cardList.size();
    }

    public List<Card> getCards() {
        return cardList.get();
    }

    public String getUsername() {
        return username;
    }

    public void clearCards() {
        cardList.clear();
    }

    public SimpleListProperty<Card> cardsProperty() {
        return cardList;
    }

    public void setCards(List<Card> cardList) {
        this.cardList.set(FXCollections.observableList(cardList));
    }

    public void drawCard(Card card) {
        cardList.add(card);
    }

    public Image getImage() {
        return image;
    }
}
