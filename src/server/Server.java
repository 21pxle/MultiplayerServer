package server;

import client.Card;
import client.Deck;
import client.UserInterface;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import misc.ListExtension;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Server extends Application {
    private Map<String, UserInterface> interfaces = new HashMap<>();
    private final TextArea textArea = new TextArea();
    private final TextField txtPort = new TextField();
    private List<PrintWriter> clientStreams = new ArrayList<>();
    private Set<String> users = new HashSet<>();
    private Deck deck = new Deck();
    private List<Card> discardPile = new ArrayList<>();
    private List<Card> selectedCards = new ArrayList<>();
    private Queue<String> turnQueue = new ArrayDeque<>();
    private List<String> userList = new ArrayList<>();
    private Map<String, String> images = new HashMap<>();
    private int noBSCalls = 0;
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final List<String> deathMessagesList = Arrays.asList(" has unfortunately died...",
            " could have won...", " might do better next time...", "... Aww, don't cry...",
            ", at least I got you a teddy bear...", ", I feel sad now because of you. :(", "... Sorry... :(",
            ", don't be sad... Have a hug.", ", you will be missed...", "... maybe next time.",
            ", practice makes perfect.", ", don't let your hopes down.",
            ", maybe if we can resurrect you, you might have another shot.",
            ", the times are tough...", ", keep calm and carry on.", ", I know... it's OK buddy.",
            ", now is the time to look at cute photos of dogs and cats.", ", you have worked very hard.",
            ", don't give up.", ", you've been a good fighter.");

    private List<String> quitMessagesList = Arrays.asList(" is a coward.", " quitted... what a coward.", ", I kindly beg you to differ.",
            " is insta-killed.", " should reconsider.", " made the dumbest decision.", " committed suicide.", " disappeared without a trace.",
            ", stop inviting people to quit!", " has won the Darwin Award!", ", where did you go?", ", I don't think you could run away from death.",
            ", stop hurting yourself!", " tried to escape.", " has won the game... Oh wait, nevermind!", " should stop quitting games!",
            ", you should have played Electric Field Hockey instead!", ", stop being so impatient!", ", I'm pretty sure quitting will get you mercilessly killed.",
            " took the easy way out!", ", that is not professional!", ", take a look at what you've done!", ", you thought you could get away with quitting.",
            ", you've given up your right to rejoin.", ", you might have embarrassed yourself...", " will not be missed.");
    private String startPlayer;
    private int playerCount = 0;
    private Stack<Card> deadCards = new Stack<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Button startButton = new Button("Start Server");
        Button endButton = new Button("End Server");
        Button onlineUsersButton = new Button("Get Online Users");
        Button clearLogButton = new Button("Clear Log");
        Button readyButton = new Button("Ready");
        ScrollPane scrollPane = new ScrollPane(textArea);
        textArea.setWrapText(true);

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                Platform.runLater(() -> new ServerMessageConsumer(messageQueue, textArea).start());
                return null;
            }
        };
        new Thread(task).start();


        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(txtPort.getText());
                if (port > 65535 || port <= 0) {
                    throw new IllegalArgumentException();
                }

                try {
                    stage.setTitle("Server: " + InetAddress.getLocalHost().getHostAddress() + " at port " + port);
                } catch (UnknownHostException ex) {
                    ex.printStackTrace();
                }
                Thread start = new Thread(new ServerInit(port));
                start.start();

                messageQueue.put("Server started...");
            } catch (IllegalArgumentException | InterruptedException ex) {
                try {
                    messageQueue.put("Invalid Port: Port must be a numeric argument from 0 to 65535.");
                } catch (InterruptedException exc) {
                    exc.printStackTrace();
                }
            }
        });

        txtPort.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                try {
                    int port = Integer.parseInt(txtPort.getText());
                    if (port > 65535 || port <= 0) {
                        throw new IllegalArgumentException();
                    }

                    Thread start = new Thread(new ServerInit(port));
                    start.start();

                    messageQueue.put("Server started...");
                } catch (IllegalArgumentException | InterruptedException ex) {
                    try {
                        messageQueue.put("Invalid Port: Port must be a numeric argument from 1 to 65535.");
                    } catch (InterruptedException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });



        endButton.setOnAction(e -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (clientStreams == null) {
                try {
                    messageQueue.put("Server did not start yet.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } else if (clientStreams.isEmpty()) {
                try {
                    messageQueue.put("Server has already stopped.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } else {
                broadcast("[Announcement]\tServer is stopping and all users are disconnected.\tM");
                try {
                    messageQueue.put("Server stopped.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        onlineUsersButton.setOnAction(e -> {
            if (users.size() > 0) {
                try {
                    messageQueue.put("Here are the online users:");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                for (String user : users) {
                    try {
                        messageQueue.put(user + "");
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    messageQueue.put("There are no online users.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            try {
                messageQueue.put("");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        readyButton.setOnAction(e -> {
            if (users.size() >= 2 && users.size() < 9) {
                broadcast(users.size() + "\t\tR");
                readyButton.setDisable(true);
            } else if (users.size() < 2) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not Enough Users");
                alert.setContentText("You need " + (2 - users.size()) + " more player(s) to play the game!");
                alert.initModality(Modality.WINDOW_MODAL);
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Too Many Users");
                alert.setContentText("The server can only support up to 8 players!");
                alert.initModality(Modality.WINDOW_MODAL);
                alert.show();
            }
        });

        clearLogButton.setOnAction(e -> textArea.clear());
        txtPort.setPromptText("Port Number (1-65535)");

        AnchorPane.setTopAnchor(scrollPane, 10d);
        AnchorPane.setLeftAnchor(scrollPane, 10d);
        scrollPane.setPrefWidth(780);
        scrollPane.setPrefHeight(460);

        textArea.setEditable(false);
        textArea.setPrefWidth(750);
        textArea.setPrefHeight(420);

        startButton.setPrefWidth(100);
        startButton.setPrefHeight(30);
        AnchorPane.setLeftAnchor(startButton, 10d);
        AnchorPane.setBottomAnchor(startButton, 50d);

        endButton.setPrefWidth(150);
        endButton.setPrefHeight(30);
        AnchorPane.setLeftAnchor(endButton, 120d);
        AnchorPane.setBottomAnchor(endButton, 50d);

        readyButton.setPrefHeight(30);
        AnchorPane.setRightAnchor(readyButton, 280d);
        AnchorPane.setLeftAnchor(readyButton, 280d);
        AnchorPane.setBottomAnchor(readyButton, 50d);

        onlineUsersButton.setPrefWidth(150);
        onlineUsersButton.setPrefHeight(30);
        AnchorPane.setRightAnchor(onlineUsersButton, 120d);
        AnchorPane.setBottomAnchor(onlineUsersButton, 50d);

        clearLogButton.setPrefWidth(100);
        clearLogButton.setPrefHeight(30);
        AnchorPane.setRightAnchor(clearLogButton, 10d);
        AnchorPane.setBottomAnchor(clearLogButton, 50d);

        AnchorPane.setLeftAnchor(txtPort, 10d);
        AnchorPane.setRightAnchor(txtPort, 10d);
        AnchorPane.setBottomAnchor(txtPort, 10d);
        txtPort.setPrefHeight(30);

        AnchorPane pane = new AnchorPane(scrollPane, startButton, endButton, onlineUsersButton, clearLogButton, readyButton, txtPort);
        stage.setScene(new Scene(pane));
        stage.setTitle("Server");
        stage.setWidth(800);
        stage.setHeight(600);
        stage.show();
    }

    public String getDeathMessage() {
        return deathMessagesList.get(new Random().nextInt(deathMessagesList.size()));
    }


    public class ClientThread implements Runnable {

        BufferedReader reader;
        Socket socket;

        PrintWriter client;
        public ClientThread(Socket socket, PrintWriter client) {
            this.client = client;
            try {
                this.socket = socket;
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                try {
                    messageQueue.put(e.getMessage());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        @Override
        public void run() {
            String message;
            String[] data;
            UserInterface ui;
            try {
                while ((message = reader.readLine()) != null) {
                    data = message.split("\t");
                    switch (data[2]) {
                        case "B":
                            broadcast("[Announcement]\t" + data[0] + "\tM");
                            break;
                        //Connect
                        case "C":
                            //Indexing Figure
                            addUser(data[0]);
                            broadcast("[Announcement]\t" + data[0] + " has connected.\tM");
                            break;
                        //Disconnect
                        case "D":
                            images.remove(data[0]);
                            removeUser(data[0]);
                            break;
                        //Message
                        case "M":
                            broadcast(message);
                            break;
                            //Image
                        case "IM":
                            images.put(data[0], data[1]);
                            break;
                        //Draw Cards
                        case "DCs":
                            try {
                                List<Card> cards = deck.draw(Integer.parseInt(data[1]));
                                //Cards
                                try {
                                    messageQueue.put(data[0] + " got " + data[1] + " brand new cards.");
                                    broadcast(data[0] + "\t" + ListExtension.cardListToString(cards) + "\tG");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } catch (IndexOutOfBoundsException e) {
                                try {
                                    messageQueue.put("There are no more cards.");
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            break;
                            //Quit
                        case "Q":
                            turnQueue.remove(data[0]);
                            List<Card> cards1 = ListExtension.stringToCardList(data[3]);
                            broadcast(UserInterfaceHelper.clearCards(data[0]));
                            Collections.shuffle(cards1);
                            broadcast(data[0] + "\t\tRD");
                            if (turnQueue.size() == 1) {
                                broadcast("[Game]\tCongratulations! " + turnQueue.element() + " has won!\tM");
                                broadcast("\t\tE");
                            } else {
                                deadCards.addAll(cards1);
                                broadcast(ListExtension.stringListToString(turnQueue) + "\t" + deadCards.size() / turnQueue.size()
                                        + "\tDCD");
                                broadcast("[Game]\t" + turnQueue.element() + " can now put down some cards.\tM");
                            }
                            broadcast(data[0] + "\t0\tMH\t" + turnQueue.element());
                            broadcast("[Game]\t" + data[0] + getQuitMessage() + "\tM");
                            break;
                        //Draw Card
                        case "DC":
                            Card card = deck.draw();
                            try {
                                messageQueue.put(data[0] + " got a brand new card: the " + card.toString() + "");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            broadcast(data[0] + "\t" + card.getShortName() + "\tDC");
                            break;
                        //Identification is necessary to prevent duplicate accounts.
                        case "ID":
                            try {
                                messageQueue.put("Testing for duplicate username...");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            broadcast(data[0] + "\t" + users.contains(data[1]) + "\tID");
                            break;
                        //Initialize the game.
                        case "I":
                            playerCount++;
                            try {
                                messageQueue.put("Initializing Turn Queue...");
                                userList.add(data[0]);
                                if (data[1].equals("true")) {
                                    startPlayer = data[0];
                                    broadcast("[Game]\t" + startPlayer + " has the Ace of Spades and can therefore go first.\tM");
                                }

                                broadcast(UserInterfaceHelper.init(data[0],
                                        ListExtension.stringToCardList(data[3]), Integer.parseInt(data[4]), Integer.parseInt(data[5]),
                                        userList.size(), images.getOrDefault(data[0], "")));

                                interfaces.put(data[0], new UserInterface(data[0], ListExtension.stringToCardList(data[3]),
                                        Integer.parseInt(data[4]), Integer.parseInt(data[5]), images.getOrDefault(data[0], "")));
                                if (playerCount == users.size()) {
                                    while (deck.hasCards()) {
                                        Card cardDC = deck.draw();
                                        List<String> players = new ArrayList<>(users);
                                        Collections.shuffle(players);
                                        broadcast(players.get(0) + "\t" + cardDC.getShortName() + "\tDC");
                                    }
                                    if (turnQueue.size() < users.size()) {
                                        Collections.shuffle(userList);
                                        userList.remove(startPlayer);
                                        userList.add(0, startPlayer);
                                        turnQueue.addAll(userList);
                                        broadcast(startPlayer + "\t" + ListExtension.stringListToString(userList) + "\tSP");
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        //Check for Current Player
                        case "CCP":
                            if (turnQueue.element().equals(data[0])) {
                                broadcast(data[0] + "\t" + new ArrayList<>(turnQueue).get(1) + "\tTURN\t" + data[1]);
                            } else {
                                broadcast(data[0] + "\t\tNOT-YOUR-TURN");
                            }
                            break;
                        //Check for Not the Current Player
                        case "CNCP":
                            if (turnQueue.element().equals(data[0])) {
                                broadcast(data[0] + "\t\tNOT-YOUR-TURN");
                            } else {
                                broadcast("[Game]\t" + data[0] + " chose to call Baloney Sandwich on " + turnQueue.element() + "\tM");
                                displayBS(data[0], turnQueue.element(), Integer.parseInt(data[1]));
                            }
                            break;
                            //No Baloney Sandwich
                        case "NBS":
                            if (turnQueue.element().equals(data[0])) {
                                broadcast(data[0] + "\t\tNOT-YOUR-TURN");
                            } else {
                                broadcast("[Game]\t" + data[0] + " chose not to call Baloney Sandwich on " + turnQueue.element() + ".\tM");
                                noBSCalls++;
                                if (noBSCalls == turnQueue.size() - 1) {
                                    turnQueue.add(turnQueue.remove());
                                    broadcast("[Game]\t" + turnQueue.element() + " can now put down some cards.\tM");
                                    broadcast("1\t\tT");
                                    broadcast(ListExtension.stringListToString(new ArrayList<>(turnQueue)) + "\t\tRVS");
                                    selectedCards.clear();
                                    noBSCalls = 0;
                                }
                            }
                            break;
                            //Put Ace of Spades
                        case "PAs":
                            discardPile.add(Card.ACE_OF_SPADES);
                            broadcast("[Game]\t" + turnQueue.element() + " has put down the Ace of Spades.\tM");
                            broadcast("1\t\tDPM");
                            broadcast(UserInterfaceHelper.removeCard(turnQueue.element(), Card.ACE_OF_SPADES));
                            turnQueue.add(turnQueue.remove());
                            ui = interfaces.get(turnQueue.element());
                            broadcast(UserInterfaceHelper.modifyHealth(turnQueue.element(),
                                    Math.max(0, ui.getHealth() - 3)));
                            ui.setHealth(Math.max(0, ui.getHealth() - 3));
                            broadcast("2\t\tT");
                            broadcast("[Game]\t" + turnQueue.element() + " can now put down some cards.\tM");
                            selectedCards.clear();
                            break;
                            //Display All Interfaces
                        case "DAI":
                            broadcast(data[0] + "\t" + userList.size() + "\tDAI");
                            break;
                            //Baloney Sandwich Successful
                        case "BSS":
                            noBSCalls = 0;
                            List<Card> list = ListExtension.stringToCardList(data[1]);
                            if (data[5].equals(data[0])) {
                                broadcast("\t\tEB");
                                broadcast(UserInterfaceHelper.modifyBSS(data[0], list, Math.max(0, Integer.parseInt(data[4]) - Integer.parseInt(data[3]))));
                                interfaces.get(data[0]).setHealth(Math.max(0, Integer.parseInt(data[4]) - Integer.parseInt(data[3])));
                                discardPile.clear();
                                selectedCards.clear();
                                turnQueue.add(turnQueue.remove());
                                broadcast("[Game]\t" + turnQueue.element() + " can now put down some cards.\tM");
                                broadcast("0\t\tDPM");
                                broadcast("1\t\tT");
                            }
                            break;
                        //Baloney Sandwich Failed
                        case "BSF":
                            if (data[6].equals(data[7])) {
                                noBSCalls = 0;
                                broadcast("\t\tEB");
                                List<Card> list2 = ListExtension.stringToCardList(data[1]);
                                List<Card> list3 = ListExtension.stringToCardList(data[4]);
                                broadcast(UserInterfaceHelper.modifyBSF(data[0], list3, list2,
                                        Math.max(0, Integer.parseInt(data[5]) - Integer.parseInt(data[3])), new ArrayList<>(turnQueue)));
                                interfaces.get(data[0]).setHealth(Math.max(0, Integer.parseInt(data[5]) - Integer.parseInt(data[3])));
                                if (list2.size() == 0) {
                                    broadcast(data[6] + "\t" + data[1] + "\tRV");
                                }
                                turnQueue.add(turnQueue.remove());
                                broadcast("[Game]\t" + turnQueue.element() + " can now put down some cards.\tM");
                                discardPile.clear();
                                broadcast("0\t\tDPM");
                                broadcast("1\t\tT");

                                selectedCards.clear();
                            }
                            break;
                            //Deck Reset
                        case "DR":
                            //Fallthrough is intentional.
                            deck = new Deck();
                            //Clear Discard Pile
                        case "CL":
                            discardPile.clear();
                            break;
                            //Placed Cards
                        case "PC":
                            String[] cardTokens = data[1].split(" ");
                            for (String s : cardTokens) {
                                discardPile.add(new Card(s));
                                selectedCards.add(new Card(s));
                            }
                            ui = interfaces.get(turnQueue.element());
                            ui.getCards().removeAll(selectedCards);
                            UserInterface uiDefender = interfaces.get(new ArrayList<>(turnQueue).get(1));
                            uiDefender.setHealth(Math.max(0, uiDefender.getHealth() - 3 * selectedCards.size()));
                            broadcast("1\t\tT");
                            broadcast(discardPile.size() + "\t\tDPM");
                            broadcast(UserInterfaceHelper.modifyHealth(new ArrayList<>(turnQueue).get(1), uiDefender.getHealth()));
                            broadcast("[Game]\t" + data[0] + " attacks " + data[3] + " for " + (3 * selectedCards.size())
                                    + " damage and claims to have put down " + selectedCards.size() + " card(s) of " +
                                    new Card(1 + Integer.parseInt(data[4]) / 2 % 13, 1).getRankName() + ".\tM");
                            break;
                        //Recognition of Death
                        case "RD":
                            if (data.length > 5) {
                                playerCount = 0;
                                if (data[0].equals(data[4])) {
                                    turnQueue.remove(data[0]);
                                    List<Card> cards = ListExtension.stringToCardList(data[3]);
                                    broadcast(UserInterfaceHelper.clearCards(data[0]));
                                    Collections.shuffle(cards);
                                    broadcast(data[0] + "\t\tRD");
                                    if (turnQueue.size() == 1) {
                                        broadcast("[Game]\tCongratulations! " + turnQueue.element() + " has won!\tM");
                                        broadcast("\t\tE");
                                    } else {
                                        deadCards.addAll(cards);
                                        broadcast(ListExtension.stringListToString(turnQueue) + "\t" + deadCards.size() / turnQueue.size()
                                                + "\tDCD");
                                        if (Integer.parseInt(data[5]) % 2 == 0)
                                            broadcast("[Game]\t" + turnQueue.element() + " can now put down some cards.\tM");
                                    }
                                }
                            } else if (Integer.parseInt(data[1]) == 0) {
                                playerCount = 0;
                                if (data[0].equals(data[4])) {
                                    turnQueue.remove(data[0]);
                                    broadcast("[Game]\t" + data[0] + getDeathMessage() + "\tM");
                                    List<Card> cards = ListExtension.stringToCardList(data[3]);
                                    broadcast(UserInterfaceHelper.clearCards(data[0]));
                                    Collections.shuffle(cards);
                                    broadcast(data[0] + "\t\tRD");
                                    if (turnQueue.size() == 1) {
                                        broadcast("[Game]\tCongratulations! " + turnQueue.element() + " has won!\tM");
                                        broadcast("\t\tE");
                                    } else {
                                        deadCards.addAll(cards);
                                        broadcast(ListExtension.stringListToString(turnQueue) + "\t" + deadCards.size() / turnQueue.size()
                                                + "\tDCD");
                                    }
                                }
                            }
                            break;
                            //Modify Cards
                        case "MC":
                            broadcast(data[0] + "\t" + data[1] + "\tMC");
                            break;
                            //Modify Health
                        case "MH":
                            broadcast(data[0] + "\t" + data[1] + "\tMH");
                            break;
                        //Draw Cards from Dead
                        case "DCD":
                            if (turnQueue.contains(data[0])) {
                                List<Card> cards = new ArrayList<>();
                                for (int i = 0; i < Integer.parseInt(data[1]); i++) {
                                    cards.add(deadCards.pop());
                                }
                                broadcast(data[0] + "\t" + ListExtension.cardListToString(cards) + "\tDCs");
                                playerCount++;
                            }
                            if (playerCount == turnQueue.size()) {
                                while (deadCards.size() > 0) {
                                    Card cardDC = deadCards.pop();
                                    List<String> players = new ArrayList<>(turnQueue);
                                    Collections.shuffle(players);
                                    broadcast(players.get(0) + "\t" + cardDC.getShortName() + "\tDC");
                                }
                                playerCount = 0;
                            }
                            break;
                            //Draw Cards - All
                        case "DCA":
                            playerCount++;
                            List<Card> cards = deck.draw(Integer.parseInt(data[1]));
                            //Cards
                            try {
                                messageQueue.put(data[0] + " got " + data[1] + " brand new cards.");
                                broadcast(data[0] + ListExtension.cardListToString(cards) + "\t\tDCs");
                                if (playerCount == turnQueue.size()) {
                                    while (deck.hasCards()) {
                                        Card cardDC = deck.draw();
                                        List<String> players = new ArrayList<>(turnQueue);
                                        Collections.shuffle(players);
                                        broadcast(players.get(0) + "\t" + cardDC.getShortName() + "\tDC");
                                    }
                                    playerCount = 0;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                            //Draw Card
                        case "DrC":
                            //Cards
                            while (deck.hasCards()) {
                                Card cardDC = deck.draw();
                                List<String> players = new ArrayList<>(turnQueue);
                                Collections.shuffle(players);
                                broadcast(players.get(0) + "\t" + cardDC.getShortName() + "\tDC");
                            }
                            break;
                        //Warnings
                        case "INVALID-CARDS":
                            broadcast(data[0] + "\t\tINVALID-CARDS");
                            break;
                        case "NOT-YOUR-TURN":
                            broadcast(data[0] + "\t\tNOT-YOUR-TURN");
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + data[2]);
                    }
                }
            } catch (IOException e) {
                try {
                    messageQueue.put("Lost a connection...");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                clientStreams.remove(client);
            } catch (IllegalStateException e) {
                try {
                    messageQueue.put(e.getMessage());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private String getQuitMessage() {
        return quitMessagesList.get(new Random().nextInt(quitMessagesList.size()));
    }

    private void displayBS(String attacker, String defender, int turns) {
        Platform.runLater(() -> {
            String result;
            broadcast("\t\tDB");

            List<Card> filterCards = new ArrayList<>(selectedCards);

            //Remove all of the requested cards.
            filterCards.removeIf(card -> card.getRank() == 1 + (turns / 2) % 13);
            Random random = new Random();
            //Fails Baloney Sandwich
            String selectedMessage;
            if (filterCards.isEmpty()) {
                result = "Failed!";
                String[] possibleFailureComments = {
                        "Would you like a cupcake, %s?",
                        "%s, you should try my sister game, Electric Field Hockey.",
                        "If you can't convince them, confuse them, %s.",
                        "Is it true that your trousers are literally on fire, %s?",
                        "I thought you were great at this game, %s.",
                        "Might as well not call Baloney Sandwich this time, %s.",
                        "Don't feel bad, %s. It's only a game...",
                        "%s, don't give up. It's never too late to make a comeback.",
                        "On the bright side, I brought you a teddy bear, %s.",
                        "Every action has an equal and opposite reaction, %s.",
                        "%s, I suggest you have a pizza party to compensate for your loss.",
                        "%s, you can hug me when you feel stressed.",
                        "%s, sometimes you have to lose the battle to win the war.",
                        "Is that your final answer, %s?",
                        "Please don't call Baloney Sandwich again, %s.",
                        "May I present to you the Darwin Award, %s?",
                        "May I present to you the dumbest decision made, %s?",
                        "I have a bad feeling about this, %s.",
                        "%s, you got some splaining to do!",
                        "Aww, %s... Don't cry, we all make mistakes.",
                        "Aww, %s... Don't cry... you're making me cry. :(",
                        "You've yeed your last haw, %s!",
                        "Aww, %s... now I feel bad for you. :(",
                        "Did you plan to call Baloney Sandwich on yourself, %s? Because it's working...",
                        "Did you really just yeet yourself, %s?",
                        "Well yes, but actually no, %s.",
                        "Well, at least you tried, %s...",
                        "When pigs fly, %s, you will successfully call Baloney Sandwich.",
                        "Hush, little %s, don't you cry...",
                        "It's OK, %s, we all make mistakes.",
                        "Here's your reward for calling too many Baloney Sandwiches, %s.",
                        "Better luck next time, %s...",
                        "You're over-thinking it, %s.",
                        "You might want to think twice before calling Baloney Sandwich too often, %s.",
                        "If Plan A fails, %s, remember that you have 25 more letters.",
                        "Poor %s... at least I have a pretty special gift for you!",
                        "%s... now I feel sad for you... :(",
                        "Really, %s? I thought you're more than this...",
                        (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1 &&
                                Calendar.getInstance().get(Calendar.MONTH) == Calendar.APRIL) ?
                                "Yay, you did it, %s! Oh wait, April Fools!" : "Yay, you did it, %s! Oh wait, nevermind...",
                        "What if I told you, %s, you're wrong?",
                        "Oh, %s, you thought you can get away with that?"
                };
                selectedMessage = String.format(possibleFailureComments[random.nextInt(possibleFailureComments.length)],
                        attacker);
            } else {
                result = "Successful!";

                String[] possibleSuccessComments = {
                        "%s has to draw the cards because of %s.", //Defender, Attacker
                        "%s fell victim to %s.", //Defender, Attacker
                        "%s, how dare you lie to %s!", //Defender, Attacker
                        "Resistance is futile, %s, thanks to %s.", //Defender, Attacker
                        "Look at what you've done to %s, %s!", //Defender, Attacker
                        "What on Earth did you do to %s, %s?", //Defender, Attacker
                        "Here's your reward for calling Baloney Sandwich on %s, %s.", //Defender, Attacker
                        "%s, I suggest you take a break from dealing with %s.", //Defender, Attacker
                        "%s, I suggest you have a party to compensate for %s.", //Defender, Attacker
                        "Poor %s, I think you should stay away from %s.", //Defender, Attacker
                        "%s, did you just get caught red-handed by %s?", //Defender, Attacker
                        "%s, did you think you could get away with hiding your cards from %s?", //Defender, Attacker
                        "%s, this is what the Baloney Sandwich Master %s is doing.", //Defender, Attacker
                        "Thank you, %s! You just made %s draw the cards.", //Attacker, Defender
                        "You might need to upgrade your insurance against %s, %s.",
                        "I think %s has a very special gift for you, %s...",
                        "I blame %s for making %s draw the cards!", //Attacker, Defender
                        "%s, how dare you make %s draw the cards!", //Attacker, Defender
                        "You're about to get yeeted by %s, %s!", //Attacker, Defender
                        "It's so hard trying to keep up with the calls of %s, %s", //Attacker, Defender
                        "Go, %s, you can defeat %s!", //Attacker, Defender
                        "Congratulations, %s, you did the right maneuver on %s!", //Attacker, Defender
                        "Keep it up, %s, show %s the right way to do it!",
                        "Good job, %s, you showed %s the true meaning of Baloney Sandwich!",
                        "%s, how did you know that %s was lying?",
                        "Congratulations, %s, you mopped the floor with %s!",
                        "You might want to hire a lawyer against %s, %s.",
                        "%s has given to you a nice bundle of birthday cards, %s.",
                        "I see that %s might be hitting a bit too hard on %s.",
                        "You've got this, %s, give a nice punch to %s.",
                        "You might want to think twice before\nletting %s call Baloney Sandwich on you, %s." //Attacker, Defender
                };

                int index = random.nextInt(possibleSuccessComments.length);
                if (index < 13) {
                    selectedMessage = String.format(possibleSuccessComments[index], defender, attacker);
                } else {
                    selectedMessage = String.format(possibleSuccessComments[index], attacker, defender);
                }
                selectedMessage = selectedMessage.replaceAll("\n", " ");
            }
            broadcast(attacker + "\t" + defender + "\tBS\t" + result.equals("Successful!")
                    + "\t" + selectedMessage + "\t" + ListExtension.cardListToString(discardPile) + "\t" + ListExtension.cardListToString(selectedCards));
        });

    }

    public class ServerInit implements Runnable {


        private final int port;

        public ServerInit(int port) {
            this.port = port;
        }
        @Override
        public void run() {
            try {
                clientStreams = new ArrayList<>();
                ServerSocket serverSocket = new ServerSocket(port);

                try {
                    messageQueue.put("The server IP is " + InetAddress.getLocalHost().getHostAddress() + " at port " + port + ".");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                    clientStreams.add(writer);

                    new Thread(new ClientThread(clientSocket, writer)).start();
                    try {
                        messageQueue.put("Another client logged in.");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                try {
                    messageQueue.put("Error in making a connection.");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            }
        }
    }

    public void addUser(String user) throws IllegalArgumentException {
        if (users.add(user)) {
            String[] list = new String[users.size()];
            try {
                messageQueue.put("Added " + user + "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            users.toArray(list);
            for (String str : list) {
                broadcast(str + "\t\tC");
            }
            broadcast("Server\t" + user + "\tF");
        }
    }

    public void removeUser(String user) {
        users.remove(user);
        String[] list = new String[users.size()];
        users.toArray(list);
        for (String str : list) {
            broadcast(str + "\t\tD");
        }
        try {
            messageQueue.put("Removed " + user + "");
            broadcast("[Announcement]\t" + user + " has disconnected.\tM");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String... strings) {
        try {
            for (PrintWriter writer : clientStreams) {
                try {
                    String code = strings[0];
                    List<String> whitelistedCodes = Arrays.asList("[Game]", "[Announcement]");
                    if (whitelistedCodes.contains(code)) {
                        messageQueue.put("Sending Message: " + Arrays.toString(strings));
                    }
                    StringBuilder str = new StringBuilder();
                    for (String s : strings) {
                        str.append(s).append("\t");
                    }
                    str.deleteCharAt(str.length() - 1);
                    writer.println(str);
                    writer.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                messageQueue.put("Error Sending to Everyone.");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void broadcast(String str) {
        try {
            for (PrintWriter writer : clientStreams) {
                try {
                    String code = str.split("\t")[0];
                    List<String> whitelistedCodes = Arrays.asList("[Game]", "[Announcement]");
                    if (whitelistedCodes.contains(code)) {
                        messageQueue.put("Sending Message: " + str);
                    }
                    writer.println(str);
                    writer.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                messageQueue.put("Error Sending to Everyone.");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
