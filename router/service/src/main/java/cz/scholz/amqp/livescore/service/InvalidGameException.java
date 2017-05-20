package cz.scholz.amqp.livescore.service;

/**
 * Created by jakub on 20/05/2017.
 */
public class InvalidGameException extends Exception {
    public InvalidGameException(String message) {
        super(message);
    }
}