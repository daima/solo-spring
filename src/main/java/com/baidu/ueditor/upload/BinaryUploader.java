package com.baidu.ueditor.upload;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.baidu.ueditor.PathFormat;
import com.baidu.ueditor.define.AppInfo;
import com.baidu.ueditor.define.BaseState;
import com.baidu.ueditor.define.FileType;
import com.baidu.ueditor.define.State;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

public class BinaryUploader {
	private static Logger logger = LoggerFactory.getLogger(BinaryUploader.class);
	public static final State save(HttpServletRequest request,
			Map<String, Object> conf) {
		String rootPath = (String) conf.get("rootPath");
		
		FileItemStream fileStream = null;
		boolean isAjaxUpload = request.getHeader( "X_Requested_With" ) != null;

		if (!ServletFileUpload.isMultipartContent(request)) {
			return new BaseState(false, AppInfo.NOT_MULTIPART_CONTENT);
		}

		ServletFileUpload upload = new ServletFileUpload(
				new DiskFileItemFactory());

        if ( isAjaxUpload ) {
            upload.setHeaderEncoding( "UTF-8" );
        }
        
        File physicalFile = null;
        State storageState = null;
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		// 判断 request 是否有文件上传,即多部分请求
		if (multipartResolver.isMultipart(request)) {
			// 转换成多部分request
			MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
			// 取得request中的所有文件名
			Iterator<String> iter = multiRequest.getFileNames();
			while (iter.hasNext()) {
				// 取得上传文件
				MultipartFile file = multiRequest.getFile(iter.next());
				// 取得当前上传文件的文件名称
				String originFileName = file.getOriginalFilename();

				String savePath = (String) conf.get("savePath");
				// String originFileName = fileStream.getName();
				String suffix = FileType.getSuffixByFilename(originFileName);

				originFileName = originFileName.substring(0, originFileName.length() - suffix.length());
				savePath = savePath + suffix;

				long maxSize = ((Long) conf.get("maxSize")).longValue();
				if (file.getSize() > maxSize) {
					return new BaseState(false, 1);
				}

				if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
					return new BaseState(false, 8);
				}

				savePath = PathFormat.parse(savePath, originFileName);

				String physicalPath = rootPath + savePath;

				physicalFile = new File(physicalPath);
				if (!physicalFile.getParentFile().exists()) {
					physicalFile.getParentFile().mkdirs();
				}

				try {
					file.transferTo(physicalFile);
					// root.put("md5", DigestUtils.md5Hex(new
					// FileInputStream(localFile)));
					// 给图片增加水印
					if ((boolean)conf.getOrDefault("isWatermark", false)) {
						String name = physicalFile.getName().toLowerCase();
						String[] arr = (String[])conf.get("imageAllowFiles");
						boolean isImage = false;	// 判断是否是图片格式
						for (String str : arr) {
							if (name.endsWith(str)) {
								isImage = true;
								break;
							}
						}
						if (isImage) {
							String watermarkPath = rootPath + conf.get("watermarkImgPath");
							String type = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
							FileUtils.copyFile(physicalFile, new File(physicalPath + ".bak"));	//backup
							addWatermark(physicalFile, type, watermarkPath);
						}
					}
				} catch (IllegalStateException | IOException e) {
					logger.warn("写文件失败!{}", physicalFile, e);
					continue;
				}
				storageState = new BaseState(true, AppInfo.SUCCESS);
				if (physicalFile != null) {
					storageState.putInfo("url", PathFormat.format(savePath));
					storageState.putInfo("type", suffix);
					storageState.putInfo("original", originFileName + suffix);

				}
			}

		}

//		try {
//			FileItemIterator iterator = upload.getItemIterator(request);
//
//			while (iterator.hasNext()) {
//				fileStream = iterator.next();
//
//				if (!fileStream.isFormField())
//					break;
//				fileStream = null;
//			}
//
//			if (fileStream == null) {
//				return new BaseState(false, AppInfo.NOTFOUND_UPLOAD_DATA);
//			}
//
//			String savePath = (String) conf.get("savePath");
//			String originFileName = fileStream.getName();
//			String suffix = FileType.getSuffixByFilename(originFileName);
//
//			originFileName = originFileName.substring(0,
//					originFileName.length() - suffix.length());
//			savePath = savePath + suffix;
//
//			long maxSize = ((Long) conf.get("maxSize")).longValue();
//
//			if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
//				return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
//			}
//
//			savePath = PathFormat.parse(savePath, originFileName);
//
//			//modified by Ternence
//            String rootPath = ConfigManager.getRootPath(request,conf);
//            String physicalPath = rootPath + savePath;
//            
//
//			InputStream is = fileStream.openStream();
//			State storageState = StorageManager.saveFileByInputStream(is,
//					physicalPath, maxSize);
//			is.close();
//
//			if (storageState.isSuccess()) {
//				storageState.putInfo("url", PathFormat.format(savePath));
//				storageState.putInfo("type", suffix);
//				storageState.putInfo("original", originFileName + suffix);
//			}
//
//			return storageState;
//		} catch (FileUploadException e) {
//			return new BaseState(false, AppInfo.PARSE_REQUEST_ERROR);
//		} catch (IOException e) {
//		}
//		return new BaseState(false, AppInfo.IO_ERROR);
		return storageState != null ? storageState : new BaseState(false, 4);
	}
	private static void addWatermark(File image, String type, String watermarkPath) throws IOException {
		BufferedImage bi = ImageIO.read(image);
		BufferedImage waterMarkBufferedImage = Thumbnails
				.of(watermarkPath)
				.size(bi.getWidth() / 2, bi.getHeight() / 2)
				.asBufferedImage();
		
		bi = Thumbnails.of(bi)
				.scale(1.0f)
				.watermark(Positions.CENTER, waterMarkBufferedImage, 0.5f)
				.asBufferedImage();
		ImageIO.write(bi, type, image);
	}

	private static boolean validType(String type, String[] allowTypes) {
		List<String> list = Arrays.asList(allowTypes);

		return list.contains(type);
	}
}
