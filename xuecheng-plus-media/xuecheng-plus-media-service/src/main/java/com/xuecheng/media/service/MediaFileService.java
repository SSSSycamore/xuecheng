package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

import java.io.File;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     *
     * @param companyId           机构id
     * @param uploadFileParamsDto 上传文件信息
     * @param localFilePath       文件磁盘路径
     * @return 文件信息
     */
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);

    RestResponse<Boolean> checkFile(String fileMd5);

    RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

    RestResponse uploadChunk(String fileMd5, int chunkIndex, String localChunkFilePath);

    public RestResponse mergechunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamsDto uploadFileParamsDto);

    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName);

    public File downloadFileFromMinIO(String bucket, String objectName);

    /**
     * @param localFilePath 文件地址
     * @param bucket        桶
     * @param objectName    对象名称
     * @return void
     * @description 将文件写入minIO
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String mimeType, String bucket, String objectName);
}
