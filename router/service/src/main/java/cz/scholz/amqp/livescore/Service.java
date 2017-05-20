package cz.scholz.amqp.livescore;

import cz.scholz.amqp.livescore.service.AddGameListener;
import cz.scholz.amqp.livescore.service.GetScoreListener;
import cz.scholz.amqp.livescore.service.LiveScoreService;
import cz.scholz.amqp.livescore.service.SetScoreListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * Broadcast Receiver
 * Receives broadcasts from the persistent broadcast queue
 */
public class Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    private final InitialContext context;
    private final int timeoutInMillis = 100000000;
    private final LiveScoreService liveScore;

    public Service() throws NamingException {
        // Configura logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss Z");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");

        // TRACE AMQP frames
        System.setProperty("org.slf4j.simpleLogger.log.org.apache.qpid.jms.provider.amqp.FRAMES", "trace");

        // Configure Qpid JMS client properties
        try {
            Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
            properties.setProperty("connectionfactory.connection", String.format(
                    "amqp://%s:%d?jms.forceAsyncSend=true&jms.username=%s&jms.password=%s&jms.presettlePolicy.presettleProducers=true&amqp.idleTimeout=120000&amqp.traceFrames=true",
                    "localhost",
                    5672,
                    "admin",
                    "123456"));
            properties.setProperty("queue.getScore", "/getScore");
            properties.setProperty("queue.setScore", "/setScore");
            properties.setProperty("queue.addGame", "/addGame");
            properties.setProperty("queue.liveScore", "/liveScore");
            this.context = new InitialContext(properties);
        } catch (NamingException ex) {
            LOGGER.error("Unable to proceed with broadcast receiver", ex);
            throw ex;
        }

        // Create LiveScoreService
        liveScore = new LiveScoreService();
    }

    public void run() throws JMSException, NamingException, InterruptedException {
        /*
        * Step 1: Initializing the context based on the properties file we prepared
        */
        Connection connection = null;
        Session broadcastSession = null, requestSession = null, requestSession2 = null, requestSession3 = null;
        MessageConsumer requestConsumer = null, requestConsumer2 = null, requestConsumer3 = null;
        MessageProducer broadcastProducer = null;

        try {
            /*
            * Step 2: Preparing the connection and session
            */
            LOGGER.info("Creating connection");
            connection = ((ConnectionFactory) context.lookup("connection")).createConnection();
            connection.setExceptionListener(new ExceptionListener());

            broadcastSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            requestSession = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            requestSession2 = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            requestSession3 = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            /*
            * Step 3: Creating a consumers / producers
            */
            requestConsumer = requestSession.createConsumer((Destination) context.lookup("getScore"));
            requestConsumer.setMessageListener(new GetScoreListener(liveScore, requestSession));
            requestConsumer2 = requestSession2.createConsumer((Destination) context.lookup("addGame"));
            requestConsumer2.setMessageListener(new AddGameListener(liveScore, requestSession2));
            requestConsumer3 = requestSession3.createConsumer((Destination) context.lookup("setScore"));
            requestConsumer3.setMessageListener(new SetScoreListener(liveScore, requestSession3));

            broadcastProducer = broadcastSession.createProducer((Destination) context.lookup("liveScore"));
            //broadcastProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            liveScore.setLiveScoreProducer(broadcastSession, broadcastProducer);

            /*
            * Step 4: Starting the connection
            */
            connection.start();
            LOGGER.info("Connected");

            /*
            * Step 5: Receiving broadcast messages using listener for timeout seconds
            */
            synchronized (this) {
                this.wait(this.timeoutInMillis);
            }
        } catch (JMSException | NamingException | InterruptedException e) {
            LOGGER.error("Unable to proceed", e);
            throw e;
        } finally {
            /*
            * Step 6: Closing the connection
            */
            if (requestConsumer != null) {
                System.out.println("Closing consumer");
                requestConsumer.close();
            }

            if (requestConsumer2 != null) {
                System.out.println("Closing consumer2");
                requestConsumer2.close();
            }

            if (requestConsumer3 != null) {
                System.out.println("Closing consumer3");
                requestConsumer3.close();
            }

            if (broadcastProducer != null) {
                System.out.println("Closing broadcaster");
                broadcastProducer.close();
            }

            if (requestSession != null) {
                System.out.println("Closing requestSession");
                requestSession.close();
            }

            if (requestSession2 != null) {
                System.out.println("Closing requestSession2");
                requestSession2.close();
            }

            if (requestSession3 != null) {
                System.out.println("Closing requestSession3");
                requestSession3.close();
            }

            if (broadcastSession != null) {
                System.out.println("Closing session");
                broadcastSession.close();
            }

            if (connection != null) {
                // implicitly closes session and producers/consumers
                System.out.println("Closing connection");
                connection.close();
            }
        }
    }

    public static void main(String[] args) throws JMSException, NamingException, InterruptedException {
        Service service = new Service();
        service.run();
    }
}