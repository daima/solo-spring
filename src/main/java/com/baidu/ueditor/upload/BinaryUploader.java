package com.baidu.ueditor.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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

public class BinaryUploader {
	private static Logger logger = LoggerFactory.getLogger(BinaryUploader.class);

	public static final State save(HttpServletRequest request, Map<String, Object> conf) {
		boolean isAjaxUpload = request.getHeader("X_Requested_With") != null;

		if (!ServletFileUpload.isMultipartContent(request)) {
			return new BaseState(false, 5);
		}
		State storageState = null;

		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

		if (isAjaxUpload) {
			upload.setHeaderEncoding("UTF-8");
		}
		File physicalFile = null;
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

				if (!validType(suffix, (String[]) conf.get("allowFiles"))) {
					return new BaseState(false, 8);
				}

				savePath = PathFormat.parse(savePath, originFileName);

				String physicalPath = (String) conf.get("rootPath") + savePath;

				physicalFile = new File(physicalPath);
				if (!physicalFile.getParentFile().exists()) {
					physicalFile.getParentFile().mkdirs();
				}

				try {
					file.transferTo(physicalFile);
					// root.put("md5", DigestUtils.md5Hex(new
					// FileInputStream(localFile)));
				} catch (IllegalStateException | IOException e) {
					logger.warn("写文件失败!{}", physicalFile);
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
		// FileItemIterator iterator = upload.getItemIterator(request);
		//
		// while (iterator.hasNext()) {
		// fileStream = iterator.next();
		//
		// if (!fileStream.isFormField())
		// break;
		// fileStream = null;
		// }
		//
		// if (fileStream == null) {
		// return new BaseState(false, 7);
		// }

		// InputStream is = fileStream.openStream();
		// State storageState = StorageManager.saveFileByInputStream(is,
		// physicalPath, maxSize);
		// is.close();
		//
		// if (storageState.isSuccess()) {
		// storageState.putInfo("url", PathFormat.format(savePath));
		// storageState.putInfo("type", suffix);
		// storageState.putInfo("original", originFileName + suffix);
		// }
		return storageState != null ? storageState : new BaseState(false, 4);
	}

	private static boolean validType(String type, String[] allowTypes) {
		List<String> list = Arrays.asList(allowTypes);

		return list.contains(type);
	}
}