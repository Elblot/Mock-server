import model.RequestT;
import model.ResponseT;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This class represents a request that will be send by teh mock.
 */

public class OutputRequest extends Thread {
	private RequestT req; //request to send extracted from the model
	private OkHttpClient client = new OkHttpClient();
	private boolean match;


	public OutputRequest(RequestT outInRule) {
		this.req = outInRule;
		match = false;
	}

	/**
	 * build the request from the RequestT req, and call Send the request, and assess the response to see if it fit the model.
	 **/
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
			if (!WebService.mode.equals("dos")) {
				requestAsync(builder.build());
			}
			else {
				requestAsyncDos(builder.build());
			}
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
			long reqSent = System.currentTimeMillis();
			try (Response res = client.newCall(request).execute()) {
				//if (!WebService.mode.equals("dos")) {
				long time = System.currentTimeMillis() - reqSent;
				//String result = "no response found in the model";
				int code = res.code();
				String body = res.body().string();
				if(!req.getResponsesDelay(time).isEmpty()) {
					//if(!req.getResponses().isEmpty()) {
					boolean b = true;
					int w = -1;
					ResponseT resp = null;
					for (ResponseT r: req.getResponsesDelay(time)) {
						//for (ResponseT r: req.getResponses()) {
						match = true;
						if(r.getStatus() != code) match = false;
						final boolean[] doesHeadersMatch = {true};
						r.getHeaders().forEach((s, s2) -> {
							if(!s2.equals(res.header(s))) doesHeadersMatch[0] = false;
						});
						match = (doesHeadersMatch[0]) && match;
						if(!RespEquals(r, body.replaceAll("\\s",""))) match = false;
						//result = match? "Response match rule": "Response doesn't match rule";
						if (match && (w == -1 || w > r.getWeight())) {
							resp = r;
							w = r.getWeight();
							b = false;
						}

					}
					if (!b && resp != null) {
						//System.out.println(resp.getTarget());
						LogManager.getLogger("MOCK").info(String.format("Request: %s %s -- %d (%s)", request.method(), request.url(), res.code(), "response match rule"));
						resp.setProc(true);
						resp.incWeight();
					}
					else {
						LogManager.getLogger("MOCK").info(String.format("Request: %s %s -- ERROR, waited: %s ; received : %s", request.method(), request.url(), req.getResponses().toString(), res.toString()));
					}
				}
				else {
					LogManager.getLogger("MOCK").info(String.format("Request: %s %s -- ERROR, no request with coresponding delay found", request.method(), request.url()));
				}
				//}
			} catch (IOException e) {
				LogManager.getLogger("MOCK").info(String.format("Request: %s %s -- ERROR %s", request.method(), request.url(), e.getClass().getSimpleName()));
			}
		}).run();
	}

	
	
	
	/**
	 * CHeck if the response received st2 match with a response resp1 of the model
	 * @param resp1 a response from the model
	 * @param st2 the response received
	 * @return boolean
	 */
	private boolean RespEquals(ResponseT resp1, String st2) {
		String st1 = resp1.getBody().replaceAll("\\s","");
		if (st1.contains("**values**")){ 	
			String r = "^(" + cleanReg(st1);
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
		return false;
	}

	/**
	 * Clean the string, adding special characters for the regex that are remove by java.
	 * @param reg
	 * @return 
	 */
	private static String cleanReg(String reg) {
		String res = reg;
		res = res.replaceAll("\\&", "\\\\&");
		res = res.replaceAll("\\(", "\\\\(");
		res = res.replaceAll("\\)", "\\\\)");
		res = res.replaceAll("\\[", "\\\\[");
		res = res.replaceAll("\\]", "\\\\]");
		res = res.replaceAll("\\.", "\\\\.");
		res = res.replaceAll("\\?", "\\\\?");		
		res = res.replaceAll("\\{", "\\\\{");
		res = res.replaceAll("\\}", "\\\\}");
		//res = res.replaceAll("\\*", "\\\\*"); block the detection of **values**
		res = res.replaceAll("\\|", "\\\\|");
		res = res.replaceAll("\\+", "\\\\+");
		res = res.replaceAll("\\:", "\\:");
		res = res.replaceAll("\\$", "\\\\$");
		res = res.replaceAll("\\^", "\\\\^");
		return res;
	}

	/**
	 * This method sends a HTTP request in a new Thread.
	 * @param request: the request to send.
	 */
	private void requestAsyncDos(Request request) {
		new Thread(() -> {
			try (Response res = client.newCall(request).execute()) {
						//LogManager.getLogger("MOCK").info(String.format("Request: %s %s -- sent", request.method(), request.url()));
			} catch (IOException e) {
				//LogManager.getLogger("MOCK").info(String.format("Request: %s %s -- ERROR %s", request.method(), request.url(), e.getClass().getSimpleName()));
			}
		}).start();
	}
	
}
