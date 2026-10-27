package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 操作日志记录表 oper_log
 *
 * 操作日志实体类，对应数据库表 sys_oper_log。
 * 用于记录系统中所有接口的操作日志，包括操作模块、业务类型（新增/修改/删除/授权/导出等）、
 * 请求方法、请求参数、返回结果、操作人、操作地点、IP地址、执行状态和耗时等信息。
 * 操作日志通过 @OperLog 注解配合切面（OperLogAspect）自动采集，@Async 异步写入。
 * businessTypes 字段用于前端多条件筛选时传递多个业务类型值，不持久化到数据库。
 *
 * @author ruoyi
 */
public class SysOperLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键，数据库自增 */
    @Excel(name = "操作序号", cellType = ColumnType.NUMERIC)
    private Long operId;

    /** 操作模块（Controller 中 @OperLog(title="...") 指定，如"用户管理"）；不可空 */
    @Excel(name = "操作模块")
    private String title;

    /** 业务类型字典值：0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8生成代码 9清空数据；不可空 */
    @Excel(name = "业务类型", readConverterExp = "0=其它,1=新增,2=修改,3=删除,4=授权,5=导出,6=导入,7=强退,8=生成代码,9=清空数据")
    private Integer businessType;

    /** 业务类型数组，多条件筛选时传递多个值；非持久化字段 */
    private Integer[] businessTypes;

    /** 请求方法（Java 方法签名简写，如"com.ruoyi.web.controller.system.SysUserController.addSave"）；不可空 */
    @Excel(name = "请求方法")
    private String method;

    /** 请求方式（HTTP Method：GET/POST/PUT/DELETE 等）；不可空 */
    @Excel(name = "请求方式")
    private String requestMethod;

    /** 操作类别：0其它 1后台用户 2手机端用户；由 LoginUser.getDeviceType 推断 */
    @Excel(name = "操作类别", readConverterExp = "0=其它,1=后台用户,2=手机端用户")
    private Integer operatorType;

    /** 操作人员登录账号；可空（定时任务等无人触发场景） */
    @Excel(name = "操作人员")
    private String operName;

    /** 部门名称，从操作人所属部门获取；可空 */
    @Excel(name = "部门名称")
    private String deptName;

    /** 请求 URL（接口相对路径，如"/system/user"）；不可空 */
    @Excel(name = "请求地址")
    private String operUrl;

    /** 操作 IP 地址；可空 */
    @Excel(name = "操作地址")
    private String operIp;

    /** 操作地点，由 IP 地址库解析得到，如"广东省深圳市"；可空 */
    @Excel(name = "操作地点")
    private String operLocation;

    /** 请求参数（JSON 格式字符串），超长可能被截断；可空 */
    @Excel(name = "请求参数")
    private String operParam;

    /** 返回参数（JSON 格式）；成功时为 JSON 响应体，异常时为错误信息；可空 */
    @Excel(name = "返回参数")
    private String jsonResult;

    /** 操作状态：0=正常（接口正常返回），1=异常（抛出 ServiceException 等）；不可空 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=异常")
    private Integer status;

    /** 错误消息，仅当 status=1 时填充具体异常信息；可空 */
    @Excel(name = "错误消息")
    private String errorMsg;

    /** 操作时间，即接口被调用的时间点；不可空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date operTime;

    /** 接口耗时（毫秒）；可空 */
    @Excel(name = "消耗时间", suffix = "毫秒")
    private Long costTime;

    /**
     * 获取操作日志主键
     *
     * @return 日志记录主键
     */
    public Long getOperId()
    {
        return operId;
    }

    /**
     * 设置操作日志主键
     *
     * @param operId 日志主键
     */
    public void setOperId(Long operId)
    {
        this.operId = operId;
    }

    /**
     * 获取操作模块
     *
     * @return 操作模块中文名
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * 设置操作模块
     *
     * @param title 模块名称
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * 获取业务类型
     *
     * @return 业务类型字典值（0-9）
     */
    public Integer getBusinessType()
    {
        return businessType;
    }

    /**
     * 设置业务类型
     *
     * @param businessType 业务类型编码
     */
    public void setBusinessType(Integer businessType)
    {
        this.businessType = businessType;
    }

    /**
     * 获取业务类型数组
     *
     * @return 业务类型数组（多条件筛选用，非持久化）
     */
    public Integer[] getBusinessTypes()
    {
        return businessTypes;
    }

    /**
     * 设置业务类型数组
     *
     * @param businessTypes 业务类型数组
     */
    public void setBusinessTypes(Integer[] businessTypes)
    {
        this.businessTypes = businessTypes;
    }

    /**
     * 获取请求方法
     *
     * @return 完整类名.方法名
     */
    public String getMethod()
    {
        return method;
    }

    /**
     * 设置请求方法
     *
     * @param method 方法全名
     */
    public void setMethod(String method)
    {
        this.method = method;
    }

    /**
     * 获取请求方式
     *
     * @return HTTP Method：GET/POST/PUT/DELETE 等
     */
    public String getRequestMethod()
    {
        return requestMethod;
    }

    /**
     * 设置请求方式
     *
     * @param requestMethod HTTP Method
     */
    public void setRequestMethod(String requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    /**
     * 获取操作类别
     *
     * @return 0=其它，1=后台用户，2=手机端用户
     */
    public Integer getOperatorType()
    {
        return operatorType;
    }

    /**
     * 设置操作类别
     *
     * @param operatorType 操作类别编码
     */
    public void setOperatorType(Integer operatorType)
    {
        this.operatorType = operatorType;
    }

    /**
     * 获取操作人员
     *
     * @return 操作人登录账号，可空
     */
    public String getOperName()
    {
        return operName;
    }

    /**
     * 设置操作人员
     *
     * @param operName 操作人账号
     */
    public void setOperName(String operName)
    {
        this.operName = operName;
    }

    /**
     * 获取部门名称
     *
     * @return 操作人所属部门，可空
     */
    public String getDeptName()
    {
        return deptName;
    }

    /**
     * 设置部门名称
     *
     * @param deptName 部门名称
     */
    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    /**
     * 获取请求URL
     *
     * @return 接口相对路径
     */
    public String getOperUrl()
    {
        return operUrl;
    }

    /**
     * 设置请求URL
     *
     * @param operUrl 接口路径
     */
    public void setOperUrl(String operUrl)
    {
        this.operUrl = operUrl;
    }

    /**
     * 获取操作IP地址
     *
     * @return 请求来源 IP 地址，可空
     */
    public String getOperIp()
    {
        return operIp;
    }

    /**
     * 设置操作IP地址
     *
     * @param operIp IP 地址
     */
    public void setOperIp(String operIp)
    {
        this.operIp = operIp;
    }

    /**
     * 获取操作地点
     *
     * @return IP 解析后的中文地点，可空
     */
    public String getOperLocation()
    {
        return operLocation;
    }

    /**
     * 设置操作地点
     *
     * @param operLocation 地点描述
     */
    public void setOperLocation(String operLocation)
    {
        this.operLocation = operLocation;
    }

    /**
     * 获取请求参数
     *
     * @return 请求体 JSON 字符串，超长截断，可空
     */
    public String getOperParam()
    {
        return operParam;
    }

    /**
     * 设置请求参数
     *
     * @param operParam 请求参数 JSON
     */
    public void setOperParam(String operParam)
    {
        this.operParam = operParam;
    }

    /**
     * 获取返回参数
     *
     * @return 接口响应 JSON 字符串（成功）或错误信息（失败），可空
     */
    public String getJsonResult()
    {
        return jsonResult;
    }

    /**
     * 设置返回参数
     *
     * @param jsonResult 返回值 JSON
     */
    public void setJsonResult(String jsonResult)
    {
        this.jsonResult = jsonResult;
    }

    /**
     * 获取操作状态
     *
     * @return 0=正常，1=异常
     */
    public Integer getStatus()
    {
        return status;
    }

    /**
     * 设置操作状态
     *
     * @param status 状态编码
     */
    public void setStatus(Integer status)
    {
        this.status = status;
    }

    /**
     * 获取错误消息
     *
     * @return 异常时的错误描述，可空
     */
    public String getErrorMsg()
    {
        return errorMsg;
    }

    /**
     * 设置错误消息
     *
     * @param errorMsg 错误描述
     */
    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    /**
     * 获取操作时间
     *
     * @return 接口调用发生的时间点
     */
    public Date getOperTime()
    {
        return operTime;
    }

    /**
     * 设置操作时间
     *
     * @param operTime 操作发生时间
     */
    public void setOperTime(Date operTime)
    {
        this.operTime = operTime;
    }

    /**
     * 获取耗时（毫秒）
     *
     * @return 接口执行耗时，可空
     */
    public Long getCostTime()
    {
        return costTime;
    }

    /**
     * 设置耗时（毫秒）
     *
     * @param costTime 执行耗时
     */
    public void setCostTime(Long costTime)
    {
        this.costTime = costTime;
    }
}
