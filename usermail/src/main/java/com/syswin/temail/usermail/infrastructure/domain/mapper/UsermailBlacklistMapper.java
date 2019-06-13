package com.syswin.temail.usermail.infrastructure.domain.mapper;

import com.syswin.temail.usermail.domains.UsermailBlacklist;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsermailBlacklistMapper {

  /**
   * @param usermailBlacklist 黑名单信息
   * @description 新增黑名单
   */
  int insert(UsermailBlacklist usermailBlacklist);

  /**
   * @param usermailBlacklist 黑名单信息
   * @description 根据发起者和被拉黑者删除黑名单信息
   */
  int deleteByAddresses(UsermailBlacklist usermailBlacklist);

  /**
   * @param temailAddress 发起者地址
   * @param blackedAddress 被拉黑地址
   * @description 根据发起者和被拉黑者查找黑名单信息
   */
  UsermailBlacklist selectByAddresses(@Param("temailAddress") String temailAddress, @Param("blackedAddress") String blackedAddress);

  /**
   * @param temailAddress 当前地址
   * @description 查找当前地址的黑名单列表
   */
  List<UsermailBlacklist> selectByTemailAddress(String temailAddress);

  /**
   * @param temailAddress 当前地址
   * @param blackedAddress 被拉黑地址
   * @description 判断收件人是否在发件人黑名单(0 : 不在 ; 1 : 在)
   */
  int countByAddresses(@Param("temailAddress") String temailAddress, @Param("blackedAddress") String blackedAddress);

}
