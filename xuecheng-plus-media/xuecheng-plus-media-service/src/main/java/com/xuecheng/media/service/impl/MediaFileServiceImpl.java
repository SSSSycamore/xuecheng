package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    MediaFileService currentProxy;

    //普通文件桶
    @Value("${minio.bucket.files}")
    private String bucket_Files;

    @Value("${minio.bucket.videofiles}")
    private String bucket_VideoFiles;

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    //获取文件默认存储目录路径 年/月/日
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private String getMimeType(String extension) {
        if (extension == null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * @param localFilePath 文件地址
     * @param bucket        桶
     * @param objectName    对象名称
     * @return void
     * @description 将文件写入minIO
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(testbucket);
            log.info("上传文件到minio成功,bucket:{},objectName:{}", bucket, objectName);
            System.out.println("上传成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}", bucket, objectName, e.getMessage(), e);
            XueChengPlusException.cast("上传文件到文件系统失败");
        }
        return false;
    }

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket              桶
     * @param objectName          对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @description 将文件信息添加到文件表
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //从数据库查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //拷贝基本信息
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            //保存文件信息到文件表
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                log.error("保存文件信息到数据库失败,{}", mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
            }
            log.info("保存文件信息到数据库成功,{}", mediaFiles.toString());
            addWaitingTask(mediaFiles);
            log.info("添加等待任务成功,{}", mediaFiles.toString());
        }
        return mediaFiles;
    }

    private void addWaitingTask(MediaFiles mediaFiles) {
        String filename = mediaFiles.getFilename();
        String extName = filename.substring(filename.lastIndexOf("."));
        if (getMimeType(extName).equals("video/x-msvideo")) {
            //如果是avi文件，添加转码任务
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess,"url");
            mediaProcess.setStatus("1"); // 设置状态为等待处理
            mediaProcess.setFailCount(0); // 失败次数
            int insert = mediaProcessMapper.insert(mediaProcess);
            if (insert <= 0) {
                log.error("添加转码任务失败,{}", mediaProcess.toString());
                XueChengPlusException.cast("添加转码任务失败");
            }
        }
    }

    @Transactional
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        File file = new File(localFilePath);
        if (!file.exists()) {
            XueChengPlusException.cast("文件不存在");
        }
        //文件名称
        String filename = uploadFileParamsDto.getFilename();
        //文件扩展名
        String extension = filename.substring(filename.lastIndexOf("."));
        //文件mimeType
        String mimeType = getMimeType(extension);
        //文件的md5值
        String fileMd5 = getFileMd5(file);
        //文件的默认目录
        String defaultFolderPath = getDefaultFolderPath();
        //存储到minio中的对象名(带目录)
        String objectName = defaultFolderPath + fileMd5 + extension;
        //将文件上传到minio
        boolean b = addMediaFilesToMinIO(localFilePath, mimeType, bucket_Files, objectName);
        //文件大小
        uploadFileParamsDto.setFileSize(file.length());
        //将文件信息存储到数据库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_Files, objectName);
        //准备返回数据
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        // 查询文件信息
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null){
            // 如果文件信息存在，则查询minio
            try {
                GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                        .bucket(mediaFiles.getBucket())
                        .object(mediaFiles.getFilePath())
                        .build();
                GetObjectResponse object = minioClient.getObject(getObjectArgs);
                if (object != null){
                    object.close();
                    return RestResponse.success(true);
                }else{
                    return RestResponse.success(false);
                }
            }catch(Exception e){
            }
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        String chunkFilePath = chunkFileFolderPath + chunkIndex;
        try {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket_VideoFiles)
                    .object(chunkFilePath)
                    .build();
            GetObjectResponse object = minioClient.getObject(getObjectArgs);
            if (object != null) {
                object.close();
                return RestResponse.success(true);
            }
        } catch (Exception e) {
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunkIndex, String localChunkFilePath) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        String chunkFilePath = chunkFileFolderPath + chunkIndex;
        String mimeType = getMimeType(null);
        boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_VideoFiles, chunkFilePath);
        if (!b){
            log.error("上传分块文件失败，fileMd5:{},chunkIndex:{},localChunkFilePath:{}", fileMd5, chunkIndex, localChunkFilePath);
            return RestResponse.validfail("上传分块文件失败");
        }
        log.info("上传分块文件成功:{}",chunkFilePath);
        return RestResponse.success(true);
    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        // 检查文件是否存在
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        List<ComposeSource> sourceList = new ArrayList<>();
        for (int i = 0; i < chunkTotal; i++) {
            String chunkFilePath = chunkFileFolderPath + i;
            try {
                ComposeSource composeSource = ComposeSource.builder()
                        .bucket(bucket_VideoFiles)
                        .object(chunkFilePath)
                        .build();
                sourceList.add(composeSource);
            } catch (Exception e) {
                log.error("获取分块文件失败，fileMd5:{},chunkIndex:{},错误信息:{}", fileMd5, i, e.getMessage());
                return RestResponse.validfail("获取分块文件失败");
            }
        }
        // 合并分块文件
        String fileName = uploadFileParamsDto.getFilename();
        String extName = fileName.substring(fileName.lastIndexOf("."));
        String mergeFilePath = getFilePathByMd5(fileMd5, extName);
        try {
            ObjectWriteResponse objectWriteResponse = minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket_VideoFiles)
                            .object(mergeFilePath)
                            .sources(sourceList)
                            .build()
            );
            log.info("合并文件成功:{}", mergeFilePath);
        }catch (Exception e){
            log.info("合并文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage());
            return RestResponse.validfail(false,"合并文件失败");
        }
        // 验证md5
        File minioFile = downloadFileFromMinIO(bucket_VideoFiles, mergeFilePath);
        if (minioFile == null){
            log.info("下载合并后文件失败,mergeFilePath:{}",mergeFilePath);
            return RestResponse.validfail(false, "下载合并后文件失败。");
        }
        try (InputStream inputStream = new FileInputStream(minioFile)){
            String md5_minio = DigestUtils.md5Hex(inputStream);
            if (!fileMd5.equals(md5_minio)){
                return RestResponse.validfail(false, "合并后文件MD5校验失败，可能是分块文件不完整或损坏。");
            }
            log.info("合并后文件MD5校验成功，fileMd5:{},md5_minio:{}", fileMd5, md5_minio);
            uploadFileParamsDto.setFileSize(minioFile.length());
        }catch(Exception e){
            log.info("校验文件失败,fileMd5:{},异常:{}", fileMd5, e.getMessage());
            return RestResponse.validfail(false, "校验文件失败，可能是分块文件不完整或损坏。");
        }

        // 将合并后的文件信息存储到数据库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_VideoFiles, mergeFilePath);
        if (mediaFiles == null){
            return RestResponse.validfail(false, "文件入库失败");
        }
        // 删除分块文件
        clearVideoBucketChunkFiles(chunkFileFolderPath, chunkTotal);
        return RestResponse.success(true, "文件合并成功");
    }

    private void clearVideoBucketChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        List<DeleteObject> deleteObjects = new ArrayList<>();
        for (int i = 0; i < chunkTotal; i++) {
            String chunkFilePath = chunkFileFolderPath + i;
            DeleteObject deleteObject = new DeleteObject(chunkFilePath);
            deleteObjects.add(deleteObject);
        }
        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                .bucket(bucket_VideoFiles)
                .objects(deleteObjects)
                .build();
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        for (Result<DeleteError> result : results) {
            try {
                DeleteError deleteError = result.get();
                log.error("删除分块文件失败，文件名:{}", deleteError.objectName());
            } catch (Exception e) {
                log.error("删除分块文件异常:{}", e.getMessage());
            }
        }
    }

    public File downloadFileFromMinIO(String bucket,String objectName){
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try{
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile=File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,outputStream);
            outputStream.flush();
            stream.close();
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return null;
    }

    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/chunk/";
    }

    private String getFilePathByMd5(String fileMd5,String fileExt){
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 +fileExt;
    }
}

