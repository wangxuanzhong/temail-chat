package com.syswin.temail.usermail.infrastructure.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.syswin.temail.usermail.common.Constants.TemailStatus;
import com.syswin.temail.usermail.common.Constants.TemailType;
import com.syswin.temail.usermail.core.util.MsgCompressor;
import com.syswin.temail.usermail.domains.Usermail;
import com.syswin.temail.usermail.dto.QueryTrashDTO;
import com.syswin.temail.usermail.dto.TrashMailDTO;
import com.syswin.temail.usermail.dto.UmQueryDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
public class UsermailMapperTest {

  private static final String SESSIONID = "sessionid-1";
  private static Logger logger = LoggerFactory.getLogger(UsermailMapperTest.class);
  @Autowired
  private UsermailRepo usermailRepo;
  private String msgid;
  private MsgCompressor msgCompressor = new MsgCompressor();
  private static Set<Long> existIds = new HashSet<>();

  @Before
  public void before() {
    msgid = "syswin-" + UUID.randomUUID().toString();
  }

  @Test
  public void saveUsermail() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    String from = "from@syswin.com";
    String to = "to@syswin.com";
    userMail.setId(101L);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(from);
    userMail.setMsgid(msgid);
    userMail.setSeqNo(11);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_NORMAL_0);
    userMail.setMessage("");
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);
    long lastSeqno = 0;
    UmQueryDTO umQueryDto = new UmQueryDTO();
    umQueryDto.setPageSize(2);
    umQueryDto.setFromSeqNo(lastSeqno);
    umQueryDto.setSessionid(SESSIONID);
    umQueryDto.setOwner(from);
    List<Usermail> usermail = usermailRepo.getUsermail(umQueryDto);
    Assert.assertNotNull(usermail);
    Assert.assertTrue(usermail.size() >= 1);
  }

  @Test
  public void getLastUsermail() {
    UmQueryDTO umQueryDto = new UmQueryDTO();
    umQueryDto.setSessionid("123456789");
    umQueryDto.setOwner("to@syswin.com");
    List<Usermail> usermails = usermailRepo.getLastUsermail(umQueryDto);
    Assert.assertEquals(0, usermails.size());
  }

  @Test
  public void removeMsg() {
    List<String> msgIds = new ArrayList<>();
    msgIds.add("ldfk");
    msgIds.add("syswin-87532219-9c8a-41d6-976d-eaa805a145c1-1533886884707");
    int count = usermailRepo.removeMsg(msgIds, "from@syswin.com");
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void getUsermailByMsgid() {
    Usermail usermailByMsgid = usermailRepo
        .getUsermailByMsgid("syswin-87532219-9c8a-41d6-976d-eaa805a145c1-1533886884707", "from@syswin.com");
    Assert.assertNull(usermailByMsgid);
  }

  @Test
  public void destoryAfterRead() {
    usermailRepo.destoryAfterRead("from@syswin.com", "syswin-87532219-9c8a-41d6-976d-eaa805a145c1-1533886884707",
        TemailStatus.STATUS_DESTORY_AFTER_READ_2);
  }

  @Test
  public void getUsermail() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    String from = "from@syswin.com";
    String to = "to@syswin.com";
    userMail.setId(102L);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(from);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMsgid(msgid);
    userMail.setSeqNo(11);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_NORMAL_0);
    userMail.setMessage("");
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);

    UmQueryDTO umQueryDto = new UmQueryDTO();
    umQueryDto.setSessionid(SESSIONID);
    umQueryDto.setOwner("from@syswin.com");
    umQueryDto.setPageSize(10);
    List<Usermail> usermails = usermailRepo.getUsermail(umQueryDto);
    assertThat(usermails.get(0).getFrom()).isEqualTo(from);
  }

  @Test
  public void getUsermailWithSeqId() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    String from = "from@syswin.com";
    String to = "to@syswin.com";
    userMail.setId(100L);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(from);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMsgid(msgid);
    userMail.setSeqNo(11);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_NORMAL_0);
    userMail.setMessage("");
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);

    UmQueryDTO umQueryDto = new UmQueryDTO();
    umQueryDto.setSessionid(SESSIONID);
    umQueryDto.setOwner(from);
    umQueryDto.setPageSize(10);
    umQueryDto.setFromSeqNo(10);
    List<Usermail> usermails = usermailRepo.getUsermail(umQueryDto);
    assertThat(usermails.get(0).getFrom()).isEqualTo(from);
  }

  @Test
  public void batchDeleteBySessionId() {
    int count = usermailRepo.batchDeleteBySessionId("", "alice@temail.com");
    assertThat(count).isEqualTo(0);
  }

  @Test
  public void getUsermailListByMsgid() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    long id = this.generatePKid();
    String from = id + "from@syswin.com";
    String to = id + "to@syswin.com";
    String owner = from;
    int status = TemailStatus.STATUS_NORMAL_0;
    String msgid = UUID.randomUUID().toString();
    userMail.setId(id);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(owner);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("");
    userMail.setMsgid(msgid);
    userMail.setSeqNo(0);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(status);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);
    List<Usermail> usermails = usermailRepo.getUsermailListByMsgid(msgid);

    assertThat(usermails).isNotEmpty();
    assertThat(usermails.size()).isOne();
  }

  @Test
  public void getUsermailByFromToMsgIds() {
    List<String> msgIds = new ArrayList<>();
    msgIds.add("AA");
    msgIds.add("BB");
    List<Usermail> mail = usermailRepo.getUsermailByFromToMsgIds("a@systoontest.com", msgIds);
    Assert.assertEquals(0, mail.size());
  }

  @Test
  public void shouldUpdateReplyCountAndLastReplyMsgid() {
    long seqNoBefor = System.currentTimeMillis();
    String msgId = UUID.randomUUID().toString();
    String lastReplyMsgid = UUID.randomUUID().toString();
    //新建消息
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    String from = "from@syswin.com";
    String to = "to@syswin.com";
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(from);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("asdasd");
    userMail.setMsgid(msgId);
    userMail.setSeqNo(seqNoBefor);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_NORMAL_0);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);
    //更新消息seqNo
    usermailRepo.updateReplyCountAndLastReplyMsgid(msgId, from, 1, lastReplyMsgid);
    //验证最新回复消息id与消息回复总数（1）是否正常更新
    Usermail usermailUpdated = usermailRepo.getUsermailByMsgid(msgId, from);
    Assert.assertEquals(usermailUpdated.getLastReplyMsgId(), lastReplyMsgid);
    Assert.assertTrue(usermailUpdated.getReplyCount() == 1);
  }

  @Test
  public void updateStatusByMsgIdsTest() {
    long seqNoBefor = System.currentTimeMillis();
    String msgId = UUID.randomUUID().toString();
    String lastReplyMsgid = UUID.randomUUID().toString();
    //新建消息
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    String from = "from@syswin.com";
    String to = "to@syswin.com";
    userMail.setId(seqNoBefor);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(from);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("");
    userMail.setMsgid(msgId);
    userMail.setSeqNo(seqNoBefor);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_NORMAL_0);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);

    List<String> msgIds = new ArrayList<>();
    msgIds.add(msgId);
    int count = usermailRepo.updateStatusByMsgIds(msgIds, from, TemailStatus.STATUS_TRASH_4);
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void removeMsgByStatus() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    long id = this.generatePKid();
    String from = id + "from@syswin.com";
    String to = id + "to@syswin.com";
    String owner = from;
    int status = TemailStatus.STATUS_NORMAL_0;
    String msgid = UUID.randomUUID().toString();
    userMail.setId(id);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(owner);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("");
    userMail.setMsgid(msgid);
    userMail.setSeqNo(0);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(status);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);

    TrashMailDTO trashMails = new TrashMailDTO();
    trashMails.setFrom(from);
    trashMails.setTo(to);
    trashMails.setMsgId(msgid);

    int count = usermailRepo.removeMsgByStatus(Arrays.asList(trashMails), owner, status);

    assertThat(count).isOne();
  }

  @Test
  public void updateStatusByTemail() {
    // 还原废纸篓消息
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    long id = this.generatePKid();
    String from = id + "from@syswin.com";
    String to = id + "to@syswin.com";
    String owner = from;
    String msgid = UUID.randomUUID().toString();
    userMail.setId(id);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(owner);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("");
    userMail.setMsgid(msgid);
    userMail.setSeqNo(0);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_TRASH_4);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);

    TrashMailDTO trashMails = new TrashMailDTO();
    trashMails.setFrom(from);
    trashMails.setTo(to);
    trashMails.setMsgId(msgid);

    int count = usermailRepo.updateStatusByTemail(Arrays.asList(trashMails), owner, TemailStatus.STATUS_NORMAL_0);

    assertThat(count).isOne();
  }

  @Test
  public void getUsermailByStatus() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    long id = this.generatePKid();
    String from = id + "from@syswin.com";
    String to = id + "to@syswin.com";
    String owner = from;
    String msgid = UUID.randomUUID().toString();
    int status = TemailStatus.STATUS_NORMAL_0;
    userMail.setId(id);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(owner);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("");
    userMail.setMsgid(msgid);
    userMail.setSeqNo(0);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(status);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);

    QueryTrashDTO queryTrashDTO = new QueryTrashDTO();
    queryTrashDTO.setOwner(from);
    queryTrashDTO.setStatus(status);
    queryTrashDTO.setPageSize(2);

    List<Usermail> usermailByStatus = usermailRepo.getUsermailByStatus(queryTrashDTO);

    assertThat(usermailByStatus).isNotEmpty();
    assertThat(usermailByStatus.size()).isOne();
  }

  @Test
  public void shouldRevertUsermail() {
    Usermail userMail = new Usermail();
    userMail.setSessionid(SESSIONID);
    String from = "from@syswin.com";
    String to = "to@syswin.com";
    userMail.setId(123451516);
    userMail.setFrom(from);
    userMail.setTo(to);
    userMail.setOwner(from);
    userMail.setZipMsg(msgCompressor.zip("test message".getBytes()));
    userMail.setMessage("");
    userMail.setMsgid("3a2s1asd1c1a5s");
    userMail.setSeqNo(12);
    userMail.setType(TemailType.TYPE_NORMAL_0);
    userMail.setStatus(TemailStatus.STATUS_NORMAL_0);
    userMail.setAuthor(from);
    userMail.setFilter(null);
    usermailRepo.saveUsermail(userMail);
    UmQueryDTO umQueryDto = new UmQueryDTO();
    umQueryDto.setStatus(TemailStatus.STATUS_REVERT_1);
    umQueryDto.setOwner(from);
    umQueryDto.setMsgid("3a2s1asd1c1a5s");
    int count = usermailRepo.revertUsermail(umQueryDto);
    Assert.assertEquals(1, count);
  }

  private long generatePKid() {
    boolean isUnique;
    long id;
    do {
      id = new Random().nextInt(100);
      isUnique = existIds.add(id);
    } while (!isUnique);
    return id;
  }

  private void insertRecord() {

  }
}