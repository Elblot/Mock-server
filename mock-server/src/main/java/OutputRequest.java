import model.OutInRule;
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
		try {
			if( req.getDelay() > 0) {
				Thread.sleep((long) req.getDelay());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int i = 0;
		boolean infinite = (req.getRepetition() == 0);
		while ((i < req.getRepetition() || infinite)) {
			Request.Builder builder = new Request.Builder()
					.url(req.getPath());
			if(req.getVerb().equals("GET")) {
				builder.method(req.getVerb(), null);
			} else {
				builder.method(req.getVerb(), RequestBody.create(req.getBody(), null));
			}
			req.getHeaders().forEach(builder::addHeader);

			requestAsync(builder.build());

			try {
				if( req.getDelay() > 0) {
					Thread.sleep(req.getDelay());//getInterval()); TODO define interval between repetition
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
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
			try (Response res = client.newCall(request).execute()) {
				String result = "no response found in the model";
				System.out.println(request.toString());
				if(req.getResponse() != null) {
					match = true;
					/*System.out.println("status rule: " + req.getResponse().getStatus());
					System.out.println("status resp: " + res.code());*/
					if(req.getResponse().getStatus() != res.code()) match = false;
					
					/*for (String rr: req.getResponse().getHeaders().keySet()) {
						System.out.println("head rule: " + rr + req.getResponse().getHeaders().get(rr));
						System.out.println("head resp: " + rr + res.header(rr));
					}*/
					final boolean[] doesHeadersMatch = {true};
					req.getResponse().getHeaders().forEach((s, s2) -> {
						if(!s2.equals(res.header(s))) doesHeadersMatch[0] = false;
					});
					match = (doesHeadersMatch[0]) && match;
					//System.out.println("body rule: " + req.getResponse().getBody().replaceAll("\\s",""));
					//System.out.println("body resp: " + res.body().string().replaceAll("\\s",""));
					if(!Objects.equals(req.getResponse().getBody().replaceAll("\\s",""), res.body().string().replaceAll("\\s",""))) match = false;
					result = match ? "Response match rule": "Response doesn't match rule";
				}
				LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- %d (%s)", request.method(), request.url(), res.code(), result));
			} catch (IOException e) {
				LoggerFactory.getLogger("MOCK").error(String.format("Request: %s %s -- ERROR %s", request.method(), request.url(), e.getClass().getSimpleName()));
			}
		}).run();//.start();
	}
}
