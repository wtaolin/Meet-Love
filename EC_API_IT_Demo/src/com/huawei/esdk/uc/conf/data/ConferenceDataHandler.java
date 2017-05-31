package com.huawei.esdk.uc.conf.data;

import android.text.TextUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.huawei.conference.CtcMemberEntity;
import com.huawei.conference.entity.CtcEntity;
import com.huawei.contacts.ContactLogic;
import com.huawei.dao.impl.ConferenceDao;
import com.huawei.dao.impl.ConferenceMemberDao;
import com.huawei.data.entity.ConferenceEntity;
import com.huawei.data.entity.ConferenceMemberEntity;
import com.huawei.data.entity.People;
import com.huawei.esdk.uc.CommonUtil;
import com.huawei.utils.StringUtil;

public class ConferenceDataHandler
{

    private static ConferenceDataHandler ins = new ConferenceDataHandler();

    List<Map<String,List<ConferenceMemberEntity>>> bookMemberMap = new ArrayList<Map<String, List<ConferenceMemberEntity>>>();

    public static final int SORT_ASCEND = 1;

    public static final int SORT_DESCEND = 2;

    private List<ConferenceEntity> conferenceCreatedList = null;

    private List<ConferenceEntity> conferenceToAttendList = null;

    private List<ConferenceEntity> conferenceInProgressList = null;

    private List<ConferenceEntity> conferenceEndedList = null;

    private ConferenceDataHandler()
    {
        conferenceEndedList = new LinkedList<ConferenceEntity>();
        conferenceCreatedList = new LinkedList<ConferenceEntity>();
        conferenceToAttendList = new LinkedList<ConferenceEntity>();
        conferenceInProgressList = new LinkedList<ConferenceEntity>();
    }

    public static ConferenceDataHandler getIns()
    {
        return ins;
    }

    public List<ConferenceEntity> getAllConf()
    {
        List<ConferenceEntity> list = getConfUnEnded();

        list.addAll(getConfEnded());

        return list;
    }

    public List<ConferenceEntity> getConfUnEnded()
    {
        List<ConferenceEntity> list = new ArrayList<ConferenceEntity>();

        list.addAll(getConfInPro());
        list.addAll(getConfCreated());
        list.addAll(getConfToAttend());

        return list;
    }

    public List<ConferenceEntity> getConfEnded()
    {
        return conferenceEndedList;
    }

    public List<ConferenceEntity> getConfCreated()
    {
        return conferenceCreatedList;
    }

    public List<ConferenceEntity> getConfInPro()
    {
        return conferenceInProgressList;
    }

    public List<ConferenceEntity> getConfToAttend()
    {
        return conferenceToAttendList;
    }

    public void updateAllConf(List<CtcEntity> rspList)
    {
        setAllConfToEnd();

        if(null == rspList)
        {
            return;
        }

        updateConfList(rspList);
    }

    /**
     * 内存中会议全部添加到已结束会议里
     */
    private void setAllConfToEnd()
    {
        List<ConferenceEntity> confList = getConfUnEnded();

        conferenceCreatedList.clear();
        conferenceToAttendList.clear();
        conferenceInProgressList.clear();

        for (int i = 0; i < confList.size(); i++)
        {
            addConfEnded(confList.get(i));
        }
    }


    /**
     * 用来更新会议列表用的
     * @param rspList:请求获得的会议列表 list:内存中列表
     *        说明：用list中会议和rList中比较，匹配到则更新list中该想会议信息并把rList里的移除
     *        ，没有匹配到则把该会议添加到已结束列表中并删除list中的
     */
    public List<ConferenceEntity> updateConfList(List<CtcEntity> rspList)
    {
        if (rspList == null)
        {
            return null;
        }

        CtcEntity ctc = null;
        ConferenceEntity conf = null;
        String confId;

        List<ConferenceEntity> confList = new ArrayList<ConferenceEntity>();

        for (int i = 0; i < rspList.size(); i++)
        {
            ctc = rspList.get(i);
            confId = ctc.getConfId();

            if (TextUtils.isEmpty(confId))
            {
                continue;
            }
//
            conf = delConference(confId);

            // 处理会议列表，返回的时间类型是格式化的String时间
            ctc.setBeginTime(ctc.getBeginTime() + "000");
            ctc.setEndTime(ctc.getEndTime() + "000");
            if (conf != null)
            {
                // 匹配到，删除end里的，添加到对应状态列表里
                if (ConferenceEntity.STATUS_IN_PROGRESS == ctc.getCtcStatus())
                {
                    addConfInPro(conf);
                }
                else if (ConferenceEntity.STATUS_TO_ATTEND == ctc
                        .getCtcStatus())
                {
                    addConfToAttend(conf);
                }
            }
            else
            {
                addServerConfToLocal(ctc, CommonUtil.NORMAL_TIME_TYPE);
            }
             conf = transToConference(ctc, CommonUtil.NORMAL_TIME_TYPE);
             if(conf != null)
             {
             confList.add(conf);
             }
        }
        return confList;
    }

    // 更新与会人信息
    public void updateConfMember(List<CtcMemberEntity> mList,
            ConferenceEntity conf)
    {
        if (null == mList || mList.isEmpty() || null == conf)
        {
            return;
        }

        String confId = conf.getConfId();

        List<ConferenceMemberEntity> list = conf.getConfMemberList();

        saveConfMember(list, mList, confId);
        conf.sortMemberList();
    }

    private void saveConfMember(List<ConferenceMemberEntity> confM,
            List<CtcMemberEntity> mList, String confId)
    {
        ConferenceMemberEntity tempMem = null;

        for (CtcMemberEntity member : mList)
        {
            if (member == null)
            {
                continue;
            }

            tempMem = transToConfMember(member);
            tempMem.setConfId(confId);
            saveConfMember(tempMem, confM);
        }
    }

    /**
     * 根据与会人账号，号码存储与会人信息
     * @param confM:MAA返回的与会人
     * @param mList:内存中与会人列表, 不能为null，否则没意义
     */
    private void saveConfMember(ConferenceMemberEntity confM,
            List<ConferenceMemberEntity> mList)
    {
        // 推送来的与会人的状态
        int status = confM.getStatus();
        // 推送来的与会人账号
        String eNum = confM.getConfMemEspaceNumber();
        // 推送来的与会人号码
        String num = confM.getNumber();
        int role = confM.getRole();
        // String displayName = confM.getDisplayName();
        String newDisplayName = confM.getOriginDisplayName();
        boolean isSelectSite = confM.isSelectSiteEnable();
        String domain = confM.getDomain();

        if (TextUtils.isEmpty(num))
        {
            return;
        }

        // A .全匹配（账号+号码）
        for (ConferenceMemberEntity mem : mList)
        {
            // 号码相同
            if (num.equals(mem.getNumber()))
            {
                // 推送与会人的账号和本地与会人的账号都为null 或者 账号相同，则更新状态
                if ((TextUtils.isEmpty(eNum) && TextUtils.isEmpty(mem.getConfMemEspaceNumber()))
                        || (!TextUtils.isEmpty(eNum) && eNum.equals(mem
                                .getConfMemEspaceNumber())))
                {
                    // 更新状态
                    mem.setStatus(status);
                    mem.setRole(role);
                    mem.setSelectSiteEnable(isSelectSite);
                    mem.setDomain(domain);

                    if (!TextUtils.isEmpty(newDisplayName)
                            && !newDisplayName.equals(mem
                                    .getOriginDisplayName()))
                    {
                        ConferenceMemberDao.modifyConfMem(mem,
                                ConferenceMemberDao.TYPE_NAME, newDisplayName);
                        mem.setDisplayName(newDisplayName);
                    }

                    return;
                }
            }
        }

        // B .号码匹配：有账号的推送与无账号的本地 || 无账号的推送与有账号的本地
        boolean isMatch = false;

        for (ConferenceMemberEntity mem : mList)
        {
            // 号码相同
            if (num.equals(mem.getNumber()))
            {
                if (TextUtils.isEmpty(eNum)
                        ^ TextUtils.isEmpty(mem.getConfMemEspaceNumber()))
                {
                    mem.setStatus(status);
                    mem.setRole(role);
                    mem.setSelectSiteEnable(isSelectSite);
                    mem.setDomain(domain);

                    // 赋账号
                    if (TextUtils.isEmpty(mem.getConfMemEspaceNumber()))
                    {
                        // 本地无账号，给本地补充账号数据
                        ConferenceMemberDao.modifyConfMem(mem,
                                ConferenceMemberDao.TYPE_ESPACE_NUMBER, eNum);
                        mem.setConfMemEspaceNumber(eNum);
                    }

                    if (!TextUtils.isEmpty(newDisplayName)
                            && !newDisplayName.equals(mem
                                    .getOriginDisplayName()))
                    {
                        ConferenceMemberDao.modifyConfMem(mem,
                                ConferenceMemberDao.TYPE_NAME, newDisplayName);
                        mem.setDisplayName(newDisplayName);
                    }

                    isMatch = true;
                }
            }
        }

        if (isMatch)
        {
            return;
        }

        // C .对于本地与推送消息 账号相同，号码不同的;
        for (ConferenceMemberEntity mem : mList)
        {
            // 账号相同
            if (!TextUtils.isEmpty(eNum)
                    && eNum.equals(mem.getConfMemEspaceNumber()))
            {
                // 推送的离会，忽略此状态
                if (status == CtcMemberEntity.STATUS_LEAVE_CONF_SUCCESS)
                {
                    return;
                }

                if (mem.getStatus() == CtcMemberEntity.STATUS_LEAVE_CONF_SUCCESS)
                {
                    // 本地离会，改变本地号码
                    ConferenceMemberDao.modifyConfMem(mem,
                            ConferenceMemberDao.TYPE_NUMBER, num);
                    mem.setStatus(status);
                    mem.setRole(role);
                    mem.setNumber(num);
                    mem.setSelectSiteEnable(isSelectSite);
                    mem.setDomain(domain);

                    if (!TextUtils.isEmpty(newDisplayName)
                            && !newDisplayName.equals(mem
                                    .getOriginDisplayName()))
                    {
                        ConferenceMemberDao.modifyConfMem(mem,
                                ConferenceMemberDao.TYPE_NAME, newDisplayName);
                        mem.setDisplayName(newDisplayName);
                    }

                    return;
                }
                else
                {
                    mList.add(confM);
                    addConfMemDb(mem);
                    return;
                }
            }
        }

        // D .以上条件都不满足，直接保存

        mList.add(confM);
        addConfMemDb(confM);
    }

    // 添加会议
    public void addConfToDb(ConferenceEntity conf)
    {
        if (null == conf)
        {
            return;
        }

        ConferenceDao.addConf(conf);
        ConferenceDao.saveConfMembers(conf, conf.getConfMemberList());

        // TODO 删除超出限制会议
    }

    public void delConfFromDb(String confId)
    {
        ConferenceDao.deleteConference(confId);
        ConferenceMemberDao.deleteConfMember(confId);
    }

    public void addConfMemDb(ConferenceMemberEntity confM)
    {
        ConferenceMemberDao.addConfMember(confM);
    }
    /**
     * 此方法会把数据从内存和数据库中同时删除
     * @param confId 会议ID
     * @return 返回被删除的会议对象
     */
    public ConferenceEntity delConference(String confId)
    {
        // if (isDelDb)
        // {
//        delConfFromDb(confId);
        // }

        ConferenceEntity conf = delConference(confId,
                ConferenceEntity.STATUS_IN_PROGRESS);

        if (null != conf)
        {
            return conf;
        }

        conf = delConference(confId, ConferenceEntity.STATUS_CREATED);

        if (null != conf)
        {
            return conf;
        }

        conf = delConference(confId, ConferenceEntity.STATUS_TO_ATTEND);

        if (null != conf)
        {
            return conf;
        }

        conf = delConference(confId, ConferenceEntity.STATUS_END);

        return conf;
    }
    /**
     * 此方法会把数据从内存和数据库中同时删除
     * @param confId 会议ID
     * @return 返回被删除的会议对象
     */
    public ConferenceEntity delConference(String confId, boolean isDelDb)
    {
         if (isDelDb)
         {
            delConfFromDb(confId);
         }

        ConferenceEntity conf = delConference(confId,
                ConferenceEntity.STATUS_IN_PROGRESS);

        if (null != conf)
        {
            return conf;
        }

        conf = delConference(confId, ConferenceEntity.STATUS_CREATED);

        if (null != conf)
        {
            return conf;
        }

        conf = delConference(confId, ConferenceEntity.STATUS_TO_ATTEND);

        if (null != conf)
        {
            return conf;
        }

        conf = delConference(confId, ConferenceEntity.STATUS_END);

        return conf;
    }

    /**
     * 此方法只从内存中删数据，不会从数据库中删除
     * @param confId
     * @param status
     * @return
     */
    public ConferenceEntity delConference(String confId, int status)
    {
        List<ConferenceEntity> list;

        switch (status)
        {
            case ConferenceEntity.STATUS_CREATED:

                list = getConfCreated();
                break;
            case ConferenceEntity.STATUS_TO_ATTEND:

                list = getConfToAttend();
                break;
            case ConferenceEntity.STATUS_END:

                list = getConfEnded();
                break;
            case ConferenceEntity.STATUS_IN_PROGRESS:
            default:

                list = getConfInPro();
                break;
        }

        if (TextUtils.isEmpty(confId))
        {
            return null;
        }

        return delConfFromList(list, confId);
    }

    private ConferenceEntity delConfFromList(List<ConferenceEntity> list,
            String confId)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (confId.equals(list.get(i).getConfId()))
            {
                return list.remove(i);
            }
        }

        return null;
    }

     /**
     * 请求会议列表响应更新本地会议
     * @param conf
     * @param ctc
     * @return
     */
     private ConferenceEntity updateConfBaseInfo(ConferenceEntity conf,
     CtcEntity ctc, int timeType)
     {
     if (!TextUtils.isEmpty(ctc.getSubject()))
     {
     conf.setSubject(ctc.getSubject());
     }

     if (0 != ctc.getMediaType())
     {
     conf.setMediaType(ctc.getMediaType());
     }

     if (!TextUtils.isEmpty(ctc.getHostNumber()))
     {
     conf.setHost(ctc.getHostNumber());
     }

     // 只有在会议状态是未开始的情况下才更改会场的主持人账号信息
     if (!TextUtils.isEmpty(ctc.getEmcee())
     && ctc.getCtcStatus() == ConferenceEntity.STATUS_TO_ATTEND)
     {
     conf.setHostAccount(ctc.getEmcee());
     }

     if (ConferenceEntity.STATUS_TO_ATTEND == ctc.getCtcStatus())
     {
     if (!TextUtils.isEmpty(ctc.getBeginTime()))
     {
     conf.setBeginTime(CommonUtil.getTimestamp(ctc.getBeginTime(),
     timeType, ctc.getTimezone()));
     }
     if (!TextUtils.isEmpty(ctc.getEndTime()))
     {
     conf.setEndTime(CommonUtil.getTimestamp(ctc.getEndTime(),
     timeType, ctc.getTimezone()));
     }
     }

     conf.setRtpOrSipEncrypt(ctc.isRtpOrSipEncrypt());
     conf.setAccessCode(ctc.getAccessCode());
     conf.setOuterAccessCode(ctc.getOuterAccessCode());
     conf.setConfType(ctc.getConfType());

     // 预约会议推送通知中不带有这个字段 以免覆盖已有的
     if (!TextUtils.isEmpty(ctc.getPassCode()))
     {
     conf.setPassCode(ctc.getPassCode());
     }

     conf.setHostPassword(ctc.getChairmanPwd());
     conf.setMemberPassword(ctc.getMemberPwd());

     // ConferenceDao.modifyConf(conf);

     return conf;
     }

    private boolean matchConf(ConferenceEntity conf)
    {
        if (conf == null)
        {
            return false;
        }

        ConferenceEntity localConf = getConference(conf.getConfId());

        return (null != localConf);
    }

    public List<ConferenceMemberEntity> transToConfMember(
            List<CtcMemberEntity> list)
    {
        if (list == null)
        {
            return null;
        }

        List<ConferenceMemberEntity> lm = new ArrayList<ConferenceMemberEntity>();
        ConferenceMemberEntity mem = null;

        for (CtcMemberEntity cm : list)
        {
            mem = transToConfMember(cm);
            lm.add(mem);
        }

        return lm;
    }

    public ConferenceMemberEntity transToConfMember(CtcMemberEntity ctcM)
    {
        People p = new People(ctcM.getCtcEspaceNumber(), null, null);
        ConferenceMemberEntity confM = new ConferenceMemberEntity(p,
                ctcM.getDispalyName(), ctcM.getNumber());
        confM.setAccount(ctcM.getAccount());
        confM.setConfId(ctcM.getConfId());
        confM.setStatus(ctcM.getMemberStatus() == 0 ? CtcMemberEntity.STATUS_LEAVE_CONF_SUCCESS
                : ctcM.getMemberStatus());// 默认为离会
        confM.setEmail(ctcM.getEmail());
        confM.setRole(ctcM.getRole());
        confM.setDomain(ctcM.getDomain());
        boolean isSelectSite = ctcM.getUserMediaType() == CtcMemberEntity.USER_MEDIATYPE_AUDIO_AND_VIDEO;
        confM.setSelectSiteEnable(isSelectSite);
        return confM;
    }

    public List<CtcMemberEntity> transToCtcMember(
            List<ConferenceMemberEntity> list)
    {
        List<CtcMemberEntity> lm = new ArrayList<CtcMemberEntity>();
        CtcMemberEntity mem = null;

        for (ConferenceMemberEntity cm : list)
        {
            mem = transToCtcMember(cm);
            lm.add(mem);
        }

        return lm;
    }

    public CtcMemberEntity transToCtcMember(ConferenceMemberEntity cm)
    {
        CtcMemberEntity mem = new CtcMemberEntity();
        mem.setAccount(cm.getAccount());
        mem.setCtcEspaceNumber(cm.getConfMemEspaceNumber());
        mem.setNumber(cm.getNumber());
        // mem.setDispalyName(cm.getDisplayName());
        // TODO 此处有问题，解决单子的问题，不传nickname就好，传name或者nativename，不要搞帐号
        mem.setDispalyName(cm.getAccount());
        mem.setEmail(cm.getEmail());
        mem.setConfId(cm.getConfId());
        mem.setRole(cm.getRole());
        return mem;
    }

    public ConferenceEntity transToConference(CtcEntity ctc, int timeType)
    {
        ConferenceEntity conf = new ConferenceEntity();
        conf.setConfId(ctc.getConfId());
        conf.setSubject(ctc.getSubject());
        conf.setHost(ctc.getHostNumber());
        conf.setHostAccount(ctc.getEmcee());
        conf.setState(ctc.getCtcStatus());
        conf.setType(ctc.getType());
        conf.setConfType(ctc.getConfType());
        conf.setIsFiltered(ctc.getIsFiltered());

        // TODO 此处还缺少 mute lock record 暂时不用

        if (0 != ctc.getMediaType())
        {
            // 此字段 CtcEntity默认值为0，而ConferenceEntity给的默认值为1，sdk给的0就不做保存
            conf.setMediaType(ctc.getMediaType());
        }

        conf.setRtpOrSipEncrypt(ctc.isRtpOrSipEncrypt());
        conf.setAccessCode(ctc.getAccessCode());
        conf.setOuterAccessCode(ctc.getOuterAccessCode());
        conf.setPassCode(ctc.getPassCode());
        conf.setHostPassword(ctc.getChairmanPwd());
        conf.setMemberPassword(ctc.getMemberPwd());

        conf.setBeginTime(CommonUtil.getTimestamp(ctc.getBeginTime(), timeType,
                ctc.getTimezone()));
        conf.setEndTime(CommonUtil.getTimestamp(ctc.getEndTime(), timeType,
                ctc.getTimezone()));
        conf.setConfMemberList(transToConfMember(ctc.getParticates()));
        return conf;
    }

    private ConferenceEntity addServerConfToLocal(CtcEntity ctc, int timeType)
    {
        ConferenceEntity result = transToConference(ctc, timeType);

        if (ConferenceEntity.STATUS_IN_PROGRESS == result.getState())
        {
            addConfInPro(result);
        }
        else if (ConferenceEntity.STATUS_TO_ATTEND == result.getState())
        {
            addConfToAttend(result);
        }

        addConfToDb(result);

        return result;
    }

    /**
     * 注：rsp前缀代表从服务器来的 ； local前缀代表是本地的 会议全量信息推送过来以后用来更新会议信息的
     */
    public void updateConference(CtcEntity ctc)
    {
        if (null == ctc || TextUtils.isEmpty(ctc.getConfId()))
        {
            return;
        }

        ConferenceEntity local = getConference(ctc.getConfId());

        // 判断是否与会人信息中有主持人
        List<CtcMemberEntity> ctcMemberEntities = ctc.getParticates();

        boolean isNewHost = false;

        if (ctcMemberEntities != null)
        {
            for (CtcMemberEntity mem : ctcMemberEntities)
            {
                if (mem != null
                        && mem.getRole() == ConferenceMemberEntity.ROLE_PRESIDER)
                {
                    ctc.setEmcee(mem.getCtcEspaceNumber());
                    ctc.setHostNumber(mem.getNumber());
                    isNewHost = true;
                    break;
                }
            }
        }

        if (null == local)
        {
            local = addServerConfToLocal(ctc, CommonUtil.MILLISECOND_TIME_TYPE);
        }
        else
        {
            // 如果与会人中有主持人，无论传来什么信息，都替换
            if (isNewHost
                    && (!local.getHostAccount().equals(ctc.getEmcee()) || !local
                            .getHost().equals(ctc.getHostNumber())))
            {
                deleteOldHost(local);
                local.setHostAccount(ctc.getEmcee());
                local.setHost(ctc.getHostNumber());
            }

            updateConfInfo(local, ctc);

            if (local.getState() != ctc.getCtcStatus())
            {
                delConference(local.getConfId(), local.getState());
                addConfToList(local, ctc.getCtcStatus());
            }
        }

        updateConfMember(ctc.getParticates(), local);

        local.setFullInfo(true);
    }

    /**
     * 全量信息推送更新本地会议
     * @param conf
     * @param ctc
     * @return
     */
    private void updateConfInfo(ConferenceEntity conf, CtcEntity ctc)
    {
        if (!TextUtils.isEmpty(ctc.getSubject()))
        {
            conf.setSubject(ctc.getSubject());
        }

        if (0 != ctc.getMediaType())
        {
            conf.setMediaType(ctc.getMediaType());
        }

        if (!TextUtils.isEmpty(ctc.getEmcee()))
        {
            conf.setHostAccount(ctc.getEmcee());
        }

        if (!TextUtils.isEmpty(ctc.getHostNumber()))
        {
            conf.setHost(ctc.getHostNumber());
        }

        ConferenceDao.modifyConf(conf);
    }

    private ConferenceMemberEntity deleteOldHost(ConferenceEntity conf)
    {
        List<ConferenceMemberEntity> list = conf.getConfMemberList();
        ConferenceMemberEntity oldHost = null;

        for (int i = 0; i < list.size(); i++)
        {
            oldHost = list.get(i);

            if (oldHost != null
                    && oldHost.getRole() == ConferenceMemberEntity.ROLE_PRESIDER)
            {
                oldHost.setRole(ConferenceMemberEntity.ROLE_MEMBER);
                return oldHost;
            }
        }
        return null;
    }

    public ConferenceEntity getConference(String confId)
    {
        if (confId == null)
        {
            return null;
        }

        ConferenceEntity conf = getConfInPro(confId);

        if (null == conf)
        {
            conf = getConfCreated(confId);
        }

        if (null == conf)
        {
            conf = getConfToAttend(confId);
        }

        if (null == conf)
        {
            conf = getConfEnded(confId);
        }

        return conf;
    }

    public ConferenceEntity getConfInPro(String confId)
    {
        return getConfFromList(conferenceInProgressList, confId);
    }

    private ConferenceEntity getConfCreated(String confId)
    {
        return getConfFromList(conferenceCreatedList, confId);
    }

    private ConferenceEntity getConfToAttend(String confId)
    {
        return getConfFromList(conferenceToAttendList, confId);
    }

    private ConferenceEntity getConfEnded(String confId)
    {
        return getConfFromList(conferenceEndedList, confId);
    }

    private ConferenceEntity getConfFromList(List<ConferenceEntity> list,
            String confId)
    {
        if (list == null || confId == null)
        {
            return null;
        }

        for (ConferenceEntity conf : list)
        {
            if (confId.equals(conf.getConfId()))
            {
                return conf;
            }
        }

        return null;
    }

    public void addConfCreated(ConferenceEntity conf)
    {
        addConfToList(conf, ConferenceEntity.STATUS_CREATED);
    }

    public void addConfEnded(ConferenceEntity conf)
    {
        addConfToList(conf, ConferenceEntity.STATUS_END);
    }

    public void addConfInPro(ConferenceEntity conf)
    {
        addConfToList(conf, ConferenceEntity.STATUS_IN_PROGRESS);
    }

    public void addConfToAttend(ConferenceEntity conf)
    {
        addConfToList(conf, ConferenceEntity.STATUS_TO_ATTEND);
    }

    public void addConfToList(ConferenceEntity conf, int confType)
    {
        if (null == conf || matchConf(conf))
        {
            return;
        }

        int sort = 0;

        List<ConferenceEntity> list = null;

        switch (confType)
        {
            case ConferenceEntity.STATUS_CREATED:
                sort = SORT_ASCEND;
                list = conferenceCreatedList;
                break;
            case ConferenceEntity.STATUS_TO_ATTEND:
                sort = SORT_DESCEND;
                list = conferenceToAttendList;
                break;
            case ConferenceEntity.STATUS_IN_PROGRESS:
                sort = SORT_ASCEND;
                // startTimer();
                list = conferenceInProgressList;
                break;
            case ConferenceEntity.STATUS_END:
                sort = SORT_ASCEND;
                // cancelTimer();
                list = conferenceEndedList;
                break;
            default:
                break;
        }

        conf.setState(confType);

        addConfWithSort(list, conf, sort);
    }

    private void addConfWithSort(List<ConferenceEntity> list,
            ConferenceEntity conf, int sortType)
    {
        Timestamp btime = conf.getBeginTime();

        if (btime == null)
        {
            list.add(conf);
            return;
        }

        Timestamp tempTime;
        int sort;

        if (SORT_ASCEND == sortType)
        {
            sort = 1;
        }
        else
        {
            sort = -1;
        }

        for (int i = 0; i < list.size(); i++)
        {
            tempTime = list.get(i).getBeginTime();

            if (sort * btime.compareTo(tempTime) >= 0)
            {
                list.add(i, conf);
                return;
            }
        }

        list.add(conf);
    }

    //初始化会议列表
    public void initConfList()
    {
        List<ConferenceEntity> list = getAllConf();

        // 避免多次重复初始化
        if (!list.isEmpty())
        {
            return;
        }

        if (ContactLogic.getIns().getAbility().isCtcFlag())
        {
            List<ConferenceEntity> ctcList = ConferenceDao
                    .queryConf(ConferenceEntity.TYPE_INSTANT);

            if (null != ctcList)
            {
                list.addAll(ctcList);
            }
        }

        if (ContactLogic.getIns().getAbility().isMediaX())
        {
            List<ConferenceEntity> bookList = ConferenceDao
                    .queryConf(ConferenceEntity.TYPE_BOOKING);

            if (null != bookList)
            {
                list.addAll(bookList);
            }
        }

        for (int i = 0; i < list.size(); i++)
        {
            addConfEnded(list.get(i));
        }
    }

    /**
     *与会者信息从CtcMemberEntity转为ConferenceMemberEntity，将预约会议会议的confId和与confId对应的与会者信息存入HashMap中
     * by lwx302895
     * @param confId
     * @param mList
     */
    public void saveBookConfMember(String confId, List<CtcMemberEntity> mList)
    {
        ConferenceMemberEntity tempMem = null;
        List<ConferenceMemberEntity> confMemberList = new ArrayList<ConferenceMemberEntity>();
        for (CtcMemberEntity member : mList)
        {
            if (member == null)
            {
                continue;
            }
            tempMem = transToConfMember(member);
            tempMem.setConfId(confId);
            confMemberList.add(tempMem);
        }
        Map<String, List<ConferenceMemberEntity>> map = new HashMap<String, List<ConferenceMemberEntity>>();
        map.put(confId,confMemberList);
        bookMemberMap.add(map);
    }

    /**
     *预约会议取出与confId对应的与会者信息，在跳转时添加与会者信息即可
     * by lwx302895
     * @param confId
     * @return
     */
    public List<ConferenceMemberEntity> getBookMemberList(String confId)
    {
        for (int i=0; i < bookMemberMap.size() ;i++)
        {
            Map<String, List<ConferenceMemberEntity>> map= bookMemberMap.get(i);

            Iterator<Map.Entry<String, List<ConferenceMemberEntity>>> it = map.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<String, List<ConferenceMemberEntity>> entry = it.next();
                if (confId.equals(entry.getKey()))
                {
                    return  entry.getValue();
                }
            }
        }
        return null;
    }

}
