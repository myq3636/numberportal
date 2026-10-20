package com.liugh.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liugh
 * @since on 2018/7/25
 */
public final class HttpUtil {

	private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	private static final MediaType CONTENT_TYPE_FORM = MediaType
			.parse("application/x-www-form-urlencoded;charset=UTF-8");
	private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36";

	private static PoolingHttpClientConnectionManager connManager = null;
	
	public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("TLSv1.2");

		// 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
		X509TrustManager trustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
					String paramString) throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		sc.init(null, new TrustManager[] { trustManager }, null);
		return sc;
	}

	static {
		try {
			SSLContext sslcontext = createIgnoreVerifySSL();
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", new SSLConnectionSocketFactory(sslcontext)).build();
			connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			connManager.setMaxTotal(100);
			connManager.setDefaultMaxPerRoute(20);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
	static RequestConfig requestConfig = RequestConfig.custom()
			// 获取连接超时时间
			.setConnectionRequestTimeout(30000)
			// 请求超时时间
			.setConnectTimeout(30000)
			// 响应超时时间
			.setSocketTimeout(30000).build();
	static CloseableHttpClient httpclient  = HttpClients.custom()
	// 把请求相关的超时信息设置到连接客户端
	.setDefaultRequestConfig(requestConfig)
	// 配置连接池管理对象
	.setConnectionManager(connManager).build();

	/**
	 * 获取Http客户端连接对象
	 * 
	 * @param timeOut 超时时间
	 * @return Http客户端连接对象
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static CloseableHttpClient getHttpClient(Integer timeOut) {		
		/**
		 * 测出超时重试机制为了防止超时不生效而设置 如果直接放回false,不重试 这里会根据情况进行判断是否重试
		 *//*
			 * HttpRequestRetryHandler retry = new HttpRequestRetryHandler() {
			 * 
			 * @Override public boolean retryRequest(IOException exception, int
			 * executionCount, HttpContext context) { if (executionCount >= 1) {//
			 * 如果已经重试了3次，就放弃 return false; } if (exception instanceof
			 * NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试 return true; } if (exception
			 * instanceof SSLHandshakeException) {// 不要重试SSL握手异常 return false; } if
			 * (exception instanceof InterruptedIOException) {// 超时 return true; } if
			 * (exception instanceof UnknownHostException) {// 目标服务器不可达 return false; } if
			 * (exception instanceof ConnectTimeoutException) {// 连接被拒绝 return false; } if
			 * (exception instanceof SSLException) {// ssl握手异常 return false; }
			 * HttpClientContext clientContext = HttpClientContext.adapt(context);
			 * HttpRequest request = clientContext.getRequest(); // 如果请求是幂等的，就再次尝试 if
			 * (!(request instanceof HttpEntityEnclosingRequest)) { return true; } return
			 * false; } };
			 */

		// 采用绕过验证的方式处理https请求
		try {			
			return httpclient;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	private HttpUtil() {
	}

	public static final String get(String url) {
		String result = "";
		CloseableHttpClient client = getHttpClient(30000);
		HttpGet method = new HttpGet(url);
		CloseableHttpResponse response = null;
		InputStream in = null;
		try {
			// 执行请求
			response = client.execute(method);
			// 获取响应实体
			HttpEntity entity = response.getEntity();
			// 获取响应信息
			if (entity != null && entity.getContent() != null) {
				in = entity.getContent();
				result = IOUtils.toString(in, "utf-8");
			}

		} catch (ClientProtocolException e) {
			System.err.println("协议错误");
			e.printStackTrace();
		} catch (ParseException e) {
			System.err.println("解析错误");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IO错误");
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e1) {
					System.err.println("释放链接错误");
					e1.printStackTrace();
				}
			if (null != response) {
				try {
					EntityUtils.consume(response.getEntity());
					response.close();
				} catch (IOException e) {
					System.err.println("释放链接错误");
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public static final String getTexterResult(String url, Map<String, String> headerMap) {
		String result = "";
		CloseableHttpClient client = getHttpClient(300000);
		HttpGet method = new HttpGet(url);
		if (headerMap != null) {
			Set<String> keys = headerMap.keySet();
			for (String key : keys) {
				method.setHeader(key, headerMap.get(key));
			}
		}
		CloseableHttpResponse response = null;
		InputStream in = null;
		try {
			// 执行请求
			response = client.execute(method);
			// 获取响应实体
			HttpEntity entity = response.getEntity();
			// 获取响应信息
			if (entity != null && entity.getContent() != null) {
				in = entity.getContent();
				result = IOUtils.toString(in, "utf-8");
			}

		} catch (ClientProtocolException e) {
			System.err.println("协议错误");
			e.printStackTrace();
		} catch (ParseException e) {
			System.err.println("解析错误");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IO错误");
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e1) {
					System.err.println("释放链接错误");
					e1.printStackTrace();
				}
			if (null != response) {
				try {
					EntityUtils.consume(response.getEntity());
					response.close();
				} catch (IOException e) {
					System.err.println("释放链接错误");
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	/**
	 * 设置post请求的参数
	 * 
	 * @param httpPost
	 * @param params
	 */
	private static void setPostParams(HttpPost httpPost, Map<String, String> params) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		Set<String> keys = params.keySet();
		for (String key : keys) {
			nvps.add(new BasicNameValuePair(key, params.get(key)));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static final String post(String url, Map<String, String> params, Map<String, String> headerMap) {
		String result = "";
		CloseableHttpClient client = getHttpClient(300000);
		HttpPost httpPost = new HttpPost(url);
		setPostParams(httpPost, params);
		if (headerMap != null) {
			Set<String> keys = headerMap.keySet();
			for (String key : keys) {
				httpPost.setHeader(key, headerMap.get(key));
			}
		}
		CloseableHttpResponse response = null;
		InputStream in = null;
		try {
			response = client.execute(httpPost, HttpClientContext.create());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				in = entity.getContent();
				result = IOUtils.toString(in, "utf-8");

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
				if (response != null)
					response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public static String postJson(String url, String body) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postMultipart(String url, String body) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "multipart/form-data");

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postJsonWithToken(String url, String body, String token) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("token", token);

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postWithHttpEntity(String url, HttpEntity entity) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(entity);
			// 执行http请求
			response = client.execute(httpPost);
			// resultString = EntityUtils.toString(response.getEntity(), "utf-8");
			resultString = response.getStatusLine().getReasonPhrase();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postJsonWithAuthorization(String url, String body, String token) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Accept", "application/json");
			httpPost.addHeader("Authorization", token);

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postJsonWithBasicToken(String url, String body, String token) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Accept", "application/json");
			httpPost.addHeader("Authorization", "Basic " + token);

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}
	
	public static String postJsonForRCSDR(String url, String body, String token, String msgid,String eventId, String timestamp, String signature) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("X-RCS-Event-Id", eventId);
			httpPost.addHeader("X-RCS-Request-Id", msgid);
			httpPost.addHeader("X-RCS-Timestamp", String.valueOf(timestamp));
			httpPost.addHeader("X-RCS-Signature", "sha256=" + signature);
            

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postJsonWithBearerToken(String url, String body, String token) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Accept", "application/json");
			httpPost.addHeader("Authorization", "Bearer " + token);

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String postJsonWithUserHeader(String url, String body, String user, String pwd) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Accept", "application/json");
			httpPost.addHeader("user", user);
			httpPost.addHeader("password", pwd);
			httpPost.addHeader("hashSeguranca", "3Lr7vEbQA");

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}
	
	public static String postJsonForBraxis(String url, String body, String user, String pwd) {
		// 创建Httpclient对象
		CloseableHttpClient client = getHttpClient(300000);
		CloseableHttpResponse response = null;
		String resultString = null;
		try {
			// 创建Http Post请求
			HttpPost httpPost = new HttpPost(url);
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("Accept", "application/json");
			httpPost.addHeader("user", user);
			httpPost.addHeader("password", pwd);
			httpPost.addHeader("hashSeguranca", "3Lr7vEbQA");

			if (body != null) {
				httpPost.setEntity(new StringEntity(body, "utf-8"));
			}
			// 执行http请求
			response = client.execute(httpPost);
			resultString = EntityUtils.toString(response.getEntity(), "utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return resultString;
	}

	public static String post(String url, String params) {
		String result = "";
		CloseableHttpClient client = getHttpClient(300000);
		HttpPost httpPost = new HttpPost(url);
		CloseableHttpResponse response = null;
		InputStream in = null;
		try {
			httpPost.setEntity(new StringEntity(params, "utf-8"));
			response = client.execute(httpPost, HttpClientContext.create());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				in = entity.getContent();
				result = IOUtils.toString(in, "utf-8");

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
				if (response != null)
					response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private static String exec(Request request) {
		try {
			okhttp3.Response response = new OkHttpClient().newCall(request).execute();
			if (!response.isSuccessful()) {
				throw new RuntimeException("Unexpected code " + response);
			}
			String respString = response.body().string();
			return respString;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String postSSL(String url, String data, String certPath, String certPass) {
		try {
			KeyStore clientStore = KeyStore.getInstance("PKCS12");
			// 读取本机存放的PKCS12证书文件
			FileInputStream instream = new FileInputStream(certPath);
			try {
				// 指定PKCS12的密码(商户ID)
				clientStore.load(instream, certPass.toCharArray());
			} finally {
				instream.close();
			}
			SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(clientStore, certPass.toCharArray()).build();
			// 指定TLS版本
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" },
					null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			// 设置httpclient的SSLSocketFactory
			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
			try {
				HttpPost httpost = new HttpPost(url); // 设置响应头信息
				httpost.addHeader("Connection", "keep-alive");
				httpost.addHeader("Accept", "*/*");
				httpost.addHeader("Content-Type", CONTENT_TYPE_FORM.toString());
				httpost.addHeader("X-Requested-With", "XMLHttpRequest");
				httpost.addHeader("Cache-Control", "max-age=0");
				httpost.addHeader("User-Agent", DEFAULT_USER_AGENT);
				httpost.setEntity(new StringEntity(data, "UTF-8"));
				CloseableHttpResponse response = httpclient.execute(httpost);
				try {
					HttpEntity entity = response.getEntity();
					String jsonStr = EntityUtils.toString(response.getEntity(), "UTF-8");
					EntityUtils.consume(entity);
					return jsonStr;
				} finally {
					response.close();
				}
			} finally {
				httpclient.close();
			}
		} catch (Exception e) {
			logger.error("", e);
			throw new RuntimeException(e);
		}
	}
}
