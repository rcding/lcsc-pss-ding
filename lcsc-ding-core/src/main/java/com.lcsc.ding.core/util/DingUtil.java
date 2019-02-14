package com.lcsc.ding.core.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceListRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.request.OapiProcessinstanceGetRequest;
import com.dingtalk.api.request.OapiProcessinstanceListidsRequest;
import com.dingtalk.api.response.*;
import com.lcsc.ding.core.constant.Constant;
import com.taobao.api.ApiException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DingUtil {

    // 用户信息map缓存
    private static Map<String, OapiUserGetResponse> userInfoMap = new ConcurrentHashMap<String, OapiUserGetResponse>();

    //时间格式
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 存储user信息
     *
     * @param response
     */
    public static void addUserInfo(OapiUserGetResponse response) {

        if (null == response || null == response.getUserid()) {

            return;
        }

        try {
            if (null == userInfoMap.get(response.getUserid())) {

                FileUtils.writeStringToFile(new File(ResourceUtils.getURL("classpath:").getPath() + Constant.FILE_STORAGE_PATH), JSONObject.toJSONString(response) + System.getProperty("line.separator"), "utf-8", true);
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        userInfoMap.put(response.getUserid(), response);
        System.out.println(userInfoMap);
    }

    public static Set<String> getUserIdList() {

        return userInfoMap.keySet();
    }

    /**
     * 根据userid获取用户信息
     *
     * @param userId
     * @return
     */
    public static OapiUserGetResponse getUserInfo(String userId) {

        // 如果内存中的用户信息为空，那尝试从本地缓存中获取用户信息
        if (MapUtils.isEmpty(userInfoMap)) {

            initUserInfoMap();
        }
        return userInfoMap.get(userId);
    }

    /**
     * 清空内存中的用户信息
     */
    public static void clearUserInfoMap() {

        userInfoMap.clear();
    }

    /**
     * 初始化加载用户信息
     */
    public static void initUserInfoMap() {

        try {

            clearUserInfoMap();
            File file = new File(ResourceUtils.getURL("classpath:").getPath() + Constant.FILE_STORAGE_PATH);

            // 文件存在才需要加载用户信息
            if (file.exists()) {

                List<String> userStrList = FileUtils.readLines(file, "utf-8");
                for (String userStr : userStrList) {

                    OapiUserGetResponse userInfo = JSONObject.parseObject(userStr, OapiUserGetResponse.class);
                    userInfoMap.put(userInfo.getUserid(), userInfo);
                }
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    /**
     * 获取当前用户的userid
     *
     * @return
     */
    public static String getCurrentUserId() {

        // 登录信息存放到session
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest servletRequest = servletRequestAttributes.getRequest();
        Object currentUser = servletRequest.getSession().getAttribute(Constant.USER_SESSION_KEY);
        if (null != currentUser) {

            OapiUserGetResponse userInfo = (OapiUserGetResponse) currentUser;
            return userInfo.getUserid();
        }

        return null;
    }

    /**
     * 推送多个，逗号隔开
     * 文本推送
     */
    public static void push(String userIds, String text) {

        DingTalkClient pushClient = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2");

        OapiMessageCorpconversationAsyncsendV2Request pushRequest = new OapiMessageCorpconversationAsyncsendV2Request();
        pushRequest.setUseridList(userIds);
        pushRequest.setAgentId(Constant.AGENT_ID);
        pushRequest.setToAllUser(Boolean.FALSE);
        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        msg.setMsgtype("text");
        msg.setText(new OapiMessageCorpconversationAsyncsendV2Request.Text());
        msg.getText().setContent(text);
        pushRequest.setMsg(msg);
        try {

            OapiMessageCorpconversationAsyncsendV2Response response = pushClient.execute(pushRequest, AccessTokenUtil.getToken());
        } catch (ApiException e) {

            e.printStackTrace();
        }
    }

    /**
     * 根据审批code及用户获取审批id
     */
    public static List<String> getProcessByCodeAndId(String processCode, String userIds, Date start, Date end) {

        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/listids");
        OapiProcessinstanceListidsRequest req = new OapiProcessinstanceListidsRequest();
        req.setProcessCode(processCode);
        req.setStartTime(start.getTime());
        req.setEndTime(end.getTime());
        req.setSize(10L);
        req.setCursor(0L);
        req.setUseridList(userIds);
        OapiProcessinstanceListidsResponse response = null;
        try {

            response = client.execute(req, AccessTokenUtil.getToken());
            // 解析申请单id
            String jsonString = response.getBody();
            JSONObject result = JSONObject.parseObject(jsonString);
            Integer errcode = result.getInteger("errcode");

            if (0 != errcode) {

                return null;
            }
            JSONArray processIds = result.getJSONObject("result").getJSONArray("list");

            return Arrays.asList(processIds.toJSONString());

        } catch (ApiException e) {

            e.printStackTrace();
        }

        return null;
    }

    /**
     * 根据申请id获取申请详情
     */
    public static OapiProcessinstanceGetResponse.ProcessInstanceTopVo getProcessById(String processId) {

        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/processinstance/get");
        OapiProcessinstanceGetRequest request = new OapiProcessinstanceGetRequest();
        request.setProcessInstanceId(processId);
        OapiProcessinstanceGetResponse response = null;
        OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstanceTopVo = null;

        try {

            response = client.execute(request, AccessTokenUtil.getToken());
            processInstanceTopVo = response.getProcessInstance();

        } catch (ApiException e) {

            e.printStackTrace();
        }

        return processInstanceTopVo;
    }


    /**
     * 根据时间段及用户获取考勤
     */
    public static OapiAttendanceListResponse getAttendanceByUserId(Date beginDate, Date endDate, String userId) {

        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/attendance/list");
        OapiAttendanceListRequest request = new OapiAttendanceListRequest();
        request.setWorkDateFrom(simpleDateFormat.format(beginDate));
        request.setWorkDateTo(simpleDateFormat.format(endDate));
        request.setOffset(0l);
        request.setLimit(50l);
        request.setUserIdList(Arrays.asList(userId));
        OapiAttendanceListResponse response = null;
        try {
            response = client.execute(request, AccessTokenUtil.getToken());

        } catch (ApiException e) {

            e.printStackTrace();
        }

        return response;
    }


    public static void main(String args[]) {
        DateTime dateTime = new DateTime(2018, 12, 1, 0, 0);

        // 当月最后一天
        DateTime lastDay = dateTime.dayOfMonth().withMaximumValue();
        getProcessByCodeAndId(Constant.LEAVE_PROCESS_CODE,"manager4081",dateTime.toDate(),lastDay.toDate() );
        //  getProcessById("45285f52-a5a3-4bc9-9829-e82697cf47b0");
    }

}
