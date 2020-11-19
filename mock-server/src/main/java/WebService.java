import io.javalin.Javalin;
import io.javalin.http.Handler;
import load.DotLoader;
import load.Loader;
import load.LoaderException;
import model.LTS;
import model.RequestT;
import model.ResponseT;
import model.State;

//import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


/**
 * @author Elliott Blot
 */
@WebServlet(urlPatterns = {"/*"})
public class WebService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private volatile boolean dotLoaded;
	private LTS dot;
	private State pos;
	private Javalin app;
	private long lastAction;
	private static ArrayList<Long> fifo = new ArrayList<Long>(); // not a queue, the different times can be put in any order
	private String mode;

	/**
	 * This method starts the web server. The server is listening on 2 paths:
	 * /rules and /mode.
	 * It also collects the content type of the HTTP request.
	 * @throws InterruptedException 
	 */
	public WebService() throws InterruptedException {
		mode = "classic";
		//mode = "XSS";
		dotLoaded = false;
		
		
		
		
		
		app = Javalin.createStandalone(config -> {
			config.requestLogger((ctx, executionTimeMs) -> LogManager.getLogger("MOCK").error(String.format("%s on %s -> %d: %s", ctx.method(), ctx.fullUrl(), ctx.res.getStatus(), ctx.resultString())));
		});
		
		
		/*
		//testing output
        System.out.println("SYSTEM.OUT.PRINTLN");
        LogManager.getLogger("MOCK").info("LOGGER.INFO");
        LogManager.getLogger("MOCK").debug("LOGGER.DEBUG");
        LogManager.getLogger("MOCK").trace("LOGGER.TRACE");
        LogManager.getLogger("MOCK").error("LOGGER.ERROR");
        LogManager.getLogger("MOCK").warn("LOGGER.WARN");

        // reading log4j.properties
        java.io.InputStream propertiesStream = this.getClass().getClassLoader().getResourceAsStream("log4j.properties");
        java.util.Scanner s = new java.util.Scanner(propertiesStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        // writing log4j.properties to stdout
        System.out.println("---CONTENTS OF log4j.properties---");
        System.out.println(result);
        System.out.println("---END CONTENTS---");*/
		
		
		
		app.exception(LoaderException.class, ExceptionHandlers.genericHandler(400));
		app.exception(Exception.class, (exception, ctx) -> {
			exception.printStackTrace();
			ctx.status(500);
			ctx.result(String.format("%s, %s", exception.getClass().toString(), exception.getMessage()));
		});

		app.post("/mode", ModeChange());
		app.post("/rules", DotHandler()); // receipt the mock model
		app.get("/", ctx -> ctx.result("Mock: It works ! \n Send the dot file representing the mock to the path /rules as plain/text data to start it. "
				+ "\n current mode: " + mode
				+ "\n \n You can change the mode by sendind the wanted mode as plain/text data to the path /mode"
				+ "\n The following modes are available: "
				+ "\n \"classic\": the mock will follow the behaviour of the model sent."
				+ "\n \"XSS\": the model sent will be modified to produce somme XSS attacks."
				+ "\n \"dos\": the model will be modified for sending a lot of huge messages."
				+ "\n \"robustness\": the model will be modified, by replacing values of messages sent by random strings."));

		new Thread(() -> runner()).start();
	}

	private void runner() {
		//return ctx -> {
			try {
				while(true) {
					//System.out.println("in loop");
					if (dotLoaded) {
						//System.out.println("running....");
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
							LogManager.getLogger("MOCK").info(String.format("beginning of a new session."));
							lastAction = System.currentTimeMillis();
							this.notifyAll();// if end of the session, start one paused.
						}
					}			
				}
			} catch (InterruptedException e) {
				LogManager.getLogger("MOCK").info(String.format("the run was interupted."));
				e.printStackTrace();
			}
		//};
	}

	private Handler ModeChange() {
		return ctx -> {
			if (!Objects.equals(ctx.header("Content-Type"), "text/plain")) {
				throw new LoaderException("Wrong content type, \"text/plain\" was expected");
			}
			if (ctx.body().equals("classic")){
				mode = "classic";
				ctx.status(200);
			}
			else if (ctx.body().equals("XSS")){
				mode = "XSS";
				ctx.status(200);
			}
			else if (ctx.body().equals("dos")){
				mode = "dos";
				ctx.status(200);
			}
			else if (ctx.body().equals("robustness")){
				mode = "robustness";
				ctx.status(200);
			}
			else {
				throw new LoaderException("Error, expected modes are \"classic\", \"XSS\", \"dos\", or \"robustness\"");
			}
		};
	}
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
				System.out.println("req received:" + path);
				long time = System.currentTimeMillis();
				fifo.add(time);
				synchronized(this){ 
					RequestT t = dot.getReq(ctx.fullUrl(), pos);
					long now = System.currentTimeMillis();
					if (t !=  null) { // if the request received is allowed by the graph from current position
						if (t.getDelay() == 0 || lastAction != 0 || t.getDelay() < now - lastAction) { // if the request is not received to late
							while (pos.isInit() && getMin() != time) { 
								// if several possible, only one processed at a time (the first received)
								this.wait();
							}						
							pos = t.getTarget();
							lastAction = now;
							while (runMock(true) == true); //run multiple times if nested requests
							ResponseT resp = t.getMinResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
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
								while (System.currentTimeMillis() - lastAction < resp.getDelay()) {
									Thread.sleep(10);
								}
								if (pos.isFinal()) {
									pos = dot.getInitialState();
									LogManager.getLogger("MOCK").info(String.format("beginning of a new session."));
									lastAction = System.currentTimeMillis();
								}
							}
							else { // the response cannot be send from this state in the graph
								ctx.status(500);
								LogManager.getLogger("MOCK").info(String.format("error: response " + resp.toString() + "cannot be launched from state " + pos.toString()));
								pos = dot.getInitialState();
							}
						}
						else { // the request is received too late
							fifo.remove(time);
							ctx.result("request received too late.");
							ctx.status(500);
							LogManager.getLogger("MOCK").info(String.format("request received too late: " + ctx.fullUrl()));
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
							ResponseT resp = t2.getMinResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
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
								while (System.currentTimeMillis() - lastAction < resp.getDelay()) {
									Thread.sleep(10);
								}
								if (pos.isFinal()) {
									pos = dot.getInitialState();
									LogManager.getLogger("MOCK").info(String.format("beginning of a new session."));
									lastAction = System.currentTimeMillis();
								}
							}
							else { // the response cannot be send from this state in the graph
								ctx.status(500);
								LogManager.getLogger("MOCK").info(String.format("error: response " + resp.toString() + " launched from state " + pos.toString()));
								pos = dot.getInitialState();
							}
							//LoggerFactory.getLogger("MOCK").info(String.format("out : " +  time	 + " req : " + ctx.fullUrl()));
						}
						else { // the request is received at the wrong position in the model.
							fifo.remove(time);
							ctx.result("request received at the wrong position in the model.");
							ctx.status(500);
							LogManager.getLogger("MOCK").error(String.format("request received at the wrong position in the model: " + ctx.fullUrl()));
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
			//System.out.println(dot.toString());
			if (mode.equals("XSS")) {
				dot.makeXSS();
			}
			else if (mode.equals("robustness")) {
				//dot.makeRobustness();
			}
			else if (mode.equals("dos")) {
				//dot.makeDos();
			}
			//System.out.println(dot.toString());
			pos = dot.getInitialState();
			buildInputRequestHandler();
			lastAction = System.currentTimeMillis();
			ctx.status(204);
			dotLoaded = true;
		};
	}

	/** 
	 * This method runs the mock following the graph. It sends the request and 
	 * compare the corresponding response with the one waited in the graph.
	 * Return true if the mock acted or can act, and return false else.
	 * @throws InterruptedException
	 *  **/
	private boolean runMock(boolean passOutResp) throws InterruptedException {
		// received response are already automatically processed, skip them in the model
		if (!pos.isInit() && pos.getInRespProc() != null) {
			ResponseT r = pos.getInRespProc();
			r.setProc(false);
			long now = System.currentTimeMillis();
			pos = r.getTarget();
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
			//System.out.println(output.toString());
			OutputRequest send = new OutputRequest(output);
			lastAction = System.currentTimeMillis();;
			send.run();
			pos = output.getTarget();
			if (!send.getMatch()) {
				return false;
			}
			return true;
		}
		// if the component wants to send a request but it's too early (delay)
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
