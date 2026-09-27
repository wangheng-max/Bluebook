package com.wangheng.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import java.io.InputStream;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



/**
 * 阿里云 OSS 上传工具。
 * 注意：字段必须是非 static 且带 setter，@ConfigurationProperties 才能把
 * application-*.yml 中 aliyun.oss.* 的配置绑定进来（static 字段 Spring 不做绑定）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliOssUtil {

    /** Endpoint，如 https://oss-cn-beijing.aliyuncs.com */
    private String endpoint;
    /** 访问密钥 ID */
    private String accessKeyId;
    /** 访问密钥 Secret */
    private String accessKeySecret;
    /** Bucket 名称 */
    private String bucketName;

    /**
     * 上传文件到 OSS，返回访问 URL：https://{bucket}.{endpoint域名}/{objectName}
     * 上传失败时抛出 OSS 异常，由 GlobalExceptionHandler 统一返回错误提示
     */
    public String uploadFile(String objectName, InputStream in) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 不设置 ContentType 时 OSS 存为 application/octet-stream，
            // Safari/iOS 播放视频、部分浏览器渲染图片会被拒，需按扩展名显式指定
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(resolveContentType(objectName));
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, in, metadata);
            ossClient.putObject(putObjectRequest);
            //url组成: https://bucket名称.区域节点/objectName
            return "https://" + bucketName + "." + endpoint.substring(endpoint.lastIndexOf("/") + 1) + "/" + objectName;
        } finally {
            ossClient.shutdown();
        }
    }

    /** 按扩展名映射浏览器可识别的 MIME 类型，未知类型保持 application/octet-stream */
    private String resolveContentType(String objectName) {
        String name = objectName.toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1);
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "ogg":
            case "ogv":
                return "video/ogg";
            case "mov":
                return "video/quicktime";
            default:
                return "application/octet-stream";
        }
    }
}
