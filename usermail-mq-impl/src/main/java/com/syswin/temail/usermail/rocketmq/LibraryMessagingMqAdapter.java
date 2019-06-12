package com.syswin.temail.usermail.rocketmq;

import com.syswin.library.messaging.MessagingException;
import com.syswin.library.messaging.MqProducer;
import com.syswin.temail.usermail.core.IMqAdapter;
import com.syswin.temail.usermail.core.exception.UserMailException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class LibraryMessagingMqAdapter implements IMqAdapter {

  private final Logger LOGGER = LoggerFactory.getLogger(LibraryMessagingMqAdapter.class);
  private Map<String, MqProducer> rocketMqProducers;
  @Autowired
  private RocketMqProperties rocketMQProperties;

  public LibraryMessagingMqAdapter(Map<String, MqProducer> rocketMqProducers) {
    this.rocketMqProducers = rocketMqProducers;
  }

  @Override
  public void init() {

  }

  @Override
  public void destroy() {

  }

  @Override
  public boolean sendMessage(String topic, String tag, String message) {
    MqProducer mqProducer = rocketMqProducers.get(rocketMQProperties.getProducerGroup());
    if (mqProducer == null) {
      LOGGER.debug("no mq producer!");
      return false;
    }
    try {
      mqProducer.send(message, topic, tag, null);
    } catch (UnsupportedEncodingException | InterruptedException | MessagingException e) {
      LOGGER.error("send message error", e);
      throw new UserMailException(e);
    }
    return true;
  }

}
