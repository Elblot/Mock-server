import io.javalin.Javalin;
import io.javalin.http.Handler;
import load.DotLoader;
import load.Loader;
import load.LoaderException;
import model.LTS;
import model.RequestT;
import model.ResponseT;
import model.State;

import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@WebServlet(urlPatterns = {"/*"})
public class WebService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private LTS dot;
	private State pos;
	private Javalin app;
	private long lastAction;
	private static ArrayList<Long> fifo = new ArrayList<Long>(); // not a queue, the different times can be put in any order

	/**
	 * This method starts the web server. The server is listening on 3 paths:
	 * /config, /rules and /attack.
	 * It also collects the content type of the HTTP request.
	 */
	public WebService() {
		app = Javalin.createStandalone(config -> {
			config.requestLogger((ctx, executionTimeMs) -> LoggerFactory.getLogger("MOCK").info(String.format("%s on %s -> %d", ctx.method(), ctx.fullUrl(), ctx.res.getStatus())));
		});
		app.exception(LoaderException.class, ExceptionHandlers.genericHandler(400));
		app.exception(Exception.class, (exception, ctx) -> {
			exception.printStackTrace();
			ctx.status(500);
			ctx.result(String.format("%s, %s", exception.getClass().toString(), exception.getMessage()));
		});

		app.post("/rules", DotHandler()); // receipt the mock model
		//app.post("/attack",attackHandler()); TODO
		app.get("/", ctx -> ctx.result("Mock: It works ! \n Send the dot file representing the mock to the path /rules as plain/text data to start it."));
	}

	/**
	 * This method is called when the user does a HTTP POST request to the
	 * attack path of the mock IP address. It calls the correct attack methods
	 * according to the user's choice.
	 */
	/*private Handler attackHandler() {
		return ctx -> {
			if(getRules().size() != 0) {
				Attacker attacker = new Attacker(getRules());
				ctx.status(204);
				if(Objects.equals(ctx.queryParam("type"), "all")) {
					attacker.XSSAttacks();
					attacker.httpFloodAttack();
					attacker.robustnessAttacks();
				} else if (Objects.equals(ctx.queryParam("type"), "httpflood")) {
					attacker.httpFloodAttack();
				} else if(Objects.equals(ctx.queryParam("type"), "xss")) {
					attacker.XSSAttacks();
				} else if(Objects.equals(ctx.queryParam("type"),"robustness")) {
					attacker.robustnessAttacks();
				}
				else {
					ctx.result("Error: wrong/no attack type given;");
					ctx.status(400);
				}
				attacker.attack();
			} else {
				ctx.result("Error: no rules found.");
				ctx.status(400);
			}
		};
	}*/

	/**
	 * This method build all the rules followed by the service when it 
	 * receives a request. The response are constructed according to the graph
	 * and warn the user if the behavior of the other components do not 
	 * follow it.
	 */
	private void buildInputRequestHandler() {
		//TODO make for the post to
		for (String path: dot.getInputRequests()) {
			app.get(path, ctx -> {
				long time = System.currentTimeMillis();
				fifo.add(time);
				synchronized(this){ 
					RequestT t = dot.getReq(ctx.fullUrl(), pos);
					long now = System.currentTimeMillis();
					if (t !=  null) { // if the request received is allowed by the graph from current position
						if (t.getDelay() == 0 || lastAction != 0 || t.getDelay() < now - lastAction) { // if the request is not received to late
							while (pos.isInit() && getMin() != time) { 
								// if several possible, only one processed at a time (the first received)
								// only possible for initial state
								//TODO case of indeterministic inputs, and both received at the same times.
								this.wait();
							}						
							pos = t.getTarget();
							lastAction = now;
							while (runMock(true) == true); //if nested requests
							ResponseT resp = t.getResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
							//TODO add the delay if there is one for the response
							Thread.sleep(10);//// avoid getting several responses to send at the same time
							if (resp.getContent() != null) {
								ctx.contentType(resp.getContent());
							}
							for (String h: resp.getHeaders().keySet()) {
								ctx.header(h, resp.getHeaders().get(h));
							}
							fifo.remove(time);
							if (pos.equals(resp.getSource())) { // check if the response can be sent in the graph
								pos = resp.getTarget();
								if (pos.isFinal()) { // useless?
									pos = dot.getInitialState();
									LoggerFactory.getLogger("MOCK").info(String.format("beginning of a new session."));
								}
							}
							else { // the response cannot be send from this state in the graph
								ctx.status(500);
								LoggerFactory.getLogger("MOCK").info(String.format("error: response " + resp.toString() + " launched from state " + pos.toString()));
								pos = dot.getInitialState();
							}
						}
						else { // the request is receive after the delay
							fifo.remove(time);
							ctx.result("request received too late.");
							ctx.status(500);
							LoggerFactory.getLogger("MOCK").info(String.format("request received at the wrong position in the model: " + ctx.fullUrl()));
						}
					}
					else { // the request can be the start of a new session, check from the initial state
						RequestT t2 = dot.getReq(ctx.fullUrl(), dot.getInitialState());
						if (t2 !=  null) { // if the request received is allowed by the graph from the initial state
							this.wait();
							while (getMin() != time) {
								// if several possible, only one processed at a time (the first received)
								this.wait();
							}							
							pos = t2.getTarget();
							lastAction = System.currentTimeMillis();
							while (runMock(true) == true);//if nested requests
							ResponseT resp = t2.getResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
							//TODO add the delay if there is one for the response
							Thread.sleep(10);////avoid getting several responses to send at the same time
							if (resp.getContent() != null) {
								ctx.contentType(resp.getContent());
							}
							for (String h: resp.getHeaders().keySet()) {
								ctx.header(h, resp.getHeaders().get(h));
							}
							fifo.remove(time);
							if (pos.equals(resp.getSource())) {
								pos = resp.getTarget();
								if (pos.isFinal()) {
									pos = dot.getInitialState();
									LoggerFactory.getLogger("MOCK").info(String.format("beginning of a new session."));
								}
							}
							else { // the response cannot be send from this state in the graph
								ctx.status(500);
								LoggerFactory.getLogger("MOCK").info(String.format("error: response " + resp.toString() + " launched from state " + pos.toString()));
								pos = dot.getInitialState();
							}
							//LoggerFactory.getLogger("MOCK").info(String.format("out : " +  time	 + " req : " + ctx.fullUrl()));
						}
						else { // the request is receive after the delay
							fifo.remove(time);
							ctx.result("request received at the wrong position in the model.");
							ctx.status(500);
							LoggerFactory.getLogger("MOCK").info(String.format("request received at the wrong position in the model: " + ctx.fullUrl()));
						}
					}
				}
			});
		}
	}

	/**
	 * This method return the smallest time in the ArrayList fifo
	 * @return the smallest time in fifo
	 */
	static long getMin() {
		long res = fifo.get(0);
		for (long l : fifo) {
			if (res > l) {
				res = l;
			}
		}
		return res;
	}

	/**
	 * This method is called when the user does a HTTP POST request to the /rules
	 * path of the mock IP address. It loads the rules given with the JSON or YAML loader.
	 */
	private Handler DotHandler() {
		return ctx -> {
			Loader loader;
			if (Objects.equals(ctx.header("Content-Type"), "text/plain")) {
				loader = new DotLoader();
			}
			else {
				throw new LoaderException("Wrong content type, \"text/plain\" was expected");
			}
			dot = loader.load(ctx.body());
			pos = dot.getInitialState();
			buildInputRequestHandler();
			lastAction = System.currentTimeMillis();
			ctx.status(204);
			//for (int i = 0 ; i < 10; i++) { //nb session run by the mock
			while(true) {
				if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) { 
					Thread.sleep(10);
					continue;
				}
				synchronized(this){
					while (runMock(false) == true);
					if (pos.isInit()) {
						this.notifyAll();
					}
					// if component waits for to receive a request, free the thread
					if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {
						continue;
					}
					pos = dot.getInitialState();
					LoggerFactory.getLogger("MOCK").info(String.format("beginning of a new session."));
					this.notifyAll();// if end of the session, start one paused.
				}
			}
		};
	}

	/** 
	 * This method runs the mock following the graph. It sends the request and 
	 * compare the corresponding response with the one waited in the graph.
	 * Return true if the mock acted or can act, and return false else.
	 * @throws InterruptedException
	 *  **/
	private boolean runMock(boolean passOutResp) throws InterruptedException {
		// received response are already processed, skip them in the model
		if (!pos.isInit() && pos.getInResp() != null) {
			long now = System.currentTimeMillis();
			pos = pos.getInResp().getTarget();
			lastAction = now;
			return true;
		}
		// if the component can receive a request, wait for it 
		if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {
			Thread.sleep(10);
			return false; // free the thread in order to receive the request
		}
		// if the component have to send a response, wait it's done
		if (!passOutResp && (pos.plannedResponse() > System.currentTimeMillis() - lastAction || pos.plannedResponse() == 0)) {
			Thread.sleep(10);
			return true;
		}
		// sending of a request 
		RequestT output = pos.getOutReq(lastAction);
		if (output != null) {
			output.incWeight();
			OutputRequest send = new OutputRequest(output);
			lastAction = System.currentTimeMillis();;
			send.run();
			pos = output.getTarget();
			if (!send.getMatch()) {
				return false;
			}
			return true;
		}
		// if the component wants to send a request but it's too early
		if (!pos.getFutureOutReq().isEmpty()) {
			Thread.sleep(10);
			return true;
		}
		return false;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		app.servlet().service(req, resp);
	}
}
