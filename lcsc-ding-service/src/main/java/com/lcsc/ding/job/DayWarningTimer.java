package com.lcsc.ding.job;

import com.dingtalk.api.response.OapiAttendanceListResponse;
import com.dingtalk.api.response.OapiProcessinstanceGetResponse;
import com.lcsc.ding.core.constant.Constant;
import com.lcsc.ding.core.util.DingUtil;
import com.lcsc.ding.core.util.HolidayUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;


@SpringBootApplication
@EnableScheduling
public class DayWarningTimer {

    //时间格式
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

    SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "0 30 9 * * *")
    public void signInTimer() {

        try {

            boolean workDay = new HolidayUtil().isWorkDay(null);

            // 是工作日就开始核查打卡信息
            if (workDay) {

                Set<String> userIdList = DingUtil.getUserIdList();

                Date now = new Date();

                for (String userId : userIdList) {

                    OapiAttendanceListResponse attendance = DingUtil.getAttendanceByUserId(now, now, userId);
                    List<OapiAttendanceListResponse.Recordresult> recordresult = attendance.getRecordresult();

                    if (CollectionUtils.isEmpty(recordresult)) {

                        DingUtil.push(userId, "尊敬的用户你好，今日你忘记打上班卡了哦，请及时补卡");
                    }
                }

            }
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }

    @Scheduled(cron = "0 15 18 * * *")
    public void signOutTimer() {

        try {


            boolean workDay = new HolidayUtil().isWorkDay(null);

            // 是工作日就开始核查打卡信息
            if (workDay) {

                Set<String> userIdList = DingUtil.getUserIdList();

                Date now = new Date();

                for (String userId : userIdList) {

                    OapiAttendanceListResponse attendance = DingUtil.getAttendanceByUserId(now, now, userId);
                    List<OapiAttendanceListResponse.Recordresult> recordresult = attendance.getRecordresult();

                    Boolean offDuty = false;

                    if (CollectionUtils.isNotEmpty(recordresult)) {

                        for (OapiAttendanceListResponse.Recordresult record : recordresult) {

                            if (Constant.CHECKTYPE_OFFDUTY.equals(record.getCheckType()) && Constant.TIMERESULT_NORMAL.equals(record.getTimeResult())
                                    && StringUtils.isBlank(record.getProcInstId())) {

                                offDuty = true;

                                break;
                            }
                        }
                    }

                    if (!offDuty) {

                        DingUtil.push(userId, "尊敬的用户你好，今日你忘记打下班卡了哦，请及时补卡");
                    }
                }

            }
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }


    @Scheduled(cron = "0 25 9 * * *")
    //@Scheduled(cron = "*/5 * * * * ?")
    public void sumbitSubsidyTimer() {

        try {


            String onTime = "09:00:00";


            String offTime = "22:00:00";

            Date userCheckTime = null;


            Date today = new DateTime().toDate();

            //前一天
            Date yesterday = new DateTime().minusDays(1).toDate();

            Date checkTime = simpleDateFormat3.parse(simpleDateFormat2.format(yesterday) + " " + offTime);

            Boolean yesterdayIsWork = new HolidayUtil().isWorkDay(yesterday);

            Boolean todayIsWork = new HolidayUtil().isWorkDay(today);

            // 是工作日就开始核查打卡信息
            if (yesterdayIsWork) {

                Set<String> userIdList = DingUtil.getUserIdList();

                for (String userId : userIdList) {

                    // 是否有提交交通补贴
                    Boolean subsidyFlag = true;


                    // 是否有免扣款
                    Boolean deductFlag = false;


                    OapiAttendanceListResponse yesterdayAttendance = DingUtil.getAttendanceByUserId(yesterday, yesterday, userId);

                    List<OapiAttendanceListResponse.Recordresult> yesterdayRecordresult = yesterdayAttendance.getRecordresult();

                    for (OapiAttendanceListResponse.Recordresult recordresult1 : yesterdayRecordresult) {


                        if (Constant.CHECKTYPE_OFFDUTY.equals(recordresult1.getCheckType()) && Constant.TIMERESULT_NORMAL.equals(recordresult1.getTimeResult())) {

                            userCheckTime = recordresult1.getUserCheckTime();

                            // 是否在十点钟之后
                            if (userCheckTime.after(checkTime)) {

                                if (todayIsWork) {
                                    //今天是否迟到并且没有提交迟到免扣款
                                    OapiAttendanceListResponse todayAttendance = DingUtil.getAttendanceByUserId(today, today, userId);

                                    List<OapiAttendanceListResponse.Recordresult> todayRecordresult = todayAttendance.getRecordresult();

                                    if (CollectionUtils.isNotEmpty(todayRecordresult)) {

                                        OapiAttendanceListResponse.Recordresult recordresult = todayRecordresult.get(0);

                                        // 今天迟到了  并且今天没有提交免扣款
                                        if (simpleDateFormat.parse(simpleDateFormat.format(recordresult.getUserCheckTime())).after(simpleDateFormat.parse(onTime))) {


                                            //有没有迟到免扣的申请
                                            List<String> processIds = DingUtil.getProcessByCodeAndId(Constant.LATE_PROCESS_CODE, userId, recordresult.getUserCheckTime(), today);

                                            if (CollectionUtils.isEmpty(processIds)) {
                                                //没有提交免扣款
                                                deductFlag = true;
                                            }

                                        }

                                    }
                                }

                                // 判断是否有提交交通补贴
                                List<String> processIds = DingUtil.getProcessByCodeAndId(Constant.SUBSIDY_PROCESS_CODE, userId, yesterday, today);

                                if (CollectionUtils.isEmpty(processIds)) {

                                    subsidyFlag = false;
                                } else {


                                    for (String process : processIds) {
                                        // 查询对应的审批
                                        OapiProcessinstanceGetResponse.ProcessInstanceTopVo processInstanceTopVo = DingUtil.getProcessById(process);

                                        if (processInstanceTopVo != null) {

                                            List<OapiProcessinstanceGetResponse.FormComponentValueVo> formComponentValues = processInstanceTopVo.getFormComponentValues();

                                            //加班时间  yyyy-MM-dd HH:mm
                                            OapiProcessinstanceGetResponse.FormComponentValueVo date = formComponentValues.get(1);
                                            String dateString = date.getValue();

                                            //在下班之后有交通补贴审批 则已经提交了
                                            if (simpleDateFormat1.parse(dateString).after(userCheckTime)) {

                                                subsidyFlag = true;
                                                break;
                                            }


                                        }

                                    }
                                }


                            }

                        }

                    }

                    if (!subsidyFlag) {

                        DingUtil.push(userId, "您昨晚辛苦加班到十点之后，若有打车，别忘了填交通补贴单哦!");
                    }

                    if (deductFlag) {

                        DingUtil.push(userId, "您今天迟到了，别忘了提交迟到免扣款哦!");
                    }

                }

            }
        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }
}
