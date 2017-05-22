package cz.scholz.demo.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.proton.*;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by schojak on 16.12.16.
 */
public class AMQPServer {
    final static private Logger LOG = LoggerFactory.getLogger(AMQPServer.class);
    final private ProtonServer server;
    private Map<String, Handler<Message>> handlers = new HashMap<>();
    private Map<String, List<ProtonSender>> multicasts = new HashMap<>();
    private Map<String, ProtonSender> responders = new HashMap<>();

    public AMQPServer(Vertx vertx, int port) {
        server = ProtonServer.create(vertx, new ProtonServerOptions().setPort(port)).connectHandler(this::connectionReceivedHandler).listen();
    }

    public AMQPServer handleRequests(String address, Handler<Message> handler) {
        handlers.put(address, handler);
        return this;
    }

    public AMQPServer registerBroadcastPoint(String address) {
        multicasts.put(address, new LinkedList<ProtonSender>());
        return this;
    }

    public void broadcast(String address, Message msg) {
        if (multicasts.containsKey(address)) {
            multicasts.get(address).forEach(sender -> {
                if (sender.isOpen()) {
                    sender.send(msg);
                }
            });
        }
    }

    public void respond(String address, Message msg) {
        if (responders.containsKey(address)) {
            responders.get(address).send(msg);
        }
    }

    private void connectionReceivedHandler(ProtonConnection pc) {
        LOG.info("AMQPServer: Connection request received");
        pc.openHandler(this::connectionOpenHandler);
        pc.closeHandler(this::connectionCloseHandler);
        pc.receiverOpenHandler(this::connectionReceiverOpenHandler);
        pc.senderOpenHandler(this::connectionSenderOpenHandler);
        pc.sessionOpenHandler(this::connectionSessionOpenHandler);
        pc.disconnectHandler(this::disconnectHandler);
        //pc.open();
    }

    private void disconnectHandler(ProtonConnection protonConnection) {
        LOG.info("AMQPServer: Connection disconnected");
        protonConnection.disconnect();
    }

    private void connectionSessionOpenHandler(ProtonSession protonSession) {
        LOG.info("AMQPServer: Session open received on connection level");
        protonSession.closeHandler(this::sessionCloseHandler);
        protonSession.openHandler(this::sessionOpenHandler);
        protonSession.open();
    }

    private void sessionOpenHandler(AsyncResult<ProtonSession> protonSessionAsyncResult) {
        LOG.info("AMQPServer: Session open received");
        protonSessionAsyncResult.result().open();
    }

    private void sessionCloseHandler(AsyncResult<ProtonSession> protonSessionAsyncResult) {
        LOG.info("AMQPServer: Session close received");
        protonSessionAsyncResult.result().close();
    }

    private void connectionReceiverOpenHandler(ProtonReceiver protonReceiver) {
        if (handlers.containsKey(protonReceiver.getRemoteTarget().getAddress())) {
            protonReceiver.closeHandler(this::receiverCloseHandler);
            protonReceiver.openHandler(this::receiverOpenHandler);
            protonReceiver.handler((del, msg) -> {
                LOG.info("AMQPServer: Receiver received a message");
                handlers.get(protonReceiver.getRemoteTarget().getAddress()).handle(msg);
                del.disposition(new Accepted(), true).settle();
            });
            protonReceiver.setSource(protonReceiver.getRemoteSource());
            protonReceiver.setTarget(protonReceiver.getRemoteTarget());
            protonReceiver.open();
            LOG.info("AMQPServer: Receiver received from " + protonReceiver.getSource() + " to " + protonReceiver.getTarget());
        }
        else {
            protonReceiver.setCondition(new ErrorCondition(AmqpError.NOT_FOUND, "Node " + protonReceiver.getRemoteTarget().getAddress() + " not found"));
            protonReceiver.open();
            protonReceiver.close();
            LOG.info("AMQPServer: Declined receiver from " + protonReceiver.getSource() + " to " + protonReceiver.getTarget());
        }
    }

    /*private void receiverMessage(ProtonDelivery protonDelivery, Message message) {
        LOG.info("AMQPServer: Receiver received a message: ", ((AmqpValue)message.getBody()).getValue().toString());
        protonDelivery.disposition(new Accepted(), true).settle();
    }*/

    private void receiverOpenHandler(AsyncResult<ProtonReceiver> protonReceiverAsyncResult) {
        LOG.info("AMQPServer: Receiver open received");
        protonReceiverAsyncResult.result().open();
    }

    private void receiverCloseHandler(AsyncResult<ProtonReceiver> protonReceiverAsyncResult) {
        LOG.info("AMQPServer: Receiver close received");
        protonReceiverAsyncResult.result().close();
    }

    private void connectionSenderOpenHandler(ProtonSender protonSender) {
        if (multicasts.containsKey(protonSender.getRemoteSource().getAddress())) {
            protonSender.closeHandler(this::senderCloseHandler);
            protonSender.openHandler(this::senderOpenHandler);
            protonSender.setSource(protonSender.getRemoteSource());
            protonSender.setTarget(protonSender.getRemoteTarget());
            protonSender.open();

            LOG.info("AMQPServer: Sender received: " + protonSender.getRemoteSource().getAddress() + " " + protonSender.getRemoteTarget().getAddress());
            LOG.info("AMQPServer: " + protonSender.getRemoteSource().getAddress() + " is multicast endpoint");
            multicasts.get(protonSender.getRemoteSource().getAddress()).add(protonSender);
        }
        else if (!protonSender.getRemoteSource().getAddress().startsWith("/") && !responders.containsKey(protonSender.getRemoteSource().getAddress())) {
           if (responders.containsKey(protonSender.getRemoteSource().getAddress())) {
               protonSender.setCondition(new ErrorCondition(AmqpError.RESOURCE_LOCKED, "Node " + protonSender.getRemoteSource().getAddress() + " is already in exclusive use"));
               protonSender.open();
               protonSender.close();
               LOG.info("AMQPServer: Declined receiver from " + protonSender.getRemoteSource().getAddress() + " to " + protonSender.getRemoteSource().getAddress() + " - already taken");
           }
           else {
               protonSender.closeHandler(this::senderCloseHandler);
               protonSender.openHandler(this::senderOpenHandler);
               protonSender.setSource(protonSender.getRemoteSource());
               protonSender.setTarget(protonSender.getRemoteTarget());
               protonSender.open();

               LOG.info("AMQPServer: Sender received: " + protonSender.getRemoteSource().getAddress() + " " + protonSender.getRemoteTarget().getAddress());
               LOG.info("AMQPServer: " + protonSender.getRemoteSource().getAddress() + " is response endpoint");
               responders.put(protonSender.getRemoteSource().getAddress(), protonSender);
           }
        }
        else {
            protonSender.setCondition(new ErrorCondition(AmqpError.NOT_ALLOWED, "Node " + protonSender.getRemoteSource().getAddress() + " not allowed"));
            protonSender.open();
            protonSender.close();
            LOG.info("AMQPServer: Declined receiver from " + protonSender.getRemoteSource().getAddress() + " to " + protonSender.getRemoteSource().getAddress() + " - not allowed");
        }
    }

    private void senderCloseHandler(AsyncResult<ProtonSender> protonSenderAsyncResult) {
        LOG.info("AMQPServer: Sender close received");
        protonSenderAsyncResult.result().close();
    }

    private void senderOpenHandler(AsyncResult<ProtonSender> protonSenderAsyncResult) {
        LOG.info("AMQPServer: Sender open received");
        protonSenderAsyncResult.result().open();
    }

    private void connectionCloseHandler(AsyncResult<ProtonConnection> protonConnectionAsyncResult) {
        LOG.info("AMQPServer: Connection close received");
        protonConnectionAsyncResult.result().close();
    }

    private void connectionOpenHandler(AsyncResult<ProtonConnection> protonConnectionAsyncResult) {
        LOG.info("AMQPServer: Connection open received");
        protonConnectionAsyncResult.result().open();
    }
}
