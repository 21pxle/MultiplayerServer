package client;

import javafx.animation.AnimationTimer;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ClientMessageConsumer extends AnimationTimer {
    private final BlockingQueue<String> messageQueue;
    private final TextArea textArea;

    public ClientMessageConsumer(BlockingQueue<String> messageQueue, TextArea textArea) {
        this.messageQueue = messageQueue;
        this.textArea = textArea;
    }

    @Override
    public void handle(long now) {
        List<String> messages = new ArrayList<>();
        messageQueue.drainTo(messages);
        messages.forEach(msg -> textArea.appendText("\n" + msg));
    }
}
