package com.ruoyi.web.controller.common;

import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * 通用文件接口控制器
 * 
 * <p>提供跨业务模块复用的文件上传/下载能力，所有接口均挂载在 {@code /common} 路由前缀下。
 * 本控制器不涉及具体业务实体，仅负责将 {@link MultipartFile} 转存至磁盘并生成访问 URL，
 * 或将磁盘文件以二进制流方式输出到客户端。</p>
 * 
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET  /common/download} — 下载指定文件（基于统一下载目录 + 原文件名）</li>
 *   <li>{@code POST /common/upload}   — 上传单个文件，返回新文件名与完整访问 URL</li>
 *   <li>{@code POST /common/uploads}  — 批量上传多个文件</li>
 *   <li>{@code GET  /common/download/resource} — 按资源路径下载本地文件（数据库存储场景）</li>
 * </ul>
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    /** 服务访问地址配置，用于拼接上传文件的完整 URL（如 http://localhost:8080/profile/upload/xxx） */
    @Autowired
    private ServerConfig serverConfig;

    /** 批量文件返回字段之间的分隔符 */
    private static final String FILE_DELIMITER = ",";

    /**
     * 通用下载请求
     * 
     * <p>GET /common/download?fileName=xxx&delete=true</p>
     * 
     * <p>从 {@link RuoYiConfig#getDownloadPath()} 指定的下载目录读取文件并以二进制流输出。
     * 文件名会被重命名为 {@code 时间戳_原文件名} 以避免浏览器下载时覆盖本地已有文件。</p>
     * 
     * @param fileName 原始文件名（支持带业务前缀，如 {@code 1_报表.xlsx}，会自动去掉首个 {@code _} 之前的部分）
     * @param delete 下载完成后是否删除服务器上的源文件，{@code true} 则删除
     * @param response HTTP 响应，由容器注入
     * @param request HTTP 请求，由容器注入
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete)
            {
                FileUtils.deleteFile(filePath);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     * 
     * <p>POST /common/upload（Content-Type: multipart/form-data，字段名 {@code file}）</p>
     * 
     * <p>响应 data 字段结构：</p>
     * <pre>
     * {
     *   "url":            "http://host/profile/upload/2024/xx.jpg",
     *   "fileName":       "/profile/upload/2024/xx.jpg",   // 存储路径
     *   "newFileName":    "xx.jpg",                        // 上传后的文件名（含时间戳前缀）
     *   "originalFilename": "原图.jpg"                      // 用户本地原始文件名
     * }
     * </pre>
     * 
     * @param file 客户端上传的文件对象（MultipartFile）
     * @return AjaxResult.success() 包含访问 URL 与各类文件名；失败时返回 {@link AjaxResult#error(String)}
     * @throws Exception 文件读写异常
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     * 
     * <p>POST /common/uploads（Content-Type: multipart/form-data，字段名 {@code files}）</p>
     * 
     * <p>批量上传多个文件，响应字段与单文件接口一致，但以逗号 {@code ,} 分隔：</p>
     * <pre>
     * {
     *   "urls":            "http://...1,http://...2",
     *   "fileNames":       "/profile/upload/1,/profile/upload/2",
     *   "newFileNames":    "a.jpg,b.jpg",
     *   "originalFilenames": "原图1.jpg,原图2.jpg"
     * }
     * </pre>
     * 
     * @param files 客户端上传的多个文件对象
     * @return AjaxResult.success() 包含逗号分隔的各类文件信息
     * @throws Exception 文件读写异常
     */
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files)
            {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMITER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMITER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMITER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMITER));
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     * 
     * <p>GET /common/download/resource?resource=/profile/upload/2024/xx.jpg</p>
     * 
     * <p>适用于数据库存储相对路径场景。内部会将 {@link RuoYiConfig#getProfile()}
     * (profile 绝对目录) 与前端传入的 {@code resource} 拼接为真实磁盘路径，
     * 再以原始文件名输出。</p>
     * 
     * @param resource 数据库中保存的资源访问路径，如 {@code /profile/upload/xxx.jpg}
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象，用于写入二进制流
     * @throws Exception 文件读取异常
     */
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            if (!FileUtils.checkAllowDownload(resource))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = RuoYiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + FileUtils.stripPrefix(resource);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }
}
