import model.RequestT;
import model.ResponseT;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;


public class OutputRequest extends Thread {
	private RequestT req;
	private OkHttpClient client = new OkHttpClient();
	private boolean match;


	public OutputRequest(RequestT outInRule) {
		this.req = outInRule;
		match = false;
	}

	@Override
	public void run() {
		int i = 0;
		boolean infinite = (req.getRepetition() == 0);
		while ((i < req.getRepetition() || infinite)) {
			if( req.getDelay() > 0 & i != 0) {
				try {
					Thread.sleep(req.getDelay());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}			
			Request.Builder builder = new Request.Builder()
					.url(req.getPath());
			if(req.getVerb().equals("GET")) {
				builder.method(req.getVerb(), null);
			} else {
				builder.method(req.getVerb(), RequestBody.create(req.getBody(), null));
			}
			req.getHeaders().forEach(builder::addHeader);
			requestAsync(builder.build());
			i++;
		}
	}

	public boolean getMatch() {
		return match;
	}

	/**
	 * This method sends a HTTP request in a new Thread.
	 * @param request: the request to send.
	 */
	private void requestAsync(Request request) {
		new Thread(() -> {
			try (Response res = client.newCall(request).execute()) {
				String result = "no response found in the model";
				int code = res.code();
				//String head = res.header();
				String body = res.body().string();
				if(!req.getResponses().isEmpty()) {
					boolean b = true;
					for (ResponseT r: req.getResponses()) {
						match = true;
						if(r.getStatus() != code) match = false;
						final boolean[] doesHeadersMatch = {true};
						r.getHeaders().forEach((s, s2) -> {
							if(!s2.equals(res.header(s))) doesHeadersMatch[0] = false;
						});
						match = (doesHeadersMatch[0]) && match;
						if(!RespEquals(r, body.replaceAll("\\s",""))) match = false;
						result = match? "Response match rule": "Response doesn't match rule";
						if (match) {
							LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- %d (%s)", request.method(), request.url(), res.code(), result));
							b = false;
							r.setProc(true);
							break;
						}
					}
					if (b){
						LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- ERROR, waited: %s ; received : %s", request.method(), request.url(), req.getResponses().toString(), res.toString()));
					}
				}
				else {
					LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- %d (%s)", request.method(), request.url(), result));
				}
			} catch (IOException e) {
				LoggerFactory.getLogger("MOCK").error(String.format("Request: %s %s -- ERROR %s", request.method(), request.url(), e.getClass().getSimpleName()));
			}
		}).run();//.start();
	}

	private boolean RespEquals(ResponseT resp1, String st2) {
		String st1 = resp1.getBody().replaceAll("\\s","");
		if (st1.contains("**values**")){ 	
			String r = "^(" + cleanReg(st1);
			System.out.println(r);
			System.out.println(st2);
			for (int i = 0; i < resp1.getRegex().size(); ++i) {
				r = r.replaceFirst("\\*\\*values\\*\\*", ")" + resp1.getRegex().get(i) + "(");
			}
			r = r + ")$";
			if (Pattern.matches(r, st2)) {
				return true;
			}
		}
		else {
			if (st1.equals(st2)){
				return true;
			}
		}		
		/*
		if (st1.contains("**values**")) {//while
			boolean match = true;
			int prefix = st1.indexOf("**values**");
			int suffix = st1.length() - 1 - st1.indexOf("**values**") - 10;
			if (!resp1.getRegex().equals("")) {
				if (st2.length() > prefix + suffix) {
					String value = st2.substring(prefix, st2.length() - suffix - 1);	
					match = Pattern.matches(resp1.getRegex(), value);
				}
			}
			String path1 = st1.replaceAll("\\*\\*values\\*\\*", "");
			if (st2.length() > prefix + suffix) {
				String urlprefix = st2.substring(0, prefix);
				String urlsuffix = st2.substring(st2.length() - suffix - 1);
				String path2 = urlprefix + urlsuffix;
				if (match && path1.equals(path2)) {
					return true;
				}
			}				
		}
		else {
			if (st1.equals(st2)) {
				return true;
			}
		}*/
		return false;
	}

	private static String cleanReg(String reg) {
		String res = reg;
		res = res.replaceAll("\\&", "\\&");
		res = res.replaceAll("\\(", "\\(");
		res = res.replaceAll("\\)", "\\)");
		res = res.replaceAll("\\[", "\\[");
		res = res.replaceAll("\\]", "\\]");
		res = res.replaceAll("\\.", "\\.");
		res = res.replaceAll("\\?", "\\?");		
		res = res.replaceAll("\\{", "\\\\{");
		res = res.replaceAll("\\}", "\\\\}");
		res = res.replaceAll("\\*", "\\*");
		res = res.replaceAll("\\|", "\\|");
		res = res.replaceAll("\\+", "\\+");
		res = res.replaceAll("\\:", "\\:");
		res = res.replaceAll("\\$", "\\$");
		res = res.replaceAll("\\^", "\\^");
		return res;
	}
	
}
