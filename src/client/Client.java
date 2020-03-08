package client;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import misc.AnimationHelper;
import misc.ListExtension;
import misc.TwoKeyMap;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client extends Application {
    private int maxHealth = 100, bsDamage = 4;
    private SimpleIntegerProperty health = new SimpleIntegerProperty(maxHealth);
    private List<ImageCheckbox> checkBoxes = new ArrayList<>();
    private HBox cardsDisplay;
    private List<Card> selectedCards = new ArrayList<>();

    private String username = "";
    private List<String> users = new ArrayList<>();
    private boolean connected = false;

    private Socket socket;
    private PrintWriter writer;
    private final TextArea textArea = new TextArea();
    private final TextField textField = new TextField(), textFieldUsername = new TextField(),
    txtPort = new TextField(), txtHost = new TextField();
    private BufferedReader reader;
    private Stage stage;
    private ImageView discardPileImage = new ImageView();
    private int turns = 0;
    private final SimpleListProperty<Card> cards =
            new SimpleListProperty<>(this, "cards", FXCollections.observableArrayList());
    private boolean hasSelectedNoBS = false;
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private TextFlow flow1 = new TextFlow(new Text("Baloney Sandwich!")), flow2 = new TextFlow(new Text()), flow3 = new TextFlow(new Text());
    private Button btnCallBS = new Button();
    private Button btnDontCallBS = new Button();
    private Button btnPutCards = new Button();
    private Group group = new Group();
    private String startPlayer;
    private TwoKeyMap<String, Integer, UserInterface> userInterfaces = new TwoKeyMap<>();
    private Text requestedCardText = new Text();
    private Stage display = new Stage();
    private SimpleIntegerProperty discardPileSize = new SimpleIntegerProperty(0);
    private TwoKeyMap<String, Integer, UserInterface> unsortedMap = new TwoKeyMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("Client Login");
        Button connectButton = new Button("Connect");
        Button setAvatarButton = new Button("Set Avatar");
        Button disconnectButton = new Button("Disconnect");
        Button changeLogButton = new Button("Tips & Changes");
        Button sendButton = new Button("Send");
        ScrollPane scrollPane = new ScrollPane(textArea);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        txtHost.setPromptText("Host Name");
        txtPort.setPromptText("Port Number (1-65535)");
        textFieldUsername.setPromptText("Username");
        connectButton.setOnAction(e -> connectToServer());
        textFieldUsername.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                connectToServer();
            }
        });

        textFieldUsername.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                connectToServer();
            }
        });

        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
                Platform.runLater(() -> new ClientMessageConsumer(messageQueue, textArea).start());
                return null;
            }
        };
        task.run();

        disconnectButton.setOnAction(e -> {
            sendDisconnect();
            disconnect();
        });
        sendButton.setOnAction(e -> {
            if (connected) {
                if (!textField.getText().isEmpty()) {
                    writer.println(username + "\t" + textField.getText() + "\tM");
                    writer.flush();
                    textField.setText("");
                }
                textField.requestFocus();
            } else {
                try {
                    messageQueue.put("You are not connected.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        changeLogButton.setOnAction(e -> {
            try {
                List<String> lines = Files.readAllLines(new File("Updates.txt").toPath());
                for (String line : lines) {
                    messageQueue.put(line);
                }
                messageQueue.put("");
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        textField.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (connected) {
                    if (!textField.getText().isEmpty()) {
                        writer.println(username + "\t" + textField.getText() + "\tM");
                        writer.flush();
                        textField.setText("");
                    }
                    textField.requestFocus();
                } else {
                    try {
                        messageQueue.put("You are not connected.");
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        setAvatarButton.setOnAction(e -> {
            if (!username.isEmpty()) {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
                File selectedFile = chooser.showOpenDialog(this.stage);
                String path = selectedFile.getAbsolutePath();
                if (path.endsWith(".png") || path.endsWith(".jpg")) {
                    try {
                        byte[] bytes = Files.readAllBytes(new File(path).toPath());
                        writer.println(username + "\t" + Base64.getEncoder().encodeToString(bytes) + "\tIM");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    messageQueue.put("You are not connected.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        AnchorPane.setLeftAnchor(txtHost, 10d);
        AnchorPane.setTopAnchor(txtHost, 10d);
        txtHost.setPrefWidth(270);
        txtHost.setPrefHeight(30);

        AnchorPane.setLeftAnchor(txtPort, 290d);
        AnchorPane.setTopAnchor(txtPort, 10d);
        txtPort.setPrefWidth(270);
        txtPort.setPrefHeight(30);

        AnchorPane.setRightAnchor(connectButton, 120d);
        AnchorPane.setTopAnchor(connectButton, 50d);
        connectButton.setPrefWidth(100);
        connectButton.setPrefHeight(30);

        AnchorPane.setRightAnchor(setAvatarButton, 10d);
        AnchorPane.setTopAnchor(setAvatarButton, 50d);
        setAvatarButton.setPrefWidth(100);
        setAvatarButton.setPrefHeight(30);

        AnchorPane.setRightAnchor(changeLogButton, 120d);
        AnchorPane.setTopAnchor(changeLogButton, 10d);
        changeLogButton.setPrefWidth(100);
        changeLogButton.setPrefHeight(30);

        AnchorPane.setRightAnchor(disconnectButton, 10d);
        AnchorPane.setTopAnchor(disconnectButton, 10d);
        disconnectButton.setPrefWidth(100);
        disconnectButton.setPrefHeight(30);

        AnchorPane.setLeftAnchor(textField, 10d);
        AnchorPane.setTopAnchor(textField, 90d);
        textField.setPrefWidth(550);
        textField.setPrefHeight(30);
        textField.setPromptText("Type your message here...");

        AnchorPane.setLeftAnchor(textFieldUsername, 10d);
        AnchorPane.setTopAnchor(textFieldUsername, 50d);

        textFieldUsername.setPrefWidth(550);
        textFieldUsername.setPrefHeight(30);
        textFieldUsername.setPromptText("Enter your username (no more than 20 characters).");

        AnchorPane.setRightAnchor(sendButton, 10d);
        AnchorPane.setTopAnchor(sendButton, 90d);
        sendButton.setPrefWidth(210);
        sendButton.setPrefHeight(30);

        AnchorPane.setLeftAnchor(textArea, 10d);
        AnchorPane.setTopAnchor(textArea, 130d);
        textArea.setPrefWidth(740);
        textArea.setPrefHeight(420);

        AnchorPane pane = new AnchorPane(txtHost, txtPort, textFieldUsername,
                connectButton, setAvatarButton, changeLogButton, disconnectButton, textField, sendButton, scrollPane);
        AnchorPane.setLeftAnchor(scrollPane, 10d);
        AnchorPane.setTopAnchor(scrollPane, 130d);
        scrollPane.setPrefWidth(760);
        scrollPane.setPrefHeight(440);
        stage.setWidth(800);
        stage.setHeight(600);
        stage.setScene(new Scene(pane));
        stage.show();
    }

    private void connectToServer() {
        if (!connected) {
            String address = txtHost.getText();
            try {
                username = textFieldUsername.getText();
                if (username.length() >= 20 || !username.matches("^[\\p{L}\\-\\s]+$")) {
                    throw new IllegalArgumentException();
                }
                int port = Integer.parseInt(txtPort.getText());
                txtPort.setEditable(false);
                txtHost.setEditable(false);
                try {
                    socket = new Socket(address, port);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer = new PrintWriter(socket.getOutputStream());
                    writer.println(writer.toString() + "\t" + username + "\tID");
                    writer.flush();
                } catch (IOException ex) {
                    try {
                        messageQueue.put("Cannot connect to host " + txtHost.getText()
                                + " at port " + txtPort.getText() + ". Please try again.");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        txtHost.setEditable(true);
                        txtPort.setEditable(true);
                    }
                }
                listenThread();
            } catch (NumberFormatException ex) {
                try {
                    messageQueue.put("Invalid port number (must be 1-65536).");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IllegalArgumentException ex) {
                try {
                    messageQueue.put("Invalid username. Please try again.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                messageQueue.put("You are already connected.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void listenThread() {
        Thread incomingReader = new Thread(new IncomingReader());
        incomingReader.start();
    }

    public void addUser(String user) {
        users.add(user);
    }

    public void removeUser(String user) {
        try {
            messageQueue.put(user + " is now offline.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void printUsers() {
        String[] list = new String[users.size()];
        users.toArray(list);
        try {
            messageQueue.put("Here is the list of all the online users:");
            for (String str : list) {
                messageQueue.put(str);
            }
            messageQueue.put("");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendDisconnect() {
        String disconnectMessage = username + "\t\tD";
        try {
            writer.println(disconnectMessage);
            writer.flush();
        } catch (Exception e) {
            try {
                messageQueue.put("Cannot send disconnect message.");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void disconnect() {
        try {
            messageQueue.put("Disconnected.");
            socket.close();
            connected = false;
            removeUser(username);
        } catch (IOException | InterruptedException e) {
            try {
                messageQueue.put("Failed to disconnect. As if there was a way out...");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public class IncomingReader implements Runnable {
        @Override
        public void run() {
            List<Card> cardList;
            String stream;
            UserInterface ui;
            try {
                while ((stream = reader.readLine()) != null) {
                        String[] data = stream.split("\t");

                        switch (data[2]) {
                            //Message
                            case "M":
                                messageQueue.put(data[0] + ": " + data[1]);
                                break;
                            //Connect
                            case "C":
                                addUser(data[0]);
                                break;
                            //Disconnect
                            case "D":
                                removeUser(data[0]);
                                break;
                            //Finished
                            case "F":
                                if (data[1].equals(username)) {
                                    printUsers();
                                }
                                users.clear();
                                break;
                            //Draw Card
                            case "DC":
                                if (data[0].equals(username)) {
                                    cards.add(new Card(data[1]));
                                }
                                break;
                            case "DCs":
                                if (data[0].equals(username)) {
                                    cards.addAll(ListExtension.stringToCardList(data[1]));
                                }
                                break;
                            case "DCD":
                                if (turns == 0 && !data[0].contains(startPlayer)) {
                                    turns = 2;
                                    hasSelectedNoBS = false;
                                    requestedCardText.setText("Requested Card: 2");
                                }
                                if (data[0].contains(username)) {
                                    writer.println(username + "\t" + data[1] + "\tDCD");
                                    writer.flush();
                                }
                                break;
                            //Identification
                            case "ID":
                                // The writer has a unique hash code, which can be used to
                                // make sure that the message is sent to the right client.
                                if (data[1].equals("true") && data[0].equals(writer.toString())) {
                                    messageQueue.put("The username is a duplicate. Please try again.");
                                    txtHost.setEditable(true);
                                    txtPort.setEditable(true);
                                } else if (data[0].equals(writer.toString())) {
                                    username = username.trim();
                                    writer.println(username + "\thas connected.\tC");
                                    writer.flush();
                                    connected = true;
                                    Platform.runLater(() -> {
                                        textFieldUsername.setEditable(false);
                                        stage.setTitle("Chat: " + username);
                                    });
                                }
                                break;
                            //Ready
                            case "R":
                                writer.println(username + "\t" + (52 / Integer.parseInt(data[0])) + "\tDCs");
                                writer.flush();
                                break;
                            //Go!
                            case "G":
                                if (data[0].equals(username)) {
                                    Platform.runLater(() -> {
                                        cards.addAll(ListExtension.stringToCardList(data[1]));

                                        try {
                                            initGame();
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                                break;
                                //Sort Players
                            case "SP":
                                List<String> users = new ArrayList<>(ListExtension.stringToStringList(data[1]));
                                startPlayer = users.get(0);
                                for (int i = 0; i < users.size(); i++) {
                                    List<String> players = new ArrayList<>();
                                    unsortedMap.getValues().forEach(uiSP -> players.add(uiSP.getUsername()));
                                    String user = users.get(i);
                                    int index = players.indexOf(user);
                                    UserInterface userInterface = unsortedMap.getValueFromKey2(index + 1);
                                    userInterfaces.put(user, i, userInterface);
                                }
                                break;
                                //Transition
                            case "T":
                                turns += Integer.parseInt(data[0]);
                                hasSelectedNoBS = false;
                                requestedCardText.setText("Requested Card: " + new Card(1 + (turns / 2) % 13, 1).getRankName());
                                break;
                                //Turn
                            case "TURN":
                                if (data.length > 3) {
                                    cardList = ListExtension.stringToCardList(data[3]);
                                    if (data[0].equals(username)) {
                                        if (turns == 0 && startPlayer != null) {
                                            if (cardList.size() == 1 && cardList.get(0).equals(Card.ACE_OF_SPADES)) {
                                                cards.remove(Card.ACE_OF_SPADES);
                                                selectedCards.clear();
                                                writer.println(username + "\t\tPAs");
                                                writer.flush();
                                            } else {
                                                Platform.runLater(() -> {
                                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                                    alert.setHeaderText("Invalid Card(s)");
                                                    alert.setContentText("You can submit no less than one card and" +
                                                            "\nno more than four cards.");
                                                    alert.initModality(Modality.WINDOW_MODAL);
                                                    alert.show();
                                                });
                                            }
                                            selectedCards.clear();
                                        } else if (turns % 2 == 0) {
                                            //Ace of Spades
                                            if (cardList.size() > 0 && cardList.size() < 5) {
                                                cards.removeAll(cardList);
                                                String cardString = ListExtension.cardListToString(cardList);
                                                writer.println(data[0] + "\t" + cardString + "\tPC\t" + data[1] + "\t" + turns);
                                                selectedCards.clear();
                                                writer.flush();
                                            } else {
                                                selectedCards.clear();
                                                Platform.runLater(() -> {
                                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                                    alert.setHeaderText("Invalid Card(s)");
                                                    alert.setContentText("You can submit no less than one card and" +
                                                            "\nno more than four cards.");
                                                    alert.initModality(Modality.WINDOW_MODAL);
                                                    alert.show();
                                                });
                                            }
                                        } else {
                                            Platform.runLater(() -> {
                                                Alert alert = new Alert(Alert.AlertType.WARNING);
                                                alert.setHeaderText("Not Your Turn");
                                                alert.setContentText("It is not your turn yet. Please be patient.");
                                                alert.initModality(Modality.WINDOW_MODAL);
                                                alert.show();
                                            });
                                            selectedCards.clear();
                                        }
                                    }
                                    if (turns == 0 && startPlayer != null && cardList.size() == 1
                                            && cardList.get(0).equals(Card.ACE_OF_SPADES))
                                        discardPileImage.setImage(Card.ACE_OF_SPADES.getImage());
                                    else if (cardList.size() > 0 && cardList.size() < 5 && turns != 0) discardPileImage.setImage(Card.CARD_BACK);
                                } else if (data[0].equals(username)) {
                                    Platform.runLater(() -> {
                                        Alert alert = new Alert(Alert.AlertType.WARNING);
                                        alert.setHeaderText("Invalid Card(s)");
                                        alert.setContentText("You can submit no less than one card and" +
                                                "\nno more than four cards.");
                                        alert.initModality(Modality.WINDOW_MODAL);
                                        alert.show();
                                    });
                                    selectedCards.clear();
                                }
                                break;
                            //Display all interfaces.
                            case "DAI":
                                if (data[0].equals(username)) {
                                    Platform.runLater(() -> initDisplay(Integer.parseInt(data[1])));
                                }
                                break;
                                //Baloney Sandwich
                            case "BS":
                                displayBS(data[0], data[1], data[3].equals("true"), data[4], ListExtension.stringToCardList(data[5]), ListExtension.stringToCardList(data[6]));
                                //Server displays Baloney Sandwich message & disables all Baloney Sandwich and Put Cards buttons.
                                break;
                            //Recognize Death
                            case "RD":
                                if (username.equals(data[0])) {
                                    cards.clear();
                                    setDisableTurnButtons(true);
                                }
                                break;
                                //Exit Game
                            case "E":
                                cards.clear();
                                //Intentional Fallthrough
                                //Disable Buttons
                            case "DB":
                                setDisableTurnButtons(true);
                                break;
                            //Enable Buttons
                            case "EB":
                                setDisableTurnButtons(false);
                                break;
                            //Warning Messages
                            case "NOT-YOUR-TURN":
                                Platform.runLater(() -> {
                                    if (data[0].equals(username)) {
                                        Alert alert = new Alert(Alert.AlertType.WARNING);
                                        alert.setHeaderText("Not Your Turn");
                                        alert.setContentText("It is not your turn yet. Please be patient.");
                                        alert.initModality(Modality.WINDOW_MODAL);
                                        alert.show();
                                    }
                                });
                                break;
                            case "INVALID-CARDS":
                                if (data[0].equals(username)) {
                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                    alert.setHeaderText("Invalid Card(s)");
                                    alert.setContentText("You can submit no less than one card and" +
                                            "\nno more than four cards.");
                                    alert.initModality(Modality.WINDOW_MODAL);
                                    alert.show();
                                }
                                break;
                                //Add User Interface
                            case "ADD":
                                unsortedMap.put(data[0], Integer.parseInt(data[5]),
                                        new UserInterface(data[0],
                                                ListExtension.stringToCardList(data[1]),
                                                Integer.parseInt(data[3]), Integer.parseInt(data[4]), (data.length > 6) ? data[6] : ""));
                                break;
                                //Remove Card
                            case "RC":
                                userInterfaces.getValueFromKey1(data[0]).getCards().remove(new Card(data[1]));
                                break;
                                //Remove Cards
                            case "RCs":
                                userInterfaces.getValueFromKey1(data[0]).getCards().removeAll(ListExtension.stringToCardList(data[1]));
                                break;
                                //Discard Pile Modification
                            case "DPM":
                                new Timeline(
                                        AnimationHelper.animate(discardPileSize,
                                                Integer.parseInt(data[0]), 1)).play();
                                break;
                                //Modify Health
                            case "MH":
                                if (data.length > 3) {
                                    Timeline timeline4 = new Timeline();
                                    timeline4.getKeyFrames().add(
                                            AnimationHelper.animate(userInterfaces.getValueFromKey1(data[0]).healthProperty(),
                                                    Integer.parseInt(data[1]), 1)
                                    );
                                    timeline4.play();
                                    if (username.equals(data[3])) {
                                        writer.println(data[0] + "\t" + data[1] + "\tRD\t"
                                                + ListExtension.cardListToString(userInterfaces.getValueFromKey1(data[0]).getCards())
                                                + "\t" + username + "\tS");
                                        writer.flush();
                                    }
                                } else {
                                    Timeline timeline4 = new Timeline();
                                    timeline4.getKeyFrames().add(
                                            AnimationHelper.animate(userInterfaces.getValueFromKey1(data[0]).healthProperty(),
                                                    Integer.parseInt(data[1]), 1)
                                    );
                                    timeline4.play();
                                    if (username.equals(data[0])) {
                                        writer.println(data[0] + "\t" + data[1] + "\tRD\t" + ListExtension.cardListToString(cards.get()) + "\t" + username);
                                        writer.flush();
                                    }
                                }
                                break;
                                //Modify Failure
                            case "MF":
                                //User, Cards, Defender Cards, Health, Turn Queue
                                if (username.equals(data[0])) {
                                    cards.addAll(ListExtension.stringToCardList(data[1]));
                                    health.set(Integer.parseInt(data[4]));
                                }
                                recognizeVictory(ListExtension.stringToStringList(data[5]), ListExtension.stringToCardList(data[3]));
                                break;
                                //Recognize Victory
                            case "RV":
                                recognizeVictory(ListExtension.stringToStringList(data[0]), ListExtension.stringToCardList(data[1]));
                                break;
                                //Recognize Victory from Server
                            case "RVS":
                                List<String> players = ListExtension.stringToStringList(data[0]);
                                recognizeVictory(players, userInterfaces.getValueFromKey1(players.get(0)).getCards());
                                break;
                                //Modify Cards
                            case "MC":
                                Timeline timeline5 = new Timeline();
                                timeline5.getKeyFrames().add(
                                        AnimationHelper.animate(userInterfaces.getValueFromKey1(data[0]).cardsProperty(),
                                                FXCollections.observableList(ListExtension.stringToCardList(data[1])), 1)
                                );
                                timeline5.play();
                                break;
                                //Modify Successful Baloney Sandwich
                            case "MS":
                                if (data[0].equals(username)) {
                                    cards.addAll(ListExtension.stringToCardList(data[1]));
                                    health.set(Integer.parseInt(data[3]));
                                }
                                break;
                                //Clear Cards
                            case "CC":

                                Timeline timeline7 = new Timeline();
                                ui = userInterfaces.getValueFromKey1(data[0]);
                                timeline7.getKeyFrames().add(
                                        AnimationHelper.animate(ui.cardsProperty(),
                                                FXCollections.observableArrayList(), 1)
                                );
                                timeline7.play();
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + data[2]);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void recognizeVictory(List<String> turnQueue, List<Card> defenderCards) {
        if (defenderCards.isEmpty()) {
            cards.clear();
            if (turnQueue.get(0).equals(username)) {
                writer.println("[Game]\tCongratulations! " + username + " has won!\tM");
            }
            setDisableTurnButtons(true);
        } else if (turnQueue.contains(username)) {
            setDisableTurnButtons(false);
        }
    }

    private void displayBS(String attacker, String defender, boolean successful, String selectedMessage, List<Card> cards, List<Card> selectedCards) {
        Timeline t = new Timeline();

        Text result = (Text) (flow2.getChildren().get(0));
        Text subText = (Text) (flow3.getChildren().get(0));
        result.setText(successful ? "Successful!" : "Failed!");
        result.setVisible(true);
        subText.setVisible(true);
        subText.setText(selectedMessage);

        int numCardsScanned, index;
        for (numCardsScanned = 0, index = selectedCards.size() - 1;
             numCardsScanned < selectedCards.size();
             numCardsScanned++, index--) {
            Card c = selectedCards.get(index);
            try {
                t.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(numCardsScanned * 2),
                                new KeyValue(discardPileImage.imageProperty(), c.getImage())));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (c.getRank() != 1 + turns / 2 % 13) {
                numCardsScanned++;
                break;
            }
        }
        t.getKeyFrames().add(
                new KeyFrame(Duration.seconds(numCardsScanned * 2),
                        new KeyValue(discardPileImage.imageProperty(), null)));

        t.getKeyFrames().addAll(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(flow1.visibleProperty(), true)),

                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1),
                        new KeyValue(flow2.visibleProperty(), true)),
                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1),
                        new KeyValue(flow2.layoutXProperty(), 400 - result.getText().length() * 10)),
                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1),
                        new KeyValue(flow2.layoutYProperty(), 250)),

                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 2),
                        new KeyValue(flow3.visibleProperty(), true)),
                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 2),
                        new KeyValue(flow3.layoutXProperty(), 400 - selectedMessage.length() * 4)),
                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 2),
                        new KeyValue(flow3.layoutYProperty(), 400)),

                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 7),
                        new KeyValue(flow1.visibleProperty(), false)),
                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 7),
                        new KeyValue(flow2.visibleProperty(), false)),
                new KeyFrame(Duration.seconds(numCardsScanned * 2 + 7),
                        new KeyValue(flow3.visibleProperty(), false))
        );


        if (successful) {
            Color[] rainbow = {Color.RED, Color.YELLOW, Color.LIME, Color.CYAN, Color.BLUE, Color.MAGENTA};
            int increment = (Math.random() > 0.5) ? 1 : -1;
            int startColor = (int) (6 * Math.random());
            for (int i = 0; i <= 24; i++) {
                Color color = rainbow[(24 + startColor + i * increment) % 6];
                t.getKeyFrames().addAll(
                        new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1 + i * 0.25),
                                new KeyValue(result.strokeProperty(), color)),
                        new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1 + i * 0.25),
                                new KeyValue(result.fillProperty(), color))
                );
            }
        } else {
            t.getKeyFrames().addAll(
                    new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1),
                            new KeyValue(result.strokeProperty(), Color.DARKRED)),
                    new KeyFrame(Duration.seconds(numCardsScanned * 2 + 1),
                            new KeyValue(result.fillProperty(), Color.DARKRED)),
                    new KeyFrame(Duration.seconds(numCardsScanned * 2 + 2),
                            new KeyValue(result.strokeProperty(), Color.BLACK)),
                    new KeyFrame(Duration.seconds(numCardsScanned * 2 + 2),
                            new KeyValue(result.fillProperty(), Color.BLACK))
            );
        }

        t.setOnFinished(e -> {
            //Cards - Name 1
            int health;
            if (username.equals(attacker)) {
                writer.println("[Game]\t" + selectedMessage.replace("\n", " ") + "\tM");
                writer.flush();
                selectedCards.clear();
            }
            if (result.getText().equals("Successful!")) {
                health = userInterfaces.getValueFromKey1(defender).getHealth();
                if (health - bsDamage * cards.size() > 0 || !username.equals(defender)) {
                    setDisableTurnButtons(false);
                }

                writer.println(defender + "\t" + ListExtension.cardListToString(cards) + "\tBSS\t"
                        + bsDamage * cards.size() + "\t" + health + "\t" + username);
                //Server or Client?
                writer.flush();
            } else {
                List<Card> defenderCards = new ArrayList<>(userInterfaces.getValueFromKey1(defender).getCards());
                health = userInterfaces.getValueFromKey1(attacker).getHealth();
                if (health - bsDamage * cards.size() > 0 || !username.equals(attacker)) {
                    setDisableTurnButtons(false);
                }

                writer.println(attacker + "\t" + ListExtension.cardListToString(defenderCards) + "\tBSF\t"
                        + bsDamage * cards.size() + "\t" + ListExtension.cardListToString(cards) + "\t"
                        + health + "\t" + username + "\t" + defender);
                writer.flush();
            }
            //Decide who gets the next card.
        });
        t.play();
    }

    private void setDisableTurnButtons(boolean disable) {
        btnPutCards.setDisable(disable);
        btnCallBS.setDisable(disable);
        btnDontCallBS.setDisable(disable);
    }

    public void initGame() throws FileNotFoundException {
        //Text Area, Text Field, and Buttons
        stage.setTitle("Game: " + username);
        ScrollPane pane = new ScrollPane(textArea);
        AnchorPane.setLeftAnchor(pane, 10d);
        AnchorPane.setTopAnchor(pane, 10d);
        textArea.setPrefWidth(600);
        textArea.setPrefHeight(400);

        AnchorPane.setLeftAnchor(textField, 10d);
        AnchorPane.setBottomAnchor(textField, 210d);
        textField.setPrefHeight(30);
        textField.setMaxHeight(30);
        textField.setPrefWidth(430);
        textField.setMaxWidth(430);
        Button btnSendMessage = new Button("Send Message");
        Button btnHTP = new Button("How to Play");
        btnCallBS = new Button("Call Baloney Sandwich");
        btnDontCallBS = new Button("Don't Call Baloney Sandwich");
        btnPutCards = new Button("Put Cards");
        Button btnAddDisplay = new Button("Add Display");
        VBox vBoxButtons = new VBox(20, btnSendMessage, btnHTP, btnCallBS, btnDontCallBS, btnPutCards, btnAddDisplay);

        AnchorPane.setBottomAnchor(vBoxButtons, 10d);
        AnchorPane.setRightAnchor(vBoxButtons, 10d);

        cardsDisplay = new HBox(20);
        for (Card card : cards) {
            ImageCheckbox checkbox = new ImageCheckbox(card.getImage());
            cardsDisplay.getChildren().add(checkbox.get());
            checkBoxes.add(checkbox);
        }
        ScrollPane cardsPane = new ScrollPane(cardsDisplay);
        cardsPane.setPrefWidth(430);
        cardsPane.setPrefHeight(150);

        VBox vBoxDisplay = new VBox(textField, cardsPane);

        AnchorPane.setBottomAnchor(vBoxDisplay, 60d);
        AnchorPane.setLeftAnchor(vBoxDisplay, 10d);

        btnSendMessage.setOnAction(e -> {

            writer.println(username + "\t" + textField.getText() + "\tM");
            textField.setText("");
            writer.flush();
        });

        //Add More Cards
        Scene game = new Scene(new AnchorPane(pane, vBoxButtons, vBoxDisplay));

        health.addListener((observable, oldValue, newValue) -> {
            writer.println(username + "\t" + newValue + "\tMH");
            writer.flush();
        });

        cards.addListener((ListChangeListener<Card>) c ->
                Platform.runLater(() -> {
                    if (c.next() && (c.wasAdded() || c.wasRemoved())) {
                        cardsDisplay.getChildren().clear();
                        checkBoxes.clear();
                        cards.get().forEach(card -> {
                            try {
                                ImageCheckbox checkbox = new ImageCheckbox(card.getImage());
                                cardsDisplay.getChildren().add(checkbox.get());
                                checkBoxes.add(checkbox);
                            } catch (FileNotFoundException ex) {
                                ex.printStackTrace();
                            }
                        });
                        //Modify Cards
                        writer.println(username + "\t" + ListExtension.cardListToString(cards.get()) + "\tMC");
                        writer.flush();
                    }
                }
        ));

        stage.setScene(game);

        stage.setOnCloseRequest(e -> {
            writer.println(username + "\t\tQ\t" + ListExtension.cardListToString(cards));
            writer.flush();
        });

        btnPutCards.setOnAction(e -> {
            for (int i = 0; i < cards.size(); i++) {
                boolean selected = checkBoxes.get(i).getSelected();
                Card card = cards.get(i);
                if (selected) {
                    selectedCards.add(card);
                }
            }
            writer.println(username + "\t" + ListExtension.cardListToString(selectedCards) + "\tCCP");
            writer.flush();
        });

        btnCallBS.setOnAction(e -> {
            if (turns % 2 == 1 && !hasSelectedNoBS) {
                writer.println(username + "\t" + turns + "\tCNCP");
                writer.flush();
            } else if (hasSelectedNoBS) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not Your Turn");
                alert.setContentText("You have already selected.");
                alert.initModality(Modality.NONE);
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not Your Turn");
                alert.setContentText("It is not your turn yet. Please be patient.");
                alert.initModality(Modality.NONE);
                alert.show();
            }
        });

        btnDontCallBS.setOnAction(e -> {
            if (turns % 2 == 1 && !hasSelectedNoBS) {
                writer.println(username + "\t\tNBS");
                writer.flush();
                hasSelectedNoBS = true;
                //If Server is not the current player.
            } else if (!hasSelectedNoBS) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not Your Turn");
                alert.setContentText("It is not your turn yet. Please be patient.");
                alert.initModality(Modality.NONE);
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not Your Turn");
                alert.setContentText("You opted not to call Baloney Sandwich. Please be patient.");
                alert.initModality(Modality.NONE);
                alert.show();
            }
        });
        btnAddDisplay.setOnAction(e -> {
            writer.println(username + "\t\tDAI");
            writer.flush();
        });

        btnHTP.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("How to Play");
            alert.setContentText("1. Start Player has to play the Ace of Spades." +
                    "\n2. Play goes in clockwise order (2’s, 3’s, etc.)." +
                    "\n3. A turn occurs when you put down at" +
                    "\nleast one card but less than 4 cards of" +
                    "\nthe requested rank, which will increase sequentially." +
                    "\n4. The cards will remain hidden until someone calls Baloney Sandwich." +
                    "\n5. If Person A calls Baloney Sandwich, then if" +
                    "\nPerson B calls Baloney Sandwich and..." +
                    "\n5a. Person A has at least one card that is not of the requested rank," +
                    "\nthen Person A has to draw all the cards from the discard pile." +
                    "\n5b. Person A has all the cards of the requested rank," +
                    "\nthen Person B has to draw all the cards from the discard pile." +
                    "\n6. Each card can rack up four points of damage when someone calls Baloney Sandwich." +
                    "\n7. In addition, you can damage a player for three points of damage per card when you attack a player." +
                    "\n8. The last player alive or the first player to get rid of all the cards wins the game." +
                    "\n9. Have fun and enjoy the game.");
            alert.initModality(Modality.NONE);
            alert.show();
        });
        stage.setWidth(650);
        stage.setHeight(720);

        //Set Turn Queue
        writer.println(username + "\t" + cards.contains(Card.ACE_OF_SPADES) + "\tI\t"
                + ListExtension.cardListToString(cards.get()) + "\t" + health.get() + "\t" + maxHealth);
        writer.flush();
    }

    public void initDisplay(int size) {
        display.setResizable(false);
        display.setWidth(800);
        display.setHeight(600);
        display.setTitle("Display: " + username);

        Text bsText = new Text("Baloney Sandwich!");
        bsText.setFont(Font.font(50));
        flow1 = new TextFlow(bsText);
        flow1.setVisible(false);
        flow1.setLayoutX(400 - bsText.getBoundsInLocal().getWidth() / 2);
        flow1.setLayoutY(100);
        flow1.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        flow1.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        Text result = new Text("Successful!");
        result.setFont(Font.font(40));
        flow2 = new TextFlow(result);
        flow2.setVisible(false);
        flow2.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        flow2.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        Text subText = new Text();
        subText.setFont(Font.font(20));
        flow3 = new TextFlow(subText);
        flow3.setVisible(false);
        flow3.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        flow3.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        requestedCardText = new Text("Requested Card: " + new Card(1 + (turns / 2) % 13, 1).getRankName());
        requestedCardText.setFont(Font.font(30));
        TextFlow flow4 = new TextFlow(requestedCardText);
        flow4.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        flow4.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        flow4.setLayoutX(0);
        flow4.setLayoutY(50);

        Text discardPileText = new Text();
        discardPileText.setFont(Font.font(30));
        TextFlow flow5 = new TextFlow(discardPileText);
        discardPileText.textProperty().bind(Bindings.createStringBinding(() -> "Discard Pile Size: " + discardPileSize.get(), discardPileSize));
        flow5.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        flow5.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        flow5.layoutXProperty().bind(Bindings.createDoubleBinding(() -> 790 - flow5.getWidth(), flow5.widthProperty()));
        flow5.setLayoutY(50);

        if (group.getChildren().size() != 0) {
            group.getChildren().clear();
        }

        int userInterfaceIndex = 0;
        for (UserInterface userInterface : userInterfaces.getValues()) {
            userInterface.setLayoutX(350 + 320 * Math.cos(2 * Math.PI * userInterfaceIndex / size));
            userInterface.setLayoutY(180 + 180 * Math.sin(2 * Math.PI * userInterfaceIndex++ / size));
            group.getChildren().add(userInterface);
        }
        discardPileImage.setLayoutX(350);
        discardPileImage.setLayoutY(180);
        group.getChildren().addAll(discardPileImage, flow4, flow5, flow1, flow2, flow3);

        if (group.getScene() == null) {
            display.setScene(new Scene(group));
        } else {
            display.setScene(group.getScene());
        }
        display.show();
    }


}