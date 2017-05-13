package org.b3log.solo.filter;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.HttpPutFormContentFilter;

public class HttpPutBodyFilter extends HttpPutFormContentFilter{
	private FormHttpMessageConverter formConverter = new PutBodyConverter();
	public HttpPutBodyFilter() {
		setFormConverter(formConverter);
	}
	
}
class PutBodyConverter extends FormHttpMessageConverter {
	private Charset charset = DEFAULT_CHARSET;
	@Override
	public MultiValueMap<String, String> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType.getCharset() != null ? contentType.getCharset() : this.charset);
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
		result.add("body", body);
		return result;
	}
}
