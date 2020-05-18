import model.RequestT;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;


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
				if(req.getResponse() != null) {
					match = true;
					if(req.getResponse().getStatus() != res.code()) match = false;
					final boolean[] doesHeadersMatch = {true};
					req.getResponse().getHeaders().forEach((s, s2) -> {
						if(!s2.equals(res.header(s))) doesHeadersMatch[0] = false;
					});
					match = (doesHeadersMatch[0]) && match;
					if(!Objects.equals(req.getResponse().getBody().replaceAll("\\s",""), res.body().string().replaceAll("\\s",""))) match = false;
					result = match? "Response match rule": "Response doesn't match rule";
					if (match) {
						LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- %d (%s)", request.method(), request.url(), res.code(), result));
					}
					else {
						LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- ERROR, waited: %s ; received : %s", request.method(), request.url(), req.getResponse().toString(), res.toString()));
					}
				}
			} catch (IOException e) {
				LoggerFactory.getLogger("MOCK").error(String.format("Request: %s %s -- ERROR %s", request.method(), request.url(), e.getClass().getSimpleName()));
			}
		}).run();//.start();
	}
}
