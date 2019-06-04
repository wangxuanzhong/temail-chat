package com.syswin.temail.usermail.application;


import static com.syswin.temail.usermail.common.Contants.RESULT_CODE.ERROR_REQUEST_PARAM;
import static com.syswin.temail.usermail.common.Contants.SessionEventKey.PACKET_ID_SUFFIX;

import com.google.gson.Gson;
import com.syswin.temail.transactional.TemailShardingTransactional;
import com.syswin.temail.usermail.common.Contants.SessionEventType;
import com.syswin.temail.usermail.common.Contants.TemailArchiveStatus;
import com.syswin.temail.usermail.common.Contants.TemailStatus;
import com.syswin.temail.usermail.common.Contants.TemailType;
import com.syswin.temail.usermail.common.Contants.UsermailAgentEventType;
import com.syswin.temail.usermail.core.IUsermailAdapter;
import com.syswin.temail.usermail.core.dto.CdtpHeaderDto;
import com.syswin.temail.usermail.core.dto.Meta;
import com.syswin.temail.usermail.core.exception.IllegalGMArgsException;
import com.syswin.temail.usermail.core.util.MsgCompressor;
import com.syswin.temail.usermail.core.util.SeqIdFilter;
import com.syswin.temail.usermail.domains.Usermail;
import com.syswin.temail.usermail.domains.UsermailBlacklistRepo;
import com.syswin.temail.usermail.domains.UsermailBox;
import com.syswin.temail.usermail.domains.UsermailBoxRepo;
import com.syswin.temail.usermail.domains.UsermailMsgReplyRepo;
import com.syswin.temail.usermail.domains.UsermailRepo;
import com.syswin.temail.usermail.dto.CreateUsermailDto;
import com.syswin.temail.usermail.dto.DeleteMailBoxQueryDto;
import com.syswin.temail.usermail.dto.MailboxDto;
import com.syswin.temail.usermail.dto.QueryTrashDto;
import com.syswin.temail.usermail.dto.TrashMailDto;
import com.syswin.temail.usermail.dto.UmQueryDto;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class UsermailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UsermailService.class);
  private final UsermailRepo usermailRepo;
  private final UsermailBoxRepo usermailBoxRepo;
  private final UsermailMsgReplyRepo usermailMsgReplyRepo;
  private final IUsermailAdapter usermailAdapter;
  private final UsermailSessionService usermailSessionService;
  private final Usermail2NotfyMqService usermail2NotfyMqService;
  private final UsermailMqService usermailMqService;
  private final MsgCompressor msgCompressor;
  private final ConvertMsgService convertMsgService;


  @Autowired
  public UsermailService(UsermailRepo usermailRepo, UsermailBoxRepo usermailBoxRepo,
      UsermailMsgReplyRepo usermailMsgReplyRepo,
      IUsermailAdapter usermailAdapter,
      UsermailSessionService usermailSessionService, Usermail2NotfyMqService usermail2NotfyMqService,
      UsermailMqService usermailMqService,
      MsgCompressor msgCompressor,
      ConvertMsgService convertMsgService) {
    this.usermailRepo = usermailRepo;
    this.usermailBoxRepo = usermailBoxRepo;
    this.usermailMsgReplyRepo = usermailMsgReplyRepo;
    this.usermailAdapter = usermailAdapter;
    this.usermailSessionService = usermailSessionService;
    this.usermail2NotfyMqService = usermail2NotfyMqService;
    this.usermailMqService = usermailMqService;
    this.msgCompressor = msgCompressor;
    this.convertMsgService = convertMsgService;
  }

  public String saveUsermailBoxInfo(String from, String to, String owner) {
    String sessionId = usermailSessionService.getSessionID(from, to);
    // 保证mail2和owner是相反的，逐渐去掉mail1字段
    String target = owner.equals(from) ? to : from;
    UsermailBox usermailBox = usermailBoxRepo.selectUsermailBox(owner, target);
    if (usermailBox == null) {
      UsermailBox box = new UsermailBox(usermailAdapter.getPkID(), sessionId, target, owner);
      usermailBoxRepo.saveUsermailBox(box);
    }
    return sessionId;
  }

  @TemailShardingTransactional(shardingField = "#owner")
  public Map sendMail(CdtpHeaderDto headerInfo, CreateUsermailDto usermail, String owner, String other) {
    String from = usermail.getFrom();
    String to = usermail.getTo();
    String msgid = usermail.getMsgId();
    String author = usermail.getAuthor();
    List<String> filter = usermail.getFilter();
    String filterStr = null;
    if (filter != null && !filter.isEmpty()) {
      filterStr = String.join(",", filter);
    }
    int attachmentSize = usermail.getAttachmentSize();
    long seqNo = usermailAdapter.getMsgSeqNo(from, to, owner);
    String sessionid = saveUsermailBoxInfo(from, to, owner);
    long pkid = usermailAdapter.getPkID();
    Usermail mail = new Usermail(pkid, usermail.getMsgId(), sessionid,
        from, to, TemailStatus.STATUS_NORMAL_0, usermail.getType(), owner, "",
        seqNo, msgCompressor.zipWithDecode(usermail.getMsgData()), author, filterStr);
    Meta meta = usermail.getMeta();
    if (meta != null) {
      BeanUtils.copyProperties(meta, mail);
    }
    usermailRepo.saveUsermail(mail);
    int eventType = 0;

    switch (usermail.getType()) {
      case TemailType.TYPE_NORMAL_0:
        eventType = SessionEventType.EVENT_TYPE_0;
        break;
      case TemailType.TYPE_DESTORY_AFTER_READ_1:
        eventType = SessionEventType.EVENT_TYPE_17;
        break;
      default:
        eventType = SessionEventType.EVENT_TYPE_51;
        break;
    }
    usermail2NotfyMqService
        .sendMqMsgSaveMail(headerInfo, from, to, owner, msgid, usermail.getMsgData(), seqNo, eventType, attachmentSize,
            author, filter);
    usermailAdapter.setLastMsgId(owner, other, msgid);
    Map<String, Object> result = new HashMap<>();
    result.put("msgId", msgid);
    result.put("seqId", mail.getSeqNo());
    return result;
  }

  @TemailShardingTransactional(shardingField = "#from")
  public List<Usermail> getMails(CdtpHeaderDto headerInfo, String from, String to, long fromSeqNo,
      int pageSize, String filterSeqIds, String signal) {
    UmQueryDto umQueryDto = new UmQueryDto();
    umQueryDto.setFromSeqNo(fromSeqNo);
    umQueryDto.setSignal(signal);
    String sessionid = usermailSessionService.getSessionID(from, to);
    umQueryDto.setSessionid(sessionid);
    umQueryDto.setPageSize(pageSize);
    umQueryDto.setOwner(from);
    List<Usermail> result = convertMsgService.convertMsg(usermailRepo.getUsermail(umQueryDto));
    List<Usermail> resultFilter = new ArrayList<>();
    if (StringUtils.isNotEmpty(filterSeqIds) && !CollectionUtils.isEmpty(result)) {
      boolean isAfter = "after".equals(signal);
      SeqIdFilter filter = new SeqIdFilter(filterSeqIds, isAfter);
      for (int i = 0; i < result.size(); i++) {
        if (filter.filter(result.get(i).getSeqNo())) {
          resultFilter.add(result.get(i));
        }
      }
    } else {
      resultFilter = result;
    }
    return resultFilter;
  }

  @TemailShardingTransactional(shardingField = "#from")
  public void revert(CdtpHeaderDto headerInfo, String from, String to, String msgid) {
    usermailMqService.sendMqRevertMsg(headerInfo.getxPacketId(), headerInfo.getCdtpHeader(), from, to, to, msgid);
    usermailMqService
        .sendMqRevertMsg(headerInfo.getxPacketId() + PACKET_ID_SUFFIX, headerInfo.getCdtpHeader(), from, to, from,
            msgid);
  }

  @TemailShardingTransactional(shardingField = "#owner")
  public void revert(String xPacketId, String cdtpHeader, String from, String to, String owner, String msgid) {
    UmQueryDto umQueryDto = new UmQueryDto();
    umQueryDto.setMsgid(msgid);
    umQueryDto.setStatus(TemailStatus.STATUS_REVERT_1);
    umQueryDto.setOwner(owner);
    int count = usermailRepo.revertUsermail(umQueryDto);
    //判断是否撤回成功，防止通知重复发送
    if (count > 0) {
      usermail2NotfyMqService
          .sendMqUpdateMsg(xPacketId, cdtpHeader, from, to, owner, msgid, SessionEventType.EVENT_TYPE_2);
    } else {
      LOGGER.warn(
          "Message revert failed, xPacketId is {}, cdtpHeader is {}, from is {}, to is {}, msgId is {}, owner is {}",
          xPacketId, cdtpHeader, from, to, msgid, owner);
    }

  }

  @TemailShardingTransactional(shardingField = "#from")
  public List<MailboxDto> mailboxes(CdtpHeaderDto headerInfo, String from, int archiveStatus,
      Map<String, String> usermailBoxes) {
    List<UsermailBox> usermailBox = usermailBoxRepo.getUsermailBoxByOwner(from, archiveStatus);
    List<MailboxDto> resultDto = new ArrayList<>(usermailBox.size());
    List<Usermail> lastUsermail;
    for (int i = 0; i < usermailBox.size(); i++) {
      MailboxDto dto = new MailboxDto();
      UsermailBox box = usermailBox.get(i);
      dto.setTo(box.getMail2());
      dto.setArchiveStatus(box.getArchiveStatus());
      String sessionid = box.getSessionid();
      UmQueryDto umQueryDto = new UmQueryDto();
      umQueryDto.setSessionid(sessionid);
      umQueryDto.setOwner(from);
      if (usermailBoxes != null && usermailBoxes.size() > 0) {
        String to = box.getMail2();
        String msgId = usermailAdapter.getLastMsgId(from, to);
        if (msgId != null && msgId.equals(usermailBoxes.get(to))) {
          //最新的msgId相同，不做处理
          continue;
        }
      }
      lastUsermail = convertMsgService.convertMsg(usermailRepo.getLastUsermail(umQueryDto));
      if (!CollectionUtils.isEmpty(lastUsermail)) {
        dto.setLastMsg(lastUsermail.get(0));
      }
      resultDto.add(dto);
    }
    return resultDto;
  }

  @TemailShardingTransactional(shardingField = "#from")
  public void removeMsg(CdtpHeaderDto headerInfo, String from, String to, List<String> msgIds) {
    // msg 內容更新为空串
    LOGGER.info("Label-delete-usermail-msg: delete msg by msgIds,from is {},to is {},ids is {}", from, to, msgIds);
    usermailRepo.removeMsg(msgIds, from);
    LOGGER.info("Label-delete-usermail-msg: delete reply msg by parentMsgId, owner is {}, parentMsgId is {}", from,
        msgIds);
    usermailMsgReplyRepo.deleteMsgByParentIdAndOwner(from, msgIds);
    usermail2NotfyMqService
        .sendMqAfterUpdateStatus(headerInfo, from, to, new Gson().toJson(msgIds), SessionEventType.EVENT_TYPE_4);

    UmQueryDto umQueryDto = new UmQueryDto();
    umQueryDto.setOwner(from);
    umQueryDto.setSessionid(usermailSessionService.getSessionID(from, to));
    List<Usermail> usermails = usermailRepo.getLastUsermail(umQueryDto);
    if (CollectionUtils.isEmpty(usermails)) {
      usermailAdapter.deleteLastMsgId(from, to);
    } else {
      String lastMsgId = usermailAdapter.getLastMsgId(from, to);
      String newLastMsgId = usermails.get(0).getMsgid();
      if (!newLastMsgId.equals(lastMsgId)) {
        usermailAdapter.setLastMsgId(from, to, newLastMsgId);
      }
    }
  }

  @TemailShardingTransactional(shardingField = "#from")
  public void destroyAfterRead(CdtpHeaderDto headerInfo, String from, String to, String msgId) {
    usermailMqService.sendMqDestroyMsg(headerInfo.getxPacketId(), headerInfo.getCdtpHeader(), from, to, to, msgId);
    usermailMqService
        .sendMqDestroyMsg(headerInfo.getxPacketId() + PACKET_ID_SUFFIX, headerInfo.getCdtpHeader(), from, to, from,
            msgId);
  }

  @TemailShardingTransactional(shardingField = "#owner")
  public void destroyAfterRead(String xPacketId, String cdtpHeader, String from, String to, String owner,
      String msgId) {
    Usermail usermail = usermailRepo.getUsermailByMsgid(msgId, owner);
    //添加消息状态判断，防止通知重发
    if (usermail != null && usermail.getType() == TemailType.TYPE_DESTORY_AFTER_READ_1
        && usermail.getStatus() == TemailStatus.STATUS_NORMAL_0) {
      usermailRepo.destoryAfterRead(owner, msgId, TemailStatus.STATUS_DESTORY_AFTER_READ_2);
      usermail2NotfyMqService
          .sendMqUpdateMsg(xPacketId, cdtpHeader, to, from, owner, msgId, SessionEventType.EVENT_TYPE_3);
    } else {
      LOGGER.warn("destroyAfterRead method illegal param, from is {}, msgId is {}, usermail is {}", from, msgId,
          usermail);
    }
  }

  @TemailShardingTransactional(shardingField = "#queryDto.from")
  public boolean deleteSession(CdtpHeaderDto cdtpHeaderDto, DeleteMailBoxQueryDto queryDto) {
    usermailBoxRepo.deleteByOwnerAndTo(queryDto.getFrom(), queryDto.getTo());
    LOGGER.info("Label-delete-usermail-session: delete session, params is {}", queryDto);
    if (queryDto.isDeleteAllMsg()) {
      String sessionId = usermailSessionService.getSessionID(queryDto.getTo(), queryDto.getFrom());
      usermailRepo
          .batchDeleteBySessionId(sessionId, queryDto.getFrom());
      usermailMsgReplyRepo.batchDeleteBySessionId(sessionId, queryDto.getFrom());
    }
    usermail2NotfyMqService
        .sendMqAfterDeleteSession(cdtpHeaderDto, queryDto.getFrom(), queryDto.getTo(), queryDto.isDeleteAllMsg(),
            SessionEventType.EVENT_TYPE_4);
    usermailAdapter.deleteLastMsgId(queryDto.getFrom(), queryDto.getTo());
    return true;
  }

  @TemailShardingTransactional(shardingField = "#owner")
  public boolean deleteGroupChatSession(String groupTemail, String owner) {
    usermailBoxRepo.deleteByOwnerAndTo(owner, groupTemail);
    LOGGER
        .info("Label-delete-GroupChat-session: delete session, params is owner:{}, groupTemail:{}", owner, groupTemail);
    String sessionId = usermailSessionService.getSessionID(groupTemail, owner);
    usermailRepo.batchDeleteBySessionId(sessionId, owner);
    return true;
  }

  @TemailShardingTransactional(shardingField = "#from")
  public List<Usermail> batchQueryMsgs(CdtpHeaderDto cdtpHeaderDto, String from, String to, List<String> msgIds) {
    List<Usermail> usermailList = usermailRepo.getUsermailByFromToMsgIds(from, msgIds);
    return convertMsgService.convertMsg(usermailList);
  }

  @TemailShardingTransactional(shardingField = "#from")
  public List<Usermail> batchQueryMsgsReplyCount(CdtpHeaderDto cdtpHeaderDto, String from, String to,
      List<String> msgIds) {
    List<Usermail> usermailList = usermailRepo.getUsermailByFromToMsgIds(from, msgIds);
    for (int i = 0; i < usermailList.size(); i++) {
      usermailList.get(i).setMessage(null);
      usermailList.get(i).setZipMsg(null);
    }
    return usermailList;
  }

  @TemailShardingTransactional(shardingField = "#from")
  public void moveMsgToTrash(CdtpHeaderDto headerInfo, String from, String to, List<String> msgIds) {
    usermailRepo.updateStatusByMsgIds(msgIds, from, TemailStatus.STATUS_TRASH_4);
    usermailMsgReplyRepo.batchUpdateByParentMsgIds(from, msgIds, TemailStatus.STATUS_TRASH_4);
    usermail2NotfyMqService.sendMqMoveTrashNotify(headerInfo, from, to, msgIds, SessionEventType.EVENT_TYPE_35);
  }

  @TemailShardingTransactional(shardingField = "#temail")
  public void revertMsgToTrash(CdtpHeaderDto headerInfo, String temail, List<TrashMailDto> trashMails) {
    List<String> msgIds = new ArrayList<>(trashMails.size());
    for (TrashMailDto dto : trashMails) {
      msgIds.add(dto.getMsgId());
    }
    usermailRepo.updateStatusByTemail(trashMails, temail, TemailStatus.STATUS_NORMAL_0);
    usermailMsgReplyRepo.batchUpdateByParentMsgIds(temail, msgIds, TemailStatus.STATUS_NORMAL_0);
    usermail2NotfyMqService.sendMqTrashMsgOpratorNotify(headerInfo, temail, trashMails, SessionEventType.EVENT_TYPE_36);
  }

  @TemailShardingTransactional(shardingField = "#temail")
  public void removeMsgFromTrash(CdtpHeaderDto headerInfo, String temail, List<TrashMailDto> trashMails) {
    usermailMqService.sendMqRemoveTrash(temail, trashMails, UsermailAgentEventType.TRASH_REMOVE_0);
    LOGGER
        .info("Label-delete-usermail-trash: Remove msg from trash, params is temail:{},msginfo:{}", temail, trashMails);
    usermail2NotfyMqService.sendMqTrashMsgOpratorNotify(headerInfo, temail, trashMails, SessionEventType.EVENT_TYPE_37);
  }

  @TemailShardingTransactional(shardingField = "#temail")
  public void removeMsgFromTrash(String temail, List<TrashMailDto> trashMails) {
    List<String> msgIds = new ArrayList<>(trashMails.size());
    for (TrashMailDto dto : trashMails) {
      msgIds.add(dto.getMsgId());
    }
    usermailRepo.removeMsgByStatus(trashMails, temail, TemailStatus.STATUS_TRASH_4);
    LOGGER
        .info("Label-delete-usermail-trash: Mq consumer remove msg from trash, params is temail:{},msginfo:{}", temail,
            trashMails);
    usermailMsgReplyRepo.deleteMsgByParentIdAndOwner(temail, msgIds);
  }

  @TemailShardingTransactional(shardingField = "#temail")
  public void clearMsgFromTrash(String temail) {
    usermailRepo.removeMsgByStatus(null, temail, TemailStatus.STATUS_TRASH_4);
    LOGGER.info("Label-delete-usermail-trash: Mq consumer clear trash, params is temail:{}", temail);
    usermailMsgReplyRepo.batchDeleteByStatus(temail, TemailStatus.STATUS_TRASH_4);
  }

  @TemailShardingTransactional(shardingField = "#temail")
  public List<Usermail> getMsgFromTrash(CdtpHeaderDto headerInfo, String temail, long timestamp, int pageSize,
      String signal) {
    QueryTrashDto queryDto = new QueryTrashDto();
    queryDto.setOwner(temail);
    queryDto.setSignal(signal);
    queryDto.setPageSize(pageSize);
    queryDto.setUpdateTime(new Timestamp(timestamp));
    queryDto.setStatus(TemailStatus.STATUS_TRASH_4);
    List<Usermail> result = usermailRepo.getUsermailByStatus(queryDto);
    return convertMsgService.convertMsg(result);
  }

  @TemailShardingTransactional(shardingField = "#from")
  public void updateUsermailBoxArchiveStatus(CdtpHeaderDto headerInfo, String from, String to, int archiveStatus) {
    LOGGER.info("update usermail archiveStatus , from={},to={},archiveStatus={}", from, to, archiveStatus);
    if ((archiveStatus != TemailArchiveStatus.STATUS_NORMAL_0
        && archiveStatus != TemailArchiveStatus.STATUS_ARCHIVE_1)) {
      throw new IllegalGMArgsException(ERROR_REQUEST_PARAM);
    }
    usermailBoxRepo.updateArchiveStatus(from, to, archiveStatus);
    if (archiveStatus == TemailArchiveStatus.STATUS_NORMAL_0) {
      usermail2NotfyMqService.sendMqAfterUpdateArchiveStatus(headerInfo, from, to, SessionEventType.EVENT_TYPE_34);
    } else if (archiveStatus == TemailArchiveStatus.STATUS_ARCHIVE_1) {
      usermail2NotfyMqService.sendMqAfterUpdateArchiveStatus(headerInfo, from, to, SessionEventType.EVENT_TYPE_33);
    }
  }

}
